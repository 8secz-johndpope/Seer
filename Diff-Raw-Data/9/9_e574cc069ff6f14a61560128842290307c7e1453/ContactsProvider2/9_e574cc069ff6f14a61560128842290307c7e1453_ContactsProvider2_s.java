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
  * limitations under the License
  */
 
 package com.android.providers.contacts;
 
 import com.android.internal.content.SyncStateContentProviderHelper;
 import com.android.providers.contacts.ContactLookupKey.LookupKeySegment;
 import com.android.providers.contacts.OpenHelper.AggregatedPresenceColumns;
 import com.android.providers.contacts.OpenHelper.AggregationExceptionColumns;
 import com.android.providers.contacts.OpenHelper.Clauses;
 import com.android.providers.contacts.OpenHelper.ContactsColumns;
 import com.android.providers.contacts.OpenHelper.DataColumns;
 import com.android.providers.contacts.OpenHelper.DisplayNameSources;
 import com.android.providers.contacts.OpenHelper.GroupsColumns;
 import com.android.providers.contacts.OpenHelper.MimetypesColumns;
 import com.android.providers.contacts.OpenHelper.NameLookupColumns;
 import com.android.providers.contacts.OpenHelper.NameLookupType;
 import com.android.providers.contacts.OpenHelper.PackagesColumns;
 import com.android.providers.contacts.OpenHelper.PhoneColumns;
 import com.android.providers.contacts.OpenHelper.PhoneLookupColumns;
 import com.android.providers.contacts.OpenHelper.PresenceColumns;
 import com.android.providers.contacts.OpenHelper.RawContactsColumns;
 import com.android.providers.contacts.OpenHelper.Tables;
 import com.google.android.collect.Lists;
 import com.google.android.collect.Sets;
 import com.google.android.collect.Maps;
 
 import android.accounts.Account;
 import android.accounts.AccountManager;
 import android.accounts.OnAccountsUpdatedListener;
 import android.app.SearchManager;
 import android.content.ContentProviderOperation;
 import android.content.ContentProviderResult;
 import android.content.ContentUris;
 import android.content.ContentValues;
 import android.content.Context;
 import android.content.Entity;
 import android.content.EntityIterator;
 import android.content.OperationApplicationException;
 import android.content.SharedPreferences;
 import android.content.UriMatcher;
 import android.content.SharedPreferences.Editor;
 import android.content.res.AssetFileDescriptor;
 import android.database.Cursor;
 import android.database.DatabaseUtils;
 import android.database.sqlite.SQLiteContentHelper;
 import android.database.sqlite.SQLiteCursor;
 import android.database.sqlite.SQLiteDatabase;
 import android.database.sqlite.SQLiteQueryBuilder;
 import android.database.sqlite.SQLiteStatement;
 import android.net.Uri;
 import android.os.RemoteException;
 import android.preference.PreferenceManager;
 import android.provider.BaseColumns;
 import android.provider.ContactsContract;
 import android.provider.LiveFolders;
 import android.provider.SyncStateContract;
 import android.provider.ContactsContract.AggregationExceptions;
 import android.provider.ContactsContract.CommonDataKinds;
 import android.provider.ContactsContract.Contacts;
 import android.provider.ContactsContract.Data;
 import android.provider.ContactsContract.Groups;
 import android.provider.ContactsContract.PhoneLookup;
 import android.provider.ContactsContract.Presence;
 import android.provider.ContactsContract.RawContacts;
 import android.provider.ContactsContract.Settings;
 import android.provider.ContactsContract.CommonDataKinds.BaseTypes;
 import android.provider.ContactsContract.CommonDataKinds.Email;
 import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
 import android.provider.ContactsContract.CommonDataKinds.Im;
 import android.provider.ContactsContract.CommonDataKinds.Nickname;
 import android.provider.ContactsContract.CommonDataKinds.Organization;
 import android.provider.ContactsContract.CommonDataKinds.Phone;
 import android.provider.ContactsContract.CommonDataKinds.Photo;
 import android.provider.ContactsContract.CommonDataKinds.StructuredName;
 import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
 import android.telephony.PhoneNumberUtils;
 import android.text.TextUtils;
 import android.util.Log;
 
 import java.io.FileNotFoundException;
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Locale;
 import java.util.Set;
 import java.util.HashSet;
 import java.util.Map;
 import java.util.concurrent.CountDownLatch;
 
 /**
  * Contacts content provider. The contract between this provider and applications
  * is defined in {@link ContactsContract}.
  */
 public class ContactsProvider2 extends SQLiteContentProvider implements OnAccountsUpdatedListener {
 
     // TODO: clean up debug tag and rename this class
     private static final String TAG = "ContactsProvider ~~~~";
 
     // TODO: carefully prevent all incoming nested queries; they can be gaping security holes
     // TODO: check for restricted flag during insert(), update(), and delete() calls
 
     /** Default for the maximum number of returned aggregation suggestions. */
     private static final int DEFAULT_MAX_SUGGESTIONS = 5;
 
     /**
      * Shared preference key for the legacy contact import version. The need for a version
      * as opposed to a boolean flag is that if we discover bugs in the contact import process,
      * we can trigger re-import by incrementing the import version.
      */
     private static final String PREF_CONTACTS_IMPORTED = "contacts_imported_v1";
     private static final int PREF_CONTACTS_IMPORT_VERSION = 1;
 
     private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
 
     private static final String STREQUENT_ORDER_BY = Contacts.STARRED + " DESC, "
             + Contacts.TIMES_CONTACTED + " DESC, "
             + Contacts.DISPLAY_NAME + " ASC";
     private static final String STREQUENT_LIMIT =
             "(SELECT COUNT(1) FROM " + Tables.CONTACTS + " WHERE "
             + Contacts.STARRED + "=1) + 25";
 
     private static final int CONTACTS = 1000;
     private static final int CONTACTS_ID = 1001;
     private static final int CONTACTS_LOOKUP = 1002;
     private static final int CONTACTS_LOOKUP_ID = 1003;
     private static final int CONTACTS_DATA = 1004;
     private static final int CONTACTS_FILTER = 1005;
     private static final int CONTACTS_STREQUENT = 1006;
     private static final int CONTACTS_STREQUENT_FILTER = 1007;
     private static final int CONTACTS_GROUP = 1008;
     private static final int CONTACTS_PHOTO = 1009;
 
     private static final int RAW_CONTACTS = 2002;
     private static final int RAW_CONTACTS_ID = 2003;
     private static final int RAW_CONTACTS_DATA = 2004;
 
     private static final int DATA = 3000;
     private static final int DATA_ID = 3001;
     private static final int PHONES = 3002;
     private static final int PHONES_FILTER = 3003;
     private static final int EMAILS = 3004;
     private static final int EMAILS_LOOKUP = 3005;
     private static final int EMAILS_FILTER = 3006;
     private static final int POSTALS = 3007;
 
     private static final int PHONE_LOOKUP = 4000;
 
     private static final int AGGREGATION_EXCEPTIONS = 6000;
     private static final int AGGREGATION_EXCEPTION_ID = 6001;
 
     private static final int PRESENCE = 7000;
     private static final int PRESENCE_ID = 7001;
 
     private static final int AGGREGATION_SUGGESTIONS = 8000;
 
     private static final int SETTINGS = 9000;
 
     private static final int GROUPS = 10000;
     private static final int GROUPS_ID = 10001;
     private static final int GROUPS_SUMMARY = 10003;
 
     private static final int SYNCSTATE = 11000;
     private static final int SYNCSTATE_ID = 11001;
 
     private static final int SEARCH_SUGGESTIONS = 12001;
     private static final int SEARCH_SHORTCUT = 12002;
 
     private static final int DATA_WITH_PRESENCE = 13000;
 
     private static final int LIVE_FOLDERS_CONTACTS = 14000;
     private static final int LIVE_FOLDERS_CONTACTS_WITH_PHONES = 14001;
     private static final int LIVE_FOLDERS_CONTACTS_FAVORITES = 14002;
     private static final int LIVE_FOLDERS_CONTACTS_GROUP_NAME = 14003;
 
     private interface ContactsQuery {
         public static final String TABLE = Tables.RAW_CONTACTS;
 
         public static final String[] PROJECTION = new String[] {
             RawContactsColumns.CONCRETE_ID,
             RawContacts.ACCOUNT_NAME,
             RawContacts.ACCOUNT_TYPE,
         };
 
         public static final int RAW_CONTACT_ID = 0;
         public static final int ACCOUNT_NAME = 1;
         public static final int ACCOUNT_TYPE = 2;
     }
 
     private interface DataContactsQuery {
         public static final String TABLE = Tables.DATA_JOIN_MIMETYPES_RAW_CONTACTS_CONTACTS;
 
         public static final String[] PROJECTION = new String[] {
             RawContactsColumns.CONCRETE_ID,
             DataColumns.CONCRETE_ID,
             ContactsColumns.CONCRETE_ID,
             MimetypesColumns.CONCRETE_ID,
         };
 
         public static final int RAW_CONTACT_ID = 0;
         public static final int DATA_ID = 1;
         public static final int CONTACT_ID = 2;
         public static final int MIMETYPE_ID = 3;
     }
 
     private interface DisplayNameQuery {
         public static final String TABLE = Tables.DATA_JOIN_MIMETYPES;
 
         public static final String[] COLUMNS = new String[] {
             MimetypesColumns.MIMETYPE,
             Data.IS_PRIMARY,
             Data.DATA2,
             StructuredName.DISPLAY_NAME,
         };
 
         public static final int MIMETYPE = 0;
         public static final int IS_PRIMARY = 1;
         public static final int DATA2 = 2;
         public static final int DISPLAY_NAME = 3;
     }
 
     private interface DataDeleteQuery {
         public static final String TABLE = Tables.DATA_JOIN_MIMETYPES;
 
         public static final String[] CONCRETE_COLUMNS = new String[] {
             DataColumns.CONCRETE_ID,
             MimetypesColumns.MIMETYPE,
             Data.RAW_CONTACT_ID,
             Data.IS_PRIMARY,
             Data.DATA2,
         };
 
         public static final String[] COLUMNS = new String[] {
             Data._ID,
             MimetypesColumns.MIMETYPE,
             Data.RAW_CONTACT_ID,
             Data.IS_PRIMARY,
             Data.DATA2,
         };
 
         public static final int _ID = 0;
         public static final int MIMETYPE = 1;
         public static final int RAW_CONTACT_ID = 2;
         public static final int IS_PRIMARY = 3;
         public static final int DATA2 = 4;
     }
 
     private interface DataUpdateQuery {
         String[] COLUMNS = { Data._ID, Data.RAW_CONTACT_ID, Data.MIMETYPE };
 
         int _ID = 0;
         int RAW_CONTACT_ID = 1;
         int MIMETYPE = 2;
     }
 
     private static final HashMap<String, Integer> sDisplayNameSources;
     static {
         sDisplayNameSources = new HashMap<String, Integer>();
         sDisplayNameSources.put(StructuredName.CONTENT_ITEM_TYPE,
                 DisplayNameSources.STRUCTURED_NAME);
         sDisplayNameSources.put(Organization.CONTENT_ITEM_TYPE,
                 DisplayNameSources.ORGANIZATION);
         sDisplayNameSources.put(Phone.CONTENT_ITEM_TYPE,
                 DisplayNameSources.PHONE);
         sDisplayNameSources.put(Email.CONTENT_ITEM_TYPE,
                 DisplayNameSources.EMAIL);
     }
 
     public static final String DEFAULT_ACCOUNT_TYPE = "com.google.GAIA";
     public static final String FEATURE_LEGACY_HOSTED_OR_GOOGLE = "legacy_hosted_or_google";
 
     /** Contains just BaseColumns._COUNT */
     private static final HashMap<String, String> sCountProjectionMap;
     /** Contains just the contacts columns */
     private static final HashMap<String, String> sContactsProjectionMap;
     /** Contains contacts and presence columns */
     private static final HashMap<String, String> sContactsWithPresenceProjectionMap;
     /** Contains just the raw contacts columns */
     private static final HashMap<String, String> sRawContactsProjectionMap;
     /** Contains columns from the data view */
     private static final HashMap<String, String> sDataProjectionMap;
     /** Contains columns from the data view */
     private static final HashMap<String, String> sDistinctDataProjectionMap;
     /** Contains the data and contacts columns, for joined tables */
     private static final HashMap<String, String> sPhoneLookupProjectionMap;
     /** Contains the just the {@link Groups} columns */
     private static final HashMap<String, String> sGroupsProjectionMap;
     /** Contains {@link Groups} columns along with summary details */
     private static final HashMap<String, String> sGroupsSummaryProjectionMap;
     /** Contains the agg_exceptions columns */
     private static final HashMap<String, String> sAggregationExceptionsProjectionMap;
     /** Contains the agg_exceptions columns */
     private static final HashMap<String, String> sSettingsProjectionMap;
     /** Contains Presence columns */
     private static final HashMap<String, String> sPresenceProjectionMap;
     /** Contains Presence columns */
     private static final HashMap<String, String> sDataWithPresenceProjectionMap;
     /** Contains Live Folders columns */
     private static final HashMap<String, String> sLiveFoldersProjectionMap;
 
     /** Sql where statement for filtering on groups. */
     private static final String sContactsInGroupSelect;
 
     /** Precompiled sql statement for setting a data record to the primary. */
     private SQLiteStatement mSetPrimaryStatement;
     /** Precompiled sql statement for setting a data record to the super primary. */
     private SQLiteStatement mSetSuperPrimaryStatement;
     /** Precompiled sql statement for incrementing times contacted for an contact */
     private SQLiteStatement mLastTimeContactedUpdate;
     /** Precompiled sql statement for updating a contact display name */
     private SQLiteStatement mRawContactDisplayNameUpdate;
     /** Precompiled sql statement for marking a raw contact as dirty */
     private SQLiteStatement mRawContactDirtyUpdate;
     /** Precompiled sql statement for setting an aggregated presence */
     private SQLiteStatement mAggregatedPresenceReplace;
     /** Precompiled sql statement for updating an aggregated presence status */
     private SQLiteStatement mAggregatedPresenceStatusUpdate;
 
     static {
         // Contacts URI matching table
         final UriMatcher matcher = sUriMatcher;
         matcher.addURI(ContactsContract.AUTHORITY, "contacts", CONTACTS);
         matcher.addURI(ContactsContract.AUTHORITY, "contacts/#", CONTACTS_ID);
         matcher.addURI(ContactsContract.AUTHORITY, "contacts/#/data", CONTACTS_DATA);
         matcher.addURI(ContactsContract.AUTHORITY, "contacts/#/suggestions",
                 AGGREGATION_SUGGESTIONS);
         matcher.addURI(ContactsContract.AUTHORITY, "contacts/#/photo", CONTACTS_PHOTO);
         matcher.addURI(ContactsContract.AUTHORITY, "contacts/filter/*", CONTACTS_FILTER);
         matcher.addURI(ContactsContract.AUTHORITY, "contacts/lookup/*", CONTACTS_LOOKUP);
         matcher.addURI(ContactsContract.AUTHORITY, "contacts/lookup/*/#", CONTACTS_LOOKUP_ID);
         matcher.addURI(ContactsContract.AUTHORITY, "contacts/strequent/", CONTACTS_STREQUENT);
         matcher.addURI(ContactsContract.AUTHORITY, "contacts/strequent/filter/*",
                 CONTACTS_STREQUENT_FILTER);
         matcher.addURI(ContactsContract.AUTHORITY, "contacts/group/*", CONTACTS_GROUP);
 
         matcher.addURI(ContactsContract.AUTHORITY, "raw_contacts", RAW_CONTACTS);
         matcher.addURI(ContactsContract.AUTHORITY, "raw_contacts/#", RAW_CONTACTS_ID);
         matcher.addURI(ContactsContract.AUTHORITY, "raw_contacts/#/data", RAW_CONTACTS_DATA);
 
         matcher.addURI(ContactsContract.AUTHORITY, "data", DATA);
         matcher.addURI(ContactsContract.AUTHORITY, "data/#", DATA_ID);
         matcher.addURI(ContactsContract.AUTHORITY, "data/phones", PHONES);
         matcher.addURI(ContactsContract.AUTHORITY, "data/phones/filter", PHONES_FILTER);
         matcher.addURI(ContactsContract.AUTHORITY, "data/phones/filter/*", PHONES_FILTER);
         matcher.addURI(ContactsContract.AUTHORITY, "data/emails", EMAILS);
         matcher.addURI(ContactsContract.AUTHORITY, "data/emails/lookup/*", EMAILS_LOOKUP);
         matcher.addURI(ContactsContract.AUTHORITY, "data/emails/filter", EMAILS_FILTER);
         matcher.addURI(ContactsContract.AUTHORITY, "data/emails/filter/*", EMAILS_FILTER);
         matcher.addURI(ContactsContract.AUTHORITY, "data/postals", POSTALS);
 
         matcher.addURI(ContactsContract.AUTHORITY, "groups", GROUPS);
         matcher.addURI(ContactsContract.AUTHORITY, "groups/#", GROUPS_ID);
         matcher.addURI(ContactsContract.AUTHORITY, "groups_summary", GROUPS_SUMMARY);
 
         matcher.addURI(ContactsContract.AUTHORITY, SyncStateContentProviderHelper.PATH, SYNCSTATE);
         matcher.addURI(ContactsContract.AUTHORITY, SyncStateContentProviderHelper.PATH + "/#",
                 SYNCSTATE_ID);
 
         matcher.addURI(ContactsContract.AUTHORITY, "phone_lookup/*", PHONE_LOOKUP);
         matcher.addURI(ContactsContract.AUTHORITY, "aggregation_exceptions",
                 AGGREGATION_EXCEPTIONS);
         matcher.addURI(ContactsContract.AUTHORITY, "aggregation_exceptions/*",
                 AGGREGATION_EXCEPTION_ID);
 
         matcher.addURI(ContactsContract.AUTHORITY, "settings", SETTINGS);
 
         matcher.addURI(ContactsContract.AUTHORITY, "presence", PRESENCE);
         matcher.addURI(ContactsContract.AUTHORITY, "presence/#", PRESENCE_ID);
 
         matcher.addURI(ContactsContract.AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY,
                 SEARCH_SUGGESTIONS);
         matcher.addURI(ContactsContract.AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY + "/*",
                 SEARCH_SUGGESTIONS);
         matcher.addURI(ContactsContract.AUTHORITY, SearchManager.SUGGEST_URI_PATH_SHORTCUT + "/#",
                 SEARCH_SHORTCUT);
 
         matcher.addURI(ContactsContract.AUTHORITY, "live_folders/contacts",
                 LIVE_FOLDERS_CONTACTS);
         matcher.addURI(ContactsContract.AUTHORITY, "live_folders/contacts/*",
                 LIVE_FOLDERS_CONTACTS_GROUP_NAME);
         matcher.addURI(ContactsContract.AUTHORITY, "live_folders/contacts_with_phones",
                 LIVE_FOLDERS_CONTACTS_WITH_PHONES);
         matcher.addURI(ContactsContract.AUTHORITY, "live_folders/favorites",
                 LIVE_FOLDERS_CONTACTS_FAVORITES);
 
         // Private API
         matcher.addURI(ContactsContract.AUTHORITY, "data_with_presence", DATA_WITH_PRESENCE);
     }
 
     static {
         sCountProjectionMap = new HashMap<String, String>();
         sCountProjectionMap.put(BaseColumns._COUNT, "COUNT(*)");
 
         sContactsProjectionMap = new HashMap<String, String>();
         sContactsProjectionMap.put(Contacts._ID, Contacts._ID);
         sContactsProjectionMap.put(Contacts.DISPLAY_NAME, Contacts.DISPLAY_NAME);
         sContactsProjectionMap.put(Contacts.LAST_TIME_CONTACTED, Contacts.LAST_TIME_CONTACTED);
         sContactsProjectionMap.put(Contacts.TIMES_CONTACTED, Contacts.TIMES_CONTACTED);
         sContactsProjectionMap.put(Contacts.STARRED, Contacts.STARRED);
         sContactsProjectionMap.put(Contacts.IN_VISIBLE_GROUP, Contacts.IN_VISIBLE_GROUP);
         sContactsProjectionMap.put(Contacts.PHOTO_ID, Contacts.PHOTO_ID);
         sContactsProjectionMap.put(Contacts.CUSTOM_RINGTONE, Contacts.CUSTOM_RINGTONE);
         sContactsProjectionMap.put(Contacts.HAS_PHONE_NUMBER, Contacts.HAS_PHONE_NUMBER);
         sContactsProjectionMap.put(Contacts.SEND_TO_VOICEMAIL, Contacts.SEND_TO_VOICEMAIL);
         sContactsProjectionMap.put(Contacts.LOOKUP_KEY, Contacts.LOOKUP_KEY);
 
         sContactsWithPresenceProjectionMap = new HashMap<String, String>();
         sContactsWithPresenceProjectionMap.putAll(sContactsProjectionMap);
         sContactsWithPresenceProjectionMap.put(Contacts.PRESENCE_STATUS,
                 Presence.PRESENCE_STATUS + " AS " + Contacts.PRESENCE_STATUS);
         sContactsWithPresenceProjectionMap.put(Contacts.PRESENCE_CUSTOM_STATUS,
                 Presence.PRESENCE_CUSTOM_STATUS + " AS " + Contacts.PRESENCE_CUSTOM_STATUS);
 
         sRawContactsProjectionMap = new HashMap<String, String>();
         sRawContactsProjectionMap.put(RawContacts._ID, RawContacts._ID);
         sRawContactsProjectionMap.put(RawContacts.CONTACT_ID, RawContacts.CONTACT_ID);
         sRawContactsProjectionMap.put(RawContacts.ACCOUNT_NAME, RawContacts.ACCOUNT_NAME);
         sRawContactsProjectionMap.put(RawContacts.ACCOUNT_TYPE, RawContacts.ACCOUNT_TYPE);
         sRawContactsProjectionMap.put(RawContacts.SOURCE_ID, RawContacts.SOURCE_ID);
         sRawContactsProjectionMap.put(RawContacts.VERSION, RawContacts.VERSION);
         sRawContactsProjectionMap.put(RawContacts.DIRTY, RawContacts.DIRTY);
         sRawContactsProjectionMap.put(RawContacts.DELETED, RawContacts.DELETED);
         sRawContactsProjectionMap.put(RawContacts.TIMES_CONTACTED, RawContacts.TIMES_CONTACTED);
         sRawContactsProjectionMap.put(RawContacts.LAST_TIME_CONTACTED,
                 RawContacts.LAST_TIME_CONTACTED);
         sRawContactsProjectionMap.put(RawContacts.CUSTOM_RINGTONE, RawContacts.CUSTOM_RINGTONE);
         sRawContactsProjectionMap.put(RawContacts.SEND_TO_VOICEMAIL, RawContacts.SEND_TO_VOICEMAIL);
         sRawContactsProjectionMap.put(RawContacts.STARRED, RawContacts.STARRED);
         sRawContactsProjectionMap.put(RawContacts.AGGREGATION_MODE, RawContacts.AGGREGATION_MODE);
         sRawContactsProjectionMap.put(RawContacts.SYNC1, RawContacts.SYNC1);
         sRawContactsProjectionMap.put(RawContacts.SYNC2, RawContacts.SYNC2);
         sRawContactsProjectionMap.put(RawContacts.SYNC3, RawContacts.SYNC3);
         sRawContactsProjectionMap.put(RawContacts.SYNC4, RawContacts.SYNC4);
 
         sDataProjectionMap = new HashMap<String, String>();
         sDataProjectionMap.put(Data._ID, Data._ID);
         sDataProjectionMap.put(Data.RAW_CONTACT_ID, Data.RAW_CONTACT_ID);
         sDataProjectionMap.put(Data.DATA_VERSION, Data.DATA_VERSION);
         sDataProjectionMap.put(Data.IS_PRIMARY, Data.IS_PRIMARY);
         sDataProjectionMap.put(Data.IS_SUPER_PRIMARY, Data.IS_SUPER_PRIMARY);
         sDataProjectionMap.put(Data.RES_PACKAGE, Data.RES_PACKAGE);
         sDataProjectionMap.put(Data.MIMETYPE, Data.MIMETYPE);
         sDataProjectionMap.put(Data.DATA1, Data.DATA1);
         sDataProjectionMap.put(Data.DATA2, Data.DATA2);
         sDataProjectionMap.put(Data.DATA3, Data.DATA3);
         sDataProjectionMap.put(Data.DATA4, Data.DATA4);
         sDataProjectionMap.put(Data.DATA5, Data.DATA5);
         sDataProjectionMap.put(Data.DATA6, Data.DATA6);
         sDataProjectionMap.put(Data.DATA7, Data.DATA7);
         sDataProjectionMap.put(Data.DATA8, Data.DATA8);
         sDataProjectionMap.put(Data.DATA9, Data.DATA9);
         sDataProjectionMap.put(Data.DATA10, Data.DATA10);
         sDataProjectionMap.put(Data.DATA11, Data.DATA11);
         sDataProjectionMap.put(Data.DATA12, Data.DATA12);
         sDataProjectionMap.put(Data.DATA13, Data.DATA13);
         sDataProjectionMap.put(Data.DATA14, Data.DATA14);
         sDataProjectionMap.put(Data.DATA15, Data.DATA15);
         sDataProjectionMap.put(Data.SYNC1, Data.SYNC1);
         sDataProjectionMap.put(Data.SYNC2, Data.SYNC2);
         sDataProjectionMap.put(Data.SYNC3, Data.SYNC3);
         sDataProjectionMap.put(Data.SYNC4, Data.SYNC4);
         sDataProjectionMap.put(RawContacts.CONTACT_ID, RawContacts.CONTACT_ID);
         sDataProjectionMap.put(RawContacts.ACCOUNT_NAME, RawContacts.ACCOUNT_NAME);
         sDataProjectionMap.put(RawContacts.ACCOUNT_TYPE, RawContacts.ACCOUNT_TYPE);
         sDataProjectionMap.put(RawContacts.SOURCE_ID, RawContacts.SOURCE_ID);
         sDataProjectionMap.put(RawContacts.VERSION, RawContacts.VERSION);
         sDataProjectionMap.put(RawContacts.DIRTY, RawContacts.DIRTY);
         sDataProjectionMap.put(Contacts.DISPLAY_NAME, Contacts.DISPLAY_NAME);
         sDataProjectionMap.put(Contacts.CUSTOM_RINGTONE, Contacts.CUSTOM_RINGTONE);
         sDataProjectionMap.put(Contacts.SEND_TO_VOICEMAIL, Contacts.SEND_TO_VOICEMAIL);
         sDataProjectionMap.put(Contacts.LAST_TIME_CONTACTED, Contacts.LAST_TIME_CONTACTED);
         sDataProjectionMap.put(Contacts.TIMES_CONTACTED, Contacts.TIMES_CONTACTED);
         sDataProjectionMap.put(Contacts.STARRED, Contacts.STARRED);
         sDataProjectionMap.put(Contacts.PHOTO_ID, Contacts.PHOTO_ID);
         sDataProjectionMap.put(GroupMembership.GROUP_SOURCE_ID, GroupMembership.GROUP_SOURCE_ID);
 
         // Projection map for data grouped by contact (not raw contact) and some data field(s)
         sDistinctDataProjectionMap = new HashMap<String, String>();
         sDistinctDataProjectionMap.put(Data._ID,
                 "MIN(" + Data._ID + ") AS " + Data._ID);
         sDistinctDataProjectionMap.put(Data.DATA_VERSION, Data.DATA_VERSION);
         sDistinctDataProjectionMap.put(Data.IS_PRIMARY, Data.IS_PRIMARY);
         sDistinctDataProjectionMap.put(Data.IS_SUPER_PRIMARY, Data.IS_SUPER_PRIMARY);
         sDistinctDataProjectionMap.put(Data.RES_PACKAGE, Data.RES_PACKAGE);
         sDistinctDataProjectionMap.put(Data.MIMETYPE, Data.MIMETYPE);
         sDistinctDataProjectionMap.put(Data.DATA1, Data.DATA1);
         sDistinctDataProjectionMap.put(Data.DATA2, Data.DATA2);
         sDistinctDataProjectionMap.put(Data.DATA3, Data.DATA3);
         sDistinctDataProjectionMap.put(Data.DATA4, Data.DATA4);
         sDistinctDataProjectionMap.put(Data.DATA5, Data.DATA5);
         sDistinctDataProjectionMap.put(Data.DATA6, Data.DATA6);
         sDistinctDataProjectionMap.put(Data.DATA7, Data.DATA7);
         sDistinctDataProjectionMap.put(Data.DATA8, Data.DATA8);
         sDistinctDataProjectionMap.put(Data.DATA9, Data.DATA9);
         sDistinctDataProjectionMap.put(Data.DATA10, Data.DATA10);
         sDistinctDataProjectionMap.put(Data.DATA11, Data.DATA11);
         sDistinctDataProjectionMap.put(Data.DATA12, Data.DATA12);
         sDistinctDataProjectionMap.put(Data.DATA13, Data.DATA13);
         sDistinctDataProjectionMap.put(Data.DATA14, Data.DATA14);
         sDistinctDataProjectionMap.put(Data.DATA15, Data.DATA15);
         sDistinctDataProjectionMap.put(Data.SYNC1, Data.SYNC1);
         sDistinctDataProjectionMap.put(Data.SYNC2, Data.SYNC2);
         sDistinctDataProjectionMap.put(Data.SYNC3, Data.SYNC3);
         sDistinctDataProjectionMap.put(Data.SYNC4, Data.SYNC4);
         sDistinctDataProjectionMap.put(RawContacts.CONTACT_ID, RawContacts.CONTACT_ID);
         sDistinctDataProjectionMap.put(Contacts.DISPLAY_NAME, Contacts.DISPLAY_NAME);
         sDistinctDataProjectionMap.put(Contacts.CUSTOM_RINGTONE, Contacts.CUSTOM_RINGTONE);
         sDistinctDataProjectionMap.put(Contacts.SEND_TO_VOICEMAIL, Contacts.SEND_TO_VOICEMAIL);
         sDistinctDataProjectionMap.put(Contacts.LAST_TIME_CONTACTED, Contacts.LAST_TIME_CONTACTED);
         sDistinctDataProjectionMap.put(Contacts.TIMES_CONTACTED, Contacts.TIMES_CONTACTED);
         sDistinctDataProjectionMap.put(Contacts.STARRED, Contacts.STARRED);
         sDistinctDataProjectionMap.put(Contacts.PHOTO_ID, Contacts.PHOTO_ID);
         sDistinctDataProjectionMap.put(GroupMembership.GROUP_SOURCE_ID,
                 GroupMembership.GROUP_SOURCE_ID);
 
         sPhoneLookupProjectionMap = new HashMap<String, String>();
         sPhoneLookupProjectionMap.put(PhoneLookup._ID,
                 ContactsColumns.CONCRETE_ID + " AS " + PhoneLookup._ID);
         sPhoneLookupProjectionMap.put(PhoneLookup.DISPLAY_NAME,
                 ContactsColumns.CONCRETE_DISPLAY_NAME + " AS " + PhoneLookup.DISPLAY_NAME);
         sPhoneLookupProjectionMap.put(PhoneLookup.LAST_TIME_CONTACTED,
                 ContactsColumns.CONCRETE_LAST_TIME_CONTACTED
                         + " AS " + PhoneLookup.LAST_TIME_CONTACTED);
         sPhoneLookupProjectionMap.put(PhoneLookup.TIMES_CONTACTED,
                 ContactsColumns.CONCRETE_TIMES_CONTACTED + " AS " + PhoneLookup.TIMES_CONTACTED);
         sPhoneLookupProjectionMap.put(PhoneLookup.STARRED,
                 ContactsColumns.CONCRETE_STARRED + " AS " + PhoneLookup.STARRED);
         sPhoneLookupProjectionMap.put(PhoneLookup.IN_VISIBLE_GROUP,
                 Contacts.IN_VISIBLE_GROUP + " AS " + PhoneLookup.IN_VISIBLE_GROUP);
         sPhoneLookupProjectionMap.put(PhoneLookup.PHOTO_ID,
                 Contacts.PHOTO_ID + " AS " + PhoneLookup.PHOTO_ID);
         sPhoneLookupProjectionMap.put(PhoneLookup.CUSTOM_RINGTONE,
                 ContactsColumns.CONCRETE_CUSTOM_RINGTONE + " AS " + PhoneLookup.CUSTOM_RINGTONE);
         sPhoneLookupProjectionMap.put(PhoneLookup.HAS_PHONE_NUMBER,
                 Contacts.HAS_PHONE_NUMBER + " AS " + PhoneLookup.HAS_PHONE_NUMBER);
         sPhoneLookupProjectionMap.put(PhoneLookup.SEND_TO_VOICEMAIL,
                 ContactsColumns.CONCRETE_SEND_TO_VOICEMAIL
                         + " AS " + PhoneLookup.SEND_TO_VOICEMAIL);
         sPhoneLookupProjectionMap.put(PhoneLookup.NUMBER,
                 Phone.NUMBER + " AS " + PhoneLookup.NUMBER);
         sPhoneLookupProjectionMap.put(PhoneLookup.TYPE,
                 Phone.TYPE + " AS " + PhoneLookup.TYPE);
         sPhoneLookupProjectionMap.put(PhoneLookup.LABEL,
                 Phone.LABEL + " AS " + PhoneLookup.LABEL);
 
         HashMap<String, String> columns;
 
         // Groups projection map
         columns = new HashMap<String, String>();
         columns.put(Groups._ID, "groups._id AS _id");
         columns.put(Groups.ACCOUNT_NAME, Groups.ACCOUNT_NAME);
         columns.put(Groups.ACCOUNT_TYPE, Groups.ACCOUNT_TYPE);
         columns.put(Groups.SOURCE_ID, Groups.SOURCE_ID);
         columns.put(Groups.DIRTY, Groups.DIRTY);
         columns.put(Groups.VERSION, Groups.VERSION);
         columns.put(Groups.RES_PACKAGE, PackagesColumns.PACKAGE + " AS " + Groups.RES_PACKAGE);
         columns.put(Groups.TITLE, Groups.TITLE);
         columns.put(Groups.TITLE_RES, Groups.TITLE_RES);
         columns.put(Groups.GROUP_VISIBLE, Groups.GROUP_VISIBLE);
         columns.put(Groups.SYSTEM_ID, Groups.SYSTEM_ID);
         columns.put(Groups.DELETED, Groups.DELETED);
         columns.put(Groups.NOTES, Groups.NOTES);
         columns.put(Groups.SHOULD_SYNC, Groups.SHOULD_SYNC);
         columns.put(Groups.SYNC1, Tables.GROUPS + "." + Groups.SYNC1 + " AS " + Groups.SYNC1);
         columns.put(Groups.SYNC2, Tables.GROUPS + "." + Groups.SYNC2 + " AS " + Groups.SYNC2);
         columns.put(Groups.SYNC3, Tables.GROUPS + "." + Groups.SYNC3 + " AS " + Groups.SYNC3);
         columns.put(Groups.SYNC4, Tables.GROUPS + "." + Groups.SYNC4 + " AS " + Groups.SYNC4);
         sGroupsProjectionMap = columns;
 
         // RawContacts and groups projection map
         columns = new HashMap<String, String>();
         columns.putAll(sGroupsProjectionMap);
         columns.put(Groups.SUMMARY_COUNT, "(SELECT COUNT(DISTINCT " + ContactsColumns.CONCRETE_ID
                 + ") FROM " + Tables.DATA_JOIN_MIMETYPES_RAW_CONTACTS_CONTACTS + " WHERE "
                 + Clauses.MIMETYPE_IS_GROUP_MEMBERSHIP + " AND " + Clauses.BELONGS_TO_GROUP
                 + ") AS " + Groups.SUMMARY_COUNT);
         columns.put(Groups.SUMMARY_WITH_PHONES, "(SELECT COUNT(DISTINCT "
                 + ContactsColumns.CONCRETE_ID + ") FROM "
                 + Tables.DATA_JOIN_MIMETYPES_RAW_CONTACTS_CONTACTS + " WHERE "
                 + Clauses.MIMETYPE_IS_GROUP_MEMBERSHIP + " AND " + Clauses.BELONGS_TO_GROUP
                 + " AND " + Contacts.HAS_PHONE_NUMBER + ") AS " + Groups.SUMMARY_WITH_PHONES);
         sGroupsSummaryProjectionMap = columns;
 
         // Aggregate exception projection map
         columns = new HashMap<String, String>();
         columns.put(AggregationExceptionColumns._ID, Tables.AGGREGATION_EXCEPTIONS + "._id AS _id");
         columns.put(AggregationExceptions.TYPE, AggregationExceptions.TYPE);
         columns.put(AggregationExceptions.RAW_CONTACT_ID1, AggregationExceptions.RAW_CONTACT_ID1);
         columns.put(AggregationExceptions.RAW_CONTACT_ID2, AggregationExceptions.RAW_CONTACT_ID2);
         sAggregationExceptionsProjectionMap = columns;
 
         // Settings projection map
         columns = new HashMap<String, String>();
         columns.put(Settings.ACCOUNT_NAME, Settings.ACCOUNT_NAME);
         columns.put(Settings.ACCOUNT_TYPE, Settings.ACCOUNT_TYPE);
         columns.put(Settings.UNGROUPED_VISIBLE, Settings.UNGROUPED_VISIBLE);
         columns.put(Settings.SHOULD_SYNC, Settings.SHOULD_SYNC);
         columns.put(Settings.UNGROUPED_COUNT, "(SELECT COUNT(*) FROM (SELECT 1 FROM "
                 + Tables.SETTINGS_JOIN_RAW_CONTACTS_DATA_MIMETYPES_CONTACTS + " GROUP BY "
                 + Clauses.GROUP_BY_ACCOUNT_CONTACT_ID + " HAVING " + Clauses.HAVING_NO_GROUPS
                 + ")) AS " + Settings.UNGROUPED_COUNT);
         columns.put(Settings.UNGROUPED_WITH_PHONES, "(SELECT COUNT(*) FROM (SELECT 1 FROM "
                 + Tables.SETTINGS_JOIN_RAW_CONTACTS_DATA_MIMETYPES_CONTACTS + " WHERE "
                 + Contacts.HAS_PHONE_NUMBER + " GROUP BY " + Clauses.GROUP_BY_ACCOUNT_CONTACT_ID
                 + " HAVING " + Clauses.HAVING_NO_GROUPS + ")) AS "
                 + Settings.UNGROUPED_WITH_PHONES);
         sSettingsProjectionMap = columns;
 
         columns = new HashMap<String, String>();
         columns.put(Presence._ID, Presence._ID);
         columns.put(PresenceColumns.RAW_CONTACT_ID, PresenceColumns.RAW_CONTACT_ID);
         columns.put(Presence.DATA_ID, Presence.DATA_ID);
         columns.put(Presence.IM_ACCOUNT, Presence.IM_ACCOUNT);
         columns.put(Presence.IM_HANDLE, Presence.IM_HANDLE);
         columns.put(Presence.PROTOCOL, Presence.PROTOCOL);
         columns.put(Presence.CUSTOM_PROTOCOL, Presence.CUSTOM_PROTOCOL);
         columns.put(Presence.PRESENCE_STATUS, Presence.PRESENCE_STATUS);
         columns.put(Presence.PRESENCE_CUSTOM_STATUS, Presence.PRESENCE_CUSTOM_STATUS);
         sPresenceProjectionMap = columns;
 
         sDataWithPresenceProjectionMap = new HashMap<String, String>();
         sDataWithPresenceProjectionMap.putAll(sDataProjectionMap);
         sDataWithPresenceProjectionMap.put(Presence.PRESENCE_STATUS,
                 Presence.PRESENCE_STATUS);
         sDataWithPresenceProjectionMap.put(Presence.PRESENCE_CUSTOM_STATUS,
                 Presence.PRESENCE_CUSTOM_STATUS);
 
         // Live folder projection
         sLiveFoldersProjectionMap = new HashMap<String, String>();
         sLiveFoldersProjectionMap.put(LiveFolders._ID,
                 Contacts._ID + " AS " + LiveFolders._ID);
         sLiveFoldersProjectionMap.put(LiveFolders.NAME,
                 Contacts.DISPLAY_NAME + " AS " + LiveFolders.NAME);
 
         // TODO: Put contact photo back when we have a way to display a default icon
         // for contacts without a photo
         // sLiveFoldersProjectionMap.put(LiveFolders.ICON_BITMAP,
         //      Photos.DATA + " AS " + LiveFolders.ICON_BITMAP);
 
         sContactsInGroupSelect = Contacts._ID + " IN "
                 + "(SELECT " + RawContacts.CONTACT_ID
                 + " FROM " + Tables.RAW_CONTACTS
                 + " WHERE " + RawContactsColumns.CONCRETE_ID + " IN "
                         + "(SELECT " + DataColumns.CONCRETE_RAW_CONTACT_ID
                         + " FROM " + Tables.DATA_JOIN_MIMETYPES
                         + " WHERE " + Data.MIMETYPE + "='" + GroupMembership.CONTENT_ITEM_TYPE
                                 + "' AND " + GroupMembership.GROUP_ROW_ID + "="
                                 + "(SELECT " + Tables.GROUPS + "." + Groups._ID
                                 + " FROM " + Tables.GROUPS
                                 + " WHERE " + Groups.TITLE + "=?)))";
     }
 
     /**
      * Handles inserts and update for a specific Data type.
      */
     private abstract class DataRowHandler {
 
         protected final String mMimetype;
         protected long mMimetypeId;
 
         public DataRowHandler(String mimetype) {
             mMimetype = mimetype;
         }
 
         protected long getMimeTypeId() {
             if (mMimetypeId == 0) {
                 mMimetypeId = mOpenHelper.getMimeTypeId(mMimetype);
             }
             return mMimetypeId;
         }
 
         /**
          * Inserts a row into the {@link Data} table.
          */
         public long insert(SQLiteDatabase db, long rawContactId, ContentValues values) {
             final long dataId = db.insert(Tables.DATA, null, values);
 
             Integer primary = values.getAsInteger(Data.IS_PRIMARY);
             if (primary != null && primary != 0) {
                 setIsPrimary(rawContactId, dataId, getMimeTypeId());
             }
 
             return dataId;
         }
 
         /**
          * Validates data and updates a {@link Data} row using the cursor, which contains
          * the current data.
          */
         public void update(SQLiteDatabase db, ContentValues values, Cursor c,
                 boolean markRawContactAsDirty) {
             long dataId = c.getLong(DataUpdateQuery._ID);
             long rawContactId = c.getLong(DataUpdateQuery.RAW_CONTACT_ID);
 
             if (values.containsKey(Data.IS_SUPER_PRIMARY)) {
                 long mimeTypeId = getMimeTypeId();
                 setIsSuperPrimary(rawContactId, dataId, mimeTypeId);
                 setIsPrimary(rawContactId, dataId, mimeTypeId);
 
                 // Now that we've taken care of setting these, remove them from "values".
                 values.remove(Data.IS_SUPER_PRIMARY);
                 values.remove(Data.IS_PRIMARY);
             } else if (values.containsKey(Data.IS_PRIMARY)) {
                 setIsPrimary(rawContactId, dataId, getMimeTypeId());
 
                 // Now that we've taken care of setting this, remove it from "values".
                 values.remove(Data.IS_PRIMARY);
             }
 
             if (values.size() > 0) {
                 mDb.update(Tables.DATA, values, Data._ID + " = " + dataId, null);
             }
 
             if (markRawContactAsDirty) {
                 setRawContactDirty(rawContactId);
             }
         }
 
         public int delete(SQLiteDatabase db, Cursor c) {
             long dataId = c.getLong(DataDeleteQuery._ID);
             long rawContactId = c.getLong(DataDeleteQuery.RAW_CONTACT_ID);
             boolean primary = c.getInt(DataDeleteQuery.IS_PRIMARY) != 0;
             int count = db.delete(Tables.DATA, Data._ID + "=" + dataId, null);
             if (count != 0 && primary) {
                 fixPrimary(db, rawContactId);
             }
             return count;
         }
 
         private void fixPrimary(SQLiteDatabase db, long rawContactId) {
             long newPrimaryId = findNewPrimaryDataId(db, rawContactId);
             if (newPrimaryId != -1) {
                 setIsPrimary(rawContactId, newPrimaryId, getMimeTypeId());
             }
         }
 
         protected long findNewPrimaryDataId(SQLiteDatabase db, long rawContactId) {
             long primaryId = -1;
             int primaryType = -1;
             Cursor c = queryData(db, rawContactId);
             try {
                 while (c.moveToNext()) {
                     long dataId = c.getLong(DataDeleteQuery._ID);
                     int type = c.getInt(DataDeleteQuery.DATA2);
                     if (primaryType == -1 || getTypeRank(type) < getTypeRank(primaryType)) {
                         primaryId = dataId;
                         primaryType = type;
                     }
                 }
             } finally {
                 c.close();
             }
             return primaryId;
         }
 
         /**
          * Returns the rank of a specific record type to be used in determining the primary
          * row. Lower number represents higher priority.
          */
         protected int getTypeRank(int type) {
             return 0;
         }
 
         protected Cursor queryData(SQLiteDatabase db, long rawContactId) {
             return db.query(DataDeleteQuery.TABLE, DataDeleteQuery.CONCRETE_COLUMNS,
                     Data.RAW_CONTACT_ID + "=" + rawContactId +
                     " AND " + MimetypesColumns.MIMETYPE + "='" + mMimetype + "'",
                     null, null, null, null);
         }
 
         protected void fixRawContactDisplayName(SQLiteDatabase db, long rawContactId) {
             String bestDisplayName = null;
             int bestDisplayNameSource = DisplayNameSources.UNDEFINED;
 
             Cursor c = db.query(DisplayNameQuery.TABLE, DisplayNameQuery.COLUMNS,
                     Data.RAW_CONTACT_ID + "=" + rawContactId, null, null, null, null);
             try {
                 while (c.moveToNext()) {
                     String mimeType = c.getString(DisplayNameQuery.MIMETYPE);
                     boolean primary;
                     String name;
 
                     if (StructuredName.CONTENT_ITEM_TYPE.equals(mimeType)) {
                         name = c.getString(DisplayNameQuery.DISPLAY_NAME);
                         primary = true;
                     } else {
                         name = c.getString(DisplayNameQuery.DATA2);
                         primary = (c.getInt(DisplayNameQuery.IS_PRIMARY) != 0);
                     }
 
                     if (name != null) {
                         Integer source = sDisplayNameSources.get(mimeType);
                         if (source != null
                                 && (source > bestDisplayNameSource
                                         || (source == bestDisplayNameSource && primary))) {
                             bestDisplayNameSource = source;
                             bestDisplayName = name;
                         }
                     }
                 }
 
             } finally {
                 c.close();
             }
 
             setDisplayName(rawContactId, bestDisplayName, bestDisplayNameSource);
             if (!isNewRawContact(rawContactId)) {
                 mContactAggregator.updateDisplayName(db, rawContactId);
             }
         }
 
         public boolean isAggregationRequired() {
             return true;
         }
 
         /**
          * Return set of values, using current values at given {@link Data#_ID}
          * as baseline, but augmented with any updates.
          */
         public ContentValues getAugmentedValues(SQLiteDatabase db, long dataId,
                 ContentValues update) {
             final ContentValues values = new ContentValues();
             final Cursor cursor = db.query(Tables.DATA, null, Data._ID + "=" + dataId,
                     null, null, null, null);
             try {
                 if (cursor.moveToFirst()) {
                     for (int i = 0; i < cursor.getColumnCount(); i++) {
                         final String key = cursor.getColumnName(i);
                         values.put(key, cursor.getString(i));
                     }
                 }
             } finally {
                 cursor.close();
             }
             values.putAll(update);
             return values;
         }
     }
 
     public class CustomDataRowHandler extends DataRowHandler {
 
         public CustomDataRowHandler(String mimetype) {
             super(mimetype);
         }
     }
 
     public class StructuredNameRowHandler extends DataRowHandler {
         private final NameSplitter mSplitter;
 
         public StructuredNameRowHandler(NameSplitter splitter) {
             super(StructuredName.CONTENT_ITEM_TYPE);
             mSplitter = splitter;
         }
 
         @Override
         public long insert(SQLiteDatabase db, long rawContactId, ContentValues values) {
             fixStructuredNameComponents(values, values);
 
             long dataId = super.insert(db, rawContactId, values);
 
             String givenName = values.getAsString(StructuredName.GIVEN_NAME);
             String familyName = values.getAsString(StructuredName.FAMILY_NAME);
             mOpenHelper.insertNameLookupForStructuredName(rawContactId, dataId, givenName,
                     familyName);
             fixRawContactDisplayName(db, rawContactId);
             return dataId;
         }
 
         @Override
         public void update(SQLiteDatabase db, ContentValues values, Cursor c,
                 boolean markRawContactAsDirty) {
             final long dataId = c.getLong(DataUpdateQuery._ID);
             final long rawContactId = c.getLong(DataUpdateQuery.RAW_CONTACT_ID);
 
             final ContentValues augmented = getAugmentedValues(db, dataId, values);
             fixStructuredNameComponents(augmented, values);
 
             super.update(db, values, c, markRawContactAsDirty);
 
             boolean hasGivenName = values.containsKey(StructuredName.GIVEN_NAME);
             boolean hasFamilyName = values.containsKey(StructuredName.FAMILY_NAME);
             if  (hasGivenName || hasFamilyName) {
                 String givenName;
                 String familyName;// = values.getAsString(StructuredName.FAMILY_NAME);
                 if (hasGivenName) {
                     givenName = values.getAsString(StructuredName.GIVEN_NAME);
                 } else {
 
                     // TODO compiled statement
                     givenName = DatabaseUtils.stringForQuery(db,
                             "SELECT " + StructuredName.GIVEN_NAME +
                             " FROM " + Tables.DATA +
                             " WHERE " + Data._ID + "=" + dataId, null);
                 }
                 if (hasFamilyName) {
                     familyName = values.getAsString(StructuredName.FAMILY_NAME);
                 } else {
 
                     // TODO compiled statement
                     familyName = DatabaseUtils.stringForQuery(db,
                             "SELECT " + StructuredName.FAMILY_NAME +
                             " FROM " + Tables.DATA +
                             " WHERE " + Data._ID + "=" + dataId, null);
                 }
 
                 mOpenHelper.deleteNameLookup(dataId);
                 mOpenHelper.insertNameLookupForStructuredName(rawContactId, dataId, givenName,
                         familyName);
             }
             fixRawContactDisplayName(db, rawContactId);
         }
 
         @Override
         public int delete(SQLiteDatabase db, Cursor c) {
             long dataId = c.getLong(DataDeleteQuery._ID);
             long rawContactId = c.getLong(DataDeleteQuery.RAW_CONTACT_ID);
 
             int count = super.delete(db, c);
 
             mOpenHelper.deleteNameLookup(dataId);
             fixRawContactDisplayName(db, rawContactId);
             return count;
         }
 
         /**
          * Specific list of structured fields.
          */
         private final String[] STRUCTURED_FIELDS = new String[] {
                 StructuredName.PREFIX, StructuredName.GIVEN_NAME, StructuredName.MIDDLE_NAME,
                 StructuredName.FAMILY_NAME, StructuredName.SUFFIX
         };
 
         /**
          * Parses the supplied display name, but only if the incoming values do
          * not already contain structured name parts. Also, if the display name
          * is not provided, generate one by concatenating first name and last
          * name.
          */
         private void fixStructuredNameComponents(ContentValues augmented, ContentValues update) {
             final String unstruct = update.getAsString(StructuredName.DISPLAY_NAME);
 
             final boolean touchedUnstruct = !TextUtils.isEmpty(unstruct);
             final boolean touchedStruct = !areAllEmpty(update, STRUCTURED_FIELDS);
 
             final NameSplitter.Name name = new NameSplitter.Name();
 
             if (touchedUnstruct && !touchedStruct) {
                 mSplitter.split(name, unstruct);
                 name.toValues(update);
             } else if (!touchedUnstruct && touchedStruct) {
                 name.fromValues(augmented);
                 final String joined = mSplitter.join(name);
                 update.put(StructuredName.DISPLAY_NAME, joined);
             }
         }
     }
 
     public class StructuredPostalRowHandler extends DataRowHandler {
         private PostalSplitter mSplitter;
 
         public StructuredPostalRowHandler(PostalSplitter splitter) {
             super(StructuredPostal.CONTENT_ITEM_TYPE);
             mSplitter = splitter;
         }
 
         @Override
         public long insert(SQLiteDatabase db, long rawContactId, ContentValues values) {
             fixStructuredPostalComponents(values, values);
             return super.insert(db, rawContactId, values);
         }
 
         @Override
         public void update(SQLiteDatabase db, ContentValues values, Cursor c,
                 boolean markRawContactAsDirty) {
             final long dataId = c.getLong(DataUpdateQuery._ID);
             final ContentValues augmented = getAugmentedValues(db, dataId, values);
             fixStructuredPostalComponents(augmented, values);
             super.update(db, values, c, markRawContactAsDirty);
         }
 
         /**
          * Specific list of structured fields.
          */
         private final String[] STRUCTURED_FIELDS = new String[] {
                 StructuredPostal.STREET, StructuredPostal.POBOX, StructuredPostal.NEIGHBORHOOD,
                 StructuredPostal.CITY, StructuredPostal.REGION, StructuredPostal.POSTCODE,
                 StructuredPostal.COUNTRY,
         };
 
         /**
          * Prepares the given {@link StructuredPostal} row, building
          * {@link StructuredPostal#FORMATTED_ADDRESS} to match the structured
          * values when missing. When structured components are missing, the
          * unstructured value is assigned to {@link StructuredPostal#STREET}.
          */
         private void fixStructuredPostalComponents(ContentValues augmented, ContentValues update) {
             final String unstruct = update.getAsString(StructuredPostal.FORMATTED_ADDRESS);
 
             final boolean touchedUnstruct = !TextUtils.isEmpty(unstruct);
             final boolean touchedStruct = !areAllEmpty(update, STRUCTURED_FIELDS);
 
             final PostalSplitter.Postal postal = new PostalSplitter.Postal();
 
             if (touchedUnstruct && !touchedStruct) {
                 mSplitter.split(postal, unstruct);
                 postal.toValues(update);
             } else if (!touchedUnstruct && touchedStruct) {
                 postal.fromValues(augmented);
                 final String joined = mSplitter.join(postal);
                 update.put(StructuredPostal.FORMATTED_ADDRESS, joined);
             }
         }
     }
 
     public class CommonDataRowHandler extends DataRowHandler {
 
         private final String mTypeColumn;
         private final String mLabelColumn;
 
         public CommonDataRowHandler(String mimetype, String typeColumn, String labelColumn) {
             super(mimetype);
             mTypeColumn = typeColumn;
             mLabelColumn = labelColumn;
         }
 
         @Override
         public long insert(SQLiteDatabase db, long rawContactId, ContentValues values) {
             enforceTypeAndLabel(values, values);
             return super.insert(db, rawContactId, values);
         }
 
         @Override
         public void update(SQLiteDatabase db, ContentValues values, Cursor c,
                 boolean markRawContactAsDirty) {
             final long dataId = c.getLong(DataUpdateQuery._ID);
             final ContentValues augmented = getAugmentedValues(db, dataId, values);
             enforceTypeAndLabel(augmented, values);
             super.update(db, values, c, markRawContactAsDirty);
         }
 
         /**
          * If the given {@link ContentValues} defines {@link #mTypeColumn},
          * enforce that {@link #mLabelColumn} only appears when type is
          * {@link BaseTypes#TYPE_CUSTOM}. Exception is thrown otherwise.
          */
         private void enforceTypeAndLabel(ContentValues augmented, ContentValues update) {
             final boolean hasType = !TextUtils.isEmpty(augmented.getAsString(mTypeColumn));
             final boolean hasLabel = !TextUtils.isEmpty(augmented.getAsString(mLabelColumn));
 
             if (hasLabel && !hasType) {
                 // When label exists, assert that some type is defined
                 throw new IllegalArgumentException(mTypeColumn + " must be specified when "
                         + mLabelColumn + " is defined.");
             }
         }
     }
 
     public class OrganizationDataRowHandler extends CommonDataRowHandler {
 
         public OrganizationDataRowHandler() {
             super(Organization.CONTENT_ITEM_TYPE, Organization.TYPE, Organization.LABEL);
         }
 
         @Override
         public long insert(SQLiteDatabase db, long rawContactId, ContentValues values) {
             long id = super.insert(db, rawContactId, values);
             fixRawContactDisplayName(db, rawContactId);
             return id;
         }
 
         @Override
         public void update(SQLiteDatabase db, ContentValues values, Cursor c,
                 boolean markRawContactAsDirty) {
             long rawContactId = c.getLong(DataUpdateQuery.RAW_CONTACT_ID);
 
             super.update(db, values, c, markRawContactAsDirty);
 
             fixRawContactDisplayName(db, rawContactId);
         }
 
         @Override
         public int delete(SQLiteDatabase db, Cursor c) {
             long rawContactId = c.getLong(DataDeleteQuery.RAW_CONTACT_ID);
 
             int count = super.delete(db, c);
             fixRawContactDisplayName(db, rawContactId);
             return count;
         }
 
         @Override
         protected int getTypeRank(int type) {
             switch (type) {
                 case Organization.TYPE_WORK: return 0;
                 case Organization.TYPE_CUSTOM: return 1;
                 case Organization.TYPE_OTHER: return 2;
                 default: return 1000;
             }
         }
 
         @Override
         public boolean isAggregationRequired() {
             return false;
         }
     }
 
     public class EmailDataRowHandler extends CommonDataRowHandler {
 
         public EmailDataRowHandler() {
             super(Email.CONTENT_ITEM_TYPE, Email.TYPE, Email.LABEL);
         }
 
         @Override
         public long insert(SQLiteDatabase db, long rawContactId, ContentValues values) {
             String address = values.getAsString(Email.DATA);
 
             long dataId = super.insert(db, rawContactId, values);
 
             fixRawContactDisplayName(db, rawContactId);
             mOpenHelper.insertNameLookupForEmail(rawContactId, dataId, address);
             return dataId;
         }
 
         @Override
         public void update(SQLiteDatabase db, ContentValues values, Cursor c,
                 boolean markRawContactAsDirty) {
             long dataId = c.getLong(DataUpdateQuery._ID);
             long rawContactId = c.getLong(DataUpdateQuery.RAW_CONTACT_ID);
             String address = values.getAsString(Email.DATA);
 
             super.update(db, values, c, markRawContactAsDirty);
 
             mOpenHelper.deleteNameLookup(dataId);
             mOpenHelper.insertNameLookupForEmail(rawContactId, dataId, address);
             fixRawContactDisplayName(db, rawContactId);
         }
 
         @Override
         public int delete(SQLiteDatabase db, Cursor c) {
             long dataId = c.getLong(DataDeleteQuery._ID);
             long rawContactId = c.getLong(DataDeleteQuery.RAW_CONTACT_ID);
 
             int count = super.delete(db, c);
 
             mOpenHelper.deleteNameLookup(dataId);
             fixRawContactDisplayName(db, rawContactId);
             return count;
         }
 
         @Override
         protected int getTypeRank(int type) {
             switch (type) {
                 case Email.TYPE_HOME: return 0;
                 case Email.TYPE_WORK: return 1;
                 case Email.TYPE_CUSTOM: return 2;
                 case Email.TYPE_OTHER: return 3;
                 default: return 1000;
             }
         }
     }
 
     public class NicknameDataRowHandler extends CommonDataRowHandler {
 
         public NicknameDataRowHandler() {
             super(Nickname.CONTENT_ITEM_TYPE, Nickname.TYPE, Nickname.LABEL);
         }
 
         @Override
         public long insert(SQLiteDatabase db, long rawContactId, ContentValues values) {
             String nickname = values.getAsString(Nickname.NAME);
 
             long dataId = super.insert(db, rawContactId, values);
 
             fixRawContactDisplayName(db, rawContactId);
             mOpenHelper.insertNameLookupForNickname(rawContactId, dataId, nickname);
             return dataId;
         }
 
         @Override
         public void update(SQLiteDatabase db, ContentValues values, Cursor c,
                 boolean markRawContactAsDirty) {
             long dataId = c.getLong(DataUpdateQuery._ID);
             long rawContactId = c.getLong(DataUpdateQuery.RAW_CONTACT_ID);
             String nickname = values.getAsString(Nickname.NAME);
 
             super.update(db, values, c, markRawContactAsDirty);
 
             mOpenHelper.deleteNameLookup(dataId);
             mOpenHelper.insertNameLookupForNickname(rawContactId, dataId, nickname);
             fixRawContactDisplayName(db, rawContactId);
         }
 
         @Override
         public int delete(SQLiteDatabase db, Cursor c) {
             long dataId = c.getLong(DataDeleteQuery._ID);
             long rawContactId = c.getLong(DataDeleteQuery.RAW_CONTACT_ID);
 
             int count = super.delete(db, c);
 
             mOpenHelper.deleteNameLookup(dataId);
             fixRawContactDisplayName(db, rawContactId);
             return count;
         }
     }
 
     public class PhoneDataRowHandler extends CommonDataRowHandler {
 
         public PhoneDataRowHandler() {
             super(Phone.CONTENT_ITEM_TYPE, Phone.TYPE, Phone.LABEL);
         }
 
         @Override
         public long insert(SQLiteDatabase db, long rawContactId, ContentValues values) {
             long dataId;
             if (values.containsKey(Phone.NUMBER)) {
                 String number = values.getAsString(Phone.NUMBER);
                 String normalizedNumber = computeNormalizedNumber(number, values);
 
                 dataId = super.insert(db, rawContactId, values);
 
                 updatePhoneLookup(db, rawContactId, dataId, number, normalizedNumber);
                 mContactAggregator.updateHasPhoneNumber(db, rawContactId);
                 fixRawContactDisplayName(db, rawContactId);
             } else {
                 dataId = super.insert(db, rawContactId, values);
             }
             return dataId;
         }
 
         @Override
         public void update(SQLiteDatabase db, ContentValues values, Cursor c,
                 boolean markRawContactAsDirty) {
             long dataId = c.getLong(DataUpdateQuery._ID);
             long rawContactId = c.getLong(DataUpdateQuery.RAW_CONTACT_ID);
             if (values.containsKey(Phone.NUMBER)) {
                 String number = values.getAsString(Phone.NUMBER);
                 String normalizedNumber = computeNormalizedNumber(number, values);
 
                 super.update(db, values, c, markRawContactAsDirty);
 
                 updatePhoneLookup(db, rawContactId, dataId, number, normalizedNumber);
                 mContactAggregator.updateHasPhoneNumber(db, rawContactId);
                 fixRawContactDisplayName(db, rawContactId);
             } else {
                 super.update(db, values, c, markRawContactAsDirty);
             }
         }
 
         @Override
         public int delete(SQLiteDatabase db, Cursor c) {
             long dataId = c.getLong(DataDeleteQuery._ID);
             long rawContactId = c.getLong(DataDeleteQuery.RAW_CONTACT_ID);
 
             int count = super.delete(db, c);
 
             updatePhoneLookup(db, rawContactId, dataId, null, null);
             mContactAggregator.updateHasPhoneNumber(db, rawContactId);
             fixRawContactDisplayName(db, rawContactId);
             return count;
         }
 
         private String computeNormalizedNumber(String number, ContentValues values) {
             String normalizedNumber = null;
             if (number != null) {
                 normalizedNumber = PhoneNumberUtils.getStrippedReversed(number);
             }
             values.put(PhoneColumns.NORMALIZED_NUMBER, normalizedNumber);
             return normalizedNumber;
         }
 
         private void updatePhoneLookup(SQLiteDatabase db, long rawContactId, long dataId,
                 String number, String normalizedNumber) {
             if (number != null) {
                 ContentValues phoneValues = new ContentValues();
                 phoneValues.put(PhoneLookupColumns.RAW_CONTACT_ID, rawContactId);
                 phoneValues.put(PhoneLookupColumns.DATA_ID, dataId);
                 phoneValues.put(PhoneLookupColumns.NORMALIZED_NUMBER, normalizedNumber);
                 db.replace(Tables.PHONE_LOOKUP, null, phoneValues);
             } else {
                 db.delete(Tables.PHONE_LOOKUP, PhoneLookupColumns.DATA_ID + "=" + dataId, null);
             }
         }
 
         @Override
         protected int getTypeRank(int type) {
             switch (type) {
                 case Phone.TYPE_MOBILE: return 0;
                 case Phone.TYPE_WORK: return 1;
                 case Phone.TYPE_HOME: return 2;
                 case Phone.TYPE_PAGER: return 3;
                 case Phone.TYPE_CUSTOM: return 4;
                 case Phone.TYPE_OTHER: return 5;
                 case Phone.TYPE_FAX_WORK: return 6;
                 case Phone.TYPE_FAX_HOME: return 7;
                 default: return 1000;
             }
         }
     }
 
     public class GroupMembershipRowHandler extends DataRowHandler {
 
         public GroupMembershipRowHandler() {
             super(GroupMembership.CONTENT_ITEM_TYPE);
         }
 
         @Override
         public long insert(SQLiteDatabase db, long rawContactId, ContentValues values) {
             resolveGroupSourceIdInValues(rawContactId, db, values, true);
             return super.insert(db, rawContactId, values);
         }
 
         @Override
         public void update(SQLiteDatabase db, ContentValues values, Cursor c,
                 boolean markRawContactAsDirty) {
             long rawContactId = c.getLong(DataUpdateQuery.RAW_CONTACT_ID);
             resolveGroupSourceIdInValues(rawContactId, db, values, false);
             super.update(db, values, c, markRawContactAsDirty);
         }
 
         private void resolveGroupSourceIdInValues(long rawContactId, SQLiteDatabase db,
                 ContentValues values, boolean isInsert) {
             boolean containsGroupSourceId = values.containsKey(GroupMembership.GROUP_SOURCE_ID);
             boolean containsGroupId = values.containsKey(GroupMembership.GROUP_ROW_ID);
             if (containsGroupSourceId && containsGroupId) {
                 throw new IllegalArgumentException(
                         "you are not allowed to set both the GroupMembership.GROUP_SOURCE_ID "
                                 + "and GroupMembership.GROUP_ROW_ID");
             }
 
             if (!containsGroupSourceId && !containsGroupId) {
                 if (isInsert) {
                     throw new IllegalArgumentException(
                             "you must set exactly one of GroupMembership.GROUP_SOURCE_ID "
                                     + "and GroupMembership.GROUP_ROW_ID");
                 } else {
                     return;
                 }
             }
 
             if (containsGroupSourceId) {
                 final String sourceId = values.getAsString(GroupMembership.GROUP_SOURCE_ID);
                 final long groupId = getOrMakeGroup(db, rawContactId, sourceId);
                 values.remove(GroupMembership.GROUP_SOURCE_ID);
                 values.put(GroupMembership.GROUP_ROW_ID, groupId);
             }
         }
 
         @Override
         public boolean isAggregationRequired() {
             return false;
         }
     }
 
     public class PhotoDataRowHandler extends DataRowHandler {
 
         public PhotoDataRowHandler() {
             super(Photo.CONTENT_ITEM_TYPE);
         }
 
         @Override
         public long insert(SQLiteDatabase db, long rawContactId, ContentValues values) {
             long dataId = super.insert(db, rawContactId, values);
             if (!isNewRawContact(rawContactId)) {
                 mContactAggregator.updatePhotoId(db, rawContactId);
             }
             return dataId;
         }
 
         @Override
         public void update(SQLiteDatabase db, ContentValues values, Cursor c,
                 boolean markRawContactAsDirty) {
             long rawContactId = c.getLong(DataUpdateQuery.RAW_CONTACT_ID);
             super.update(db, values, c, markRawContactAsDirty);
             mContactAggregator.updatePhotoId(db, rawContactId);
         }
 
         @Override
         public int delete(SQLiteDatabase db, Cursor c) {
             long rawContactId = c.getLong(DataDeleteQuery.RAW_CONTACT_ID);
             int count = super.delete(db, c);
             mContactAggregator.updatePhotoId(db, rawContactId);
             return count;
         }
 
         @Override
         public boolean isAggregationRequired() {
             return false;
         }
     }
 
 
     private HashMap<String, DataRowHandler> mDataRowHandlers;
     private final ContactAggregationScheduler mAggregationScheduler;
     private OpenHelper mOpenHelper;
 
     private NameSplitter mNameSplitter;
     private PostalSplitter mPostalSplitter;
 
     private ContactAggregator mContactAggregator;
     private LegacyApiSupport mLegacyApiSupport;
     private GlobalSearchSupport mGlobalSearchSupport;
 
     private ContentValues mValues = new ContentValues();
 
     private volatile CountDownLatch mAccessLatch;
     private boolean mImportMode;
 
     private boolean mScheduleAggregation;
     private HashSet<Long> mInsertedRawContacts = Sets.newHashSet();
     private HashSet<Long> mUpdatedRawContacts = Sets.newHashSet();
     private HashMap<Long, Object> mUpdatedSyncStates = Maps.newHashMap();
 
     public ContactsProvider2() {
         this(new ContactAggregationScheduler());
     }
 
     /**
      * Constructor for testing.
      */
     /* package */ ContactsProvider2(ContactAggregationScheduler scheduler) {
         mAggregationScheduler = scheduler;
     }
 
     @Override
     public boolean onCreate() {
         super.onCreate();
 
         final Context context = getContext();
         mOpenHelper = (OpenHelper)getOpenHelper();
         mGlobalSearchSupport = new GlobalSearchSupport(this);
         mLegacyApiSupport = new LegacyApiSupport(context, mOpenHelper, this, mGlobalSearchSupport);
         mContactAggregator = new ContactAggregator(this, mOpenHelper, mAggregationScheduler);
 
         final SQLiteDatabase db = mOpenHelper.getReadableDatabase();
 
         mSetPrimaryStatement = db.compileStatement(
                 "UPDATE " + Tables.DATA +
                 " SET " + Data.IS_PRIMARY + "=(_id=?)" +
                 " WHERE " + DataColumns.MIMETYPE_ID + "=?" +
                 "   AND " + Data.RAW_CONTACT_ID + "=?");
 
         mSetSuperPrimaryStatement = db.compileStatement(
                 "UPDATE " + Tables.DATA +
                 " SET " + Data.IS_SUPER_PRIMARY + "=(" + Data._ID + "=?)" +
                 " WHERE " + DataColumns.MIMETYPE_ID + "=?" +
                 "   AND " + Data.RAW_CONTACT_ID + " IN (" +
                         "SELECT " + RawContacts._ID +
                         " FROM " + Tables.RAW_CONTACTS +
                         " WHERE " + RawContacts.CONTACT_ID + " =(" +
                                 "SELECT " + RawContacts.CONTACT_ID +
                                 " FROM " + Tables.RAW_CONTACTS +
                                 " WHERE " + RawContacts._ID + "=?))");
 
         mLastTimeContactedUpdate = db.compileStatement("UPDATE " + Tables.RAW_CONTACTS + " SET "
                 + RawContacts.TIMES_CONTACTED + "=" + RawContacts.TIMES_CONTACTED + "+1,"
                 + RawContacts.LAST_TIME_CONTACTED + "=? WHERE " + RawContacts.CONTACT_ID + "=?");
 
         mRawContactDisplayNameUpdate = db.compileStatement(
                 "UPDATE " + Tables.RAW_CONTACTS +
                 " SET " + RawContactsColumns.DISPLAY_NAME + "=?,"
                         + RawContactsColumns.DISPLAY_NAME_SOURCE + "=?" +
                 " WHERE " + RawContacts._ID + "=?");
 
         mRawContactDirtyUpdate = db.compileStatement("UPDATE " + Tables.RAW_CONTACTS + " SET "
                 + RawContacts.DIRTY + "=1 WHERE " + RawContacts._ID + "=?");
 
         mAggregatedPresenceReplace = db.compileStatement(
                 "INSERT OR REPLACE INTO " + Tables.AGGREGATED_PRESENCE + "("
                         + AggregatedPresenceColumns.CONTACT_ID + ", "
                         + Presence.PRESENCE_STATUS
                 + ") VALUES (?, (SELECT MAX(" + Presence.PRESENCE_STATUS + ")"
                         + " FROM " + Tables.PRESENCE + "," + Tables.RAW_CONTACTS
                         + " WHERE " + PresenceColumns.RAW_CONTACT_ID + "="
                                 + RawContactsColumns.CONCRETE_ID
                         + "   AND " + RawContacts.CONTACT_ID + "=?))");
 
         mAggregatedPresenceStatusUpdate = db.compileStatement(
                 "UPDATE " + Tables.AGGREGATED_PRESENCE
                 + " SET " + Presence.PRESENCE_CUSTOM_STATUS + "=? "
                 + " WHERE " + AggregatedPresenceColumns.CONTACT_ID + "=?");
 
         final Locale locale = Locale.getDefault();
         mNameSplitter = new NameSplitter(
                 context.getString(com.android.internal.R.string.common_name_prefixes),
                 context.getString(com.android.internal.R.string.common_last_name_prefixes),
                 context.getString(com.android.internal.R.string.common_name_suffixes),
                 context.getString(com.android.internal.R.string.common_name_conjunctions),
                 locale);
         mPostalSplitter = new PostalSplitter(locale);
 
         mDataRowHandlers = new HashMap<String, DataRowHandler>();
 
         mDataRowHandlers.put(Email.CONTENT_ITEM_TYPE, new EmailDataRowHandler());
         mDataRowHandlers.put(Im.CONTENT_ITEM_TYPE,
                 new CommonDataRowHandler(Im.CONTENT_ITEM_TYPE, Im.TYPE, Im.LABEL));
         mDataRowHandlers.put(Nickname.CONTENT_ITEM_TYPE, new CommonDataRowHandler(
                 StructuredPostal.CONTENT_ITEM_TYPE, StructuredPostal.TYPE, StructuredPostal.LABEL));
         mDataRowHandlers.put(Organization.CONTENT_ITEM_TYPE, new OrganizationDataRowHandler());
         mDataRowHandlers.put(Phone.CONTENT_ITEM_TYPE, new PhoneDataRowHandler());
         mDataRowHandlers.put(Nickname.CONTENT_ITEM_TYPE, new NicknameDataRowHandler());
         mDataRowHandlers.put(StructuredName.CONTENT_ITEM_TYPE,
                 new StructuredNameRowHandler(mNameSplitter));
         mDataRowHandlers.put(StructuredPostal.CONTENT_ITEM_TYPE,
                 new StructuredPostalRowHandler(mPostalSplitter));
         mDataRowHandlers.put(GroupMembership.CONTENT_ITEM_TYPE, new GroupMembershipRowHandler());
         mDataRowHandlers.put(Photo.CONTENT_ITEM_TYPE, new PhotoDataRowHandler());
 
         if (isLegacyContactImportNeeded()) {
             importLegacyContactsAsync();
         }
 
         AccountManager.get(context).addOnAccountsUpdatedListener(this, null, false);
         onAccountsUpdated(AccountManager.get(context).getAccounts());
 
         return (db != null);
     }
 
     /* Visible for testing */
     @Override
     protected OpenHelper getOpenHelper(final Context context) {
         return OpenHelper.getInstance(context);
     }
 
     /* package */ ContactAggregationScheduler getContactAggregationScheduler() {
         return mAggregationScheduler;
     }
 
     /* package */ NameSplitter getNameSplitter() {
         return mNameSplitter;
     }
 
     protected boolean isLegacyContactImportNeeded() {
         SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
         return prefs.getInt(PREF_CONTACTS_IMPORTED, 0) < PREF_CONTACTS_IMPORT_VERSION;
     }
 
     protected LegacyContactImporter getLegacyContactImporter() {
         return new LegacyContactImporter(getContext(), this);
     }
 
     /**
      * Imports legacy contacts in a separate thread.  As long as the import process is running
      * all other access to the contacts is blocked.
      */
     private void importLegacyContactsAsync() {
         mAccessLatch = new CountDownLatch(1);
 
         Thread importThread = new Thread("LegacyContactImport") {
             @Override
             public void run() {
                 if (importLegacyContacts()) {
 
                     /*
                      * When the import process is done, we can unlock the provider and
                      * start aggregating the imported contacts asynchronously.
                      */
                     mAccessLatch.countDown();
                     mAccessLatch = null;
                     scheduleContactAggregation();
                 }
             }
         };
 
         importThread.start();
     }
 
     private boolean importLegacyContacts() {
         LegacyContactImporter importer = getLegacyContactImporter();
         if (importLegacyContacts(importer)) {
             SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
             Editor editor = prefs.edit();
             editor.putInt(PREF_CONTACTS_IMPORTED, PREF_CONTACTS_IMPORT_VERSION);
             editor.commit();
             return true;
         } else {
             return false;
         }
     }
 
     /* Visible for testing */
     /* package */ boolean importLegacyContacts(LegacyContactImporter importer) {
         mContactAggregator.setEnabled(false);
         mImportMode = true;
         try {
             importer.importContacts();
             mContactAggregator.setEnabled(true);
             return true;
         } catch (Throwable e) {
            Log.e(TAG, "Legacy contact import failed", e);
            return false;
         } finally {
             mImportMode = false;
         }
     }
 
     @Override
     protected void finalize() throws Throwable {
         if (mContactAggregator != null) {
             mContactAggregator.quit();
         }
 
         super.finalize();
     }
 
     /**
      * Wipes all data from the contacts database.
      */
     /* package */ void wipeData() {
         mOpenHelper.wipeData();
     }
 
     /**
      * While importing and aggregating contacts, this content provider will
      * block all attempts to change contacts data. In particular, it will hold
      * up all contact syncs. As soon as the import process is complete, all
      * processes waiting to write to the provider are unblocked and can proceed
      * to compete for the database transaction monitor.
      */
     private void waitForAccess() {
         CountDownLatch latch = mAccessLatch;
         if (latch != null) {
             while (true) {
                 try {
                     latch.await();
                     mAccessLatch = null;
                     return;
                 } catch (InterruptedException e) {
                 }
             }
         }
     }
 
     @Override
     public Uri insert(Uri uri, ContentValues values) {
         waitForAccess();
         return super.insert(uri, values);
     }
 
     @Override
     public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
         waitForAccess();
         return super.update(uri, values, selection, selectionArgs);
     }
 
     @Override
     public int delete(Uri uri, String selection, String[] selectionArgs) {
         waitForAccess();
         return super.delete(uri, selection, selectionArgs);
     }
 
     @Override
     public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations)
             throws OperationApplicationException {
         waitForAccess();
         return super.applyBatch(operations);
     }
 
     @Override
     protected void onBeginTransaction() {
         if (Log.isLoggable(TAG, Log.VERBOSE)) {
             Log.v(TAG, "onBeginTransaction");
         }
         super.onBeginTransaction();
         clearTransactionalChanges();
     }
 
     private void clearTransactionalChanges() {
         mInsertedRawContacts.clear();
         mUpdatedRawContacts.clear();
     }
 
     @Override
     protected void beforeTransactionCommit() {
         if (Log.isLoggable(TAG, Log.VERBOSE)) {
             Log.v(TAG, "beforeTransactionCommit");
         }
         super.beforeTransactionCommit();
         flushTransactionalChanges();
     }
 
     private void flushTransactionalChanges() {
         if (Log.isLoggable(TAG, Log.VERBOSE)) {
             Log.v(TAG, "flushTransactionChanges");
         }
         for (long rawContactId : mInsertedRawContacts) {
             mContactAggregator.insertContact(mDb, rawContactId);
         }
 
         String ids;
         if (!mUpdatedRawContacts.isEmpty()) {
             ids = buildIdsString(mUpdatedRawContacts);
             mDb.execSQL("UPDATE raw_contacts SET version = version + 1 WHERE _id in " + ids,
                     new Object[]{});
         }
 
         for (Map.Entry<Long, Object> entry : mUpdatedSyncStates.entrySet()) {
             long id = entry.getKey();
             mOpenHelper.getSyncState().update(mDb, id, entry.getValue());
         }
 
         clearTransactionalChanges();
     }
 
     private String buildIdsString(HashSet<Long> ids) {
         StringBuilder idsBuilder = null;
         for (long id : ids) {
             if (idsBuilder == null) {
                 idsBuilder = new StringBuilder();
                 idsBuilder.append("(");
             } else {
                 idsBuilder.append(",");
             }
             idsBuilder.append(id);
         }
         idsBuilder.append(")");
         return idsBuilder.toString();
     }
 
     @Override
     protected void onEndTransaction() {
         if (mScheduleAggregation) {
             mScheduleAggregation = false;
             scheduleContactAggregation();
         }
         super.onEndTransaction();
     }
 
     @Override
     protected void notifyChange() {
         getContext().getContentResolver().notifyChange(ContactsContract.AUTHORITY_URI, null);
     }
 
     protected void scheduleContactAggregation() {
         mContactAggregator.schedule();
     }
 
     private boolean isNewRawContact(long rawContactId) {
         return mInsertedRawContacts.contains(rawContactId);
     }
 
     private DataRowHandler getDataRowHandler(final String mimeType) {
         DataRowHandler handler = mDataRowHandlers.get(mimeType);
         if (handler == null) {
             handler = new CustomDataRowHandler(mimeType);
             mDataRowHandlers.put(mimeType, handler);
         }
         return handler;
     }
 
     @Override
     protected Uri insertInTransaction(Uri uri, ContentValues values) {
         if (Log.isLoggable(TAG, Log.VERBOSE)) {
             Log.v(TAG, "insertInTransaction: " + uri);
         }
         final int match = sUriMatcher.match(uri);
         long id = 0;
 
         switch (match) {
             case SYNCSTATE:
                 id = mOpenHelper.getSyncState().insert(mDb, values);
                 break;
 
             case CONTACTS: {
                 insertContact(values);
                 break;
             }
 
             case RAW_CONTACTS: {
                 final Account account = readAccountFromQueryParams(uri);
                 id = insertRawContact(values, account);
                 break;
             }
 
             case RAW_CONTACTS_DATA: {
                 values.put(Data.RAW_CONTACT_ID, uri.getPathSegments().get(1));
                 id = insertData(values, shouldMarkRawContactAsDirty(uri));
                 break;
             }
 
             case DATA: {
                 id = insertData(values, shouldMarkRawContactAsDirty(uri));
                 break;
             }
 
             case GROUPS: {
                 final Account account = readAccountFromQueryParams(uri);
                 id = insertGroup(values, account, shouldMarkGroupAsDirty(uri));
                 break;
             }
 
             case SETTINGS: {
                 id = insertSettings(values);
                 break;
             }
 
             case PRESENCE: {
                 id = insertPresence(values);
                 break;
             }
 
             default:
                 return mLegacyApiSupport.insert(uri, values);
         }
 
         if (id < 0) {
             return null;
         }
 
         return ContentUris.withAppendedId(uri, id);
     }
 
     /**
      * If account is non-null then store it in the values. If the account is already
      * specified in the values then it must be consistent with the account, if it is non-null.
      * @param values the ContentValues to read from and update
      * @param account the explicitly provided Account
      * @return false if the accounts are inconsistent
      */
     private boolean resolveAccount(ContentValues values, Account account) {
         // If either is specified then both must be specified.
         final String accountName = values.getAsString(RawContacts.ACCOUNT_NAME);
         final String accountType = values.getAsString(RawContacts.ACCOUNT_TYPE);
         if (!TextUtils.isEmpty(accountName) || !TextUtils.isEmpty(accountType)) {
             final Account valuesAccount = new Account(accountName, accountType);
             if (account != null && !valuesAccount.equals(account)) {
                 return false;
             }
             account = valuesAccount;
         }
         if (account != null) {
             values.put(RawContacts.ACCOUNT_NAME, account.name);
             values.put(RawContacts.ACCOUNT_TYPE, account.type);
         }
         return true;
     }
 
     /**
      * Inserts an item in the contacts table
      *
      * @param values the values for the new row
      * @return the row ID of the newly created row
      */
     private long insertContact(ContentValues values) {
         throw new UnsupportedOperationException("Aggregate contacts are created automatically");
     }
 
     /**
      * Inserts an item in the contacts table
      *
      * @param values the values for the new row
      * @param account the account this contact should be associated with. may be null.
      * @return the row ID of the newly created row
      */
     private long insertRawContact(ContentValues values, Account account) {
         ContentValues overriddenValues = new ContentValues(values);
         overriddenValues.putNull(RawContacts.CONTACT_ID);
         if (!resolveAccount(overriddenValues, account)) {
             return -1;
         }
 
         if (values.containsKey(RawContacts.DELETED)
                 && values.getAsInteger(RawContacts.DELETED) != 0) {
             overriddenValues.put(RawContacts.AGGREGATION_MODE,
                     RawContacts.AGGREGATION_MODE_DISABLED);
         }
 
         long rawContactId =
                 mDb.insert(Tables.RAW_CONTACTS, RawContacts.CONTACT_ID, overriddenValues);
         mContactAggregator.markNewForAggregation(rawContactId);
 
         // Trigger creation of a Contact based on this RawContact at the end of transaction
         mInsertedRawContacts.add(rawContactId);
         return rawContactId;
     }
 
     /**
      * Inserts an item in the data table
      *
      * @param values the values for the new row
      * @return the row ID of the newly created row
      */
     private long insertData(ContentValues values, boolean markRawContactAsDirty) {
         long id = 0;
         mValues.clear();
         mValues.putAll(values);
 
         long rawContactId = mValues.getAsLong(Data.RAW_CONTACT_ID);
 
         // Replace package with internal mapping
         final String packageName = mValues.getAsString(Data.RES_PACKAGE);
         if (packageName != null) {
             mValues.put(DataColumns.PACKAGE_ID, mOpenHelper.getPackageId(packageName));
         }
         mValues.remove(Data.RES_PACKAGE);
 
         // Replace mimetype with internal mapping
         final String mimeType = mValues.getAsString(Data.MIMETYPE);
         if (TextUtils.isEmpty(mimeType)) {
             throw new IllegalArgumentException(Data.MIMETYPE + " is required");
         }
 
         mValues.put(DataColumns.MIMETYPE_ID, mOpenHelper.getMimeTypeId(mimeType));
         mValues.remove(Data.MIMETYPE);
 
         DataRowHandler rowHandler = getDataRowHandler(mimeType);
         id = rowHandler.insert(mDb, rawContactId, mValues);
         if (markRawContactAsDirty) {
             setRawContactDirty(rawContactId);
         }
         mUpdatedRawContacts.add(rawContactId);
 
         if (rowHandler.isAggregationRequired()) {
             triggerAggregation(rawContactId);
         }
         return id;
     }
 
     private void triggerAggregation(long rawContactId) {
         if (!mContactAggregator.isEnabled()) {
             return;
         }
 
         int aggregationMode = mOpenHelper.getAggregationMode(rawContactId);
         switch (aggregationMode) {
             case RawContacts.AGGREGATION_MODE_DISABLED:
                 break;
 
             case RawContacts.AGGREGATION_MODE_DEFAULT: {
                 mContactAggregator.markForAggregation(rawContactId);
                 mScheduleAggregation = true;
                 break;
             }
 
             case RawContacts.AGGREGATION_MODE_SUSPENDED: {
                 long contactId = mOpenHelper.getContactId(rawContactId);
 
                 if (contactId != 0) {
                     mContactAggregator.updateAggregateData(contactId);
                 }
                 break;
             }
 
            case RawContacts.AGGREGATION_MODE_IMMEDITATE: {
                 long contactId = mOpenHelper.getContactId(rawContactId);
                 mContactAggregator.aggregateContact(mDb, rawContactId, contactId);
                 break;
             }
         }
     }
 
     /**
      * Returns the group id of the group with sourceId and the same account as rawContactId.
      * If the group doesn't already exist then it is first created,
      * @param db SQLiteDatabase to use for this operation
      * @param rawContactId the contact this group is associated with
      * @param sourceId the sourceIf of the group to query or create
      * @return the group id of the existing or created group
      * @throws IllegalArgumentException if the contact is not associated with an account
      * @throws IllegalStateException if a group needs to be created but the creation failed
      */
     private long getOrMakeGroup(SQLiteDatabase db, long rawContactId, String sourceId) {
         Account account = null;
         Cursor c = db.query(ContactsQuery.TABLE, ContactsQuery.PROJECTION, RawContacts._ID + "="
                 + rawContactId, null, null, null, null);
         try {
             if (c.moveToNext()) {
                 final String accountName = c.getString(ContactsQuery.ACCOUNT_NAME);
                 final String accountType = c.getString(ContactsQuery.ACCOUNT_TYPE);
                 if (!TextUtils.isEmpty(accountName) && !TextUtils.isEmpty(accountType)) {
                     account = new Account(accountName, accountType);
                 }
             }
         } finally {
             c.close();
         }
         if (account == null) {
             throw new IllegalArgumentException("if the groupmembership only "
                     + "has a sourceid the the contact must be associate with "
                     + "an account");
         }
 
         // look up the group that contains this sourceId and has the same account name and type
         // as the contact refered to by rawContactId
         c = db.query(Tables.GROUPS, new String[]{RawContacts._ID},
                 Clauses.GROUP_HAS_ACCOUNT_AND_SOURCE_ID,
                 new String[]{sourceId, account.name, account.type}, null, null, null);
         try {
             if (c.moveToNext()) {
                 return c.getLong(0);
             } else {
                 ContentValues groupValues = new ContentValues();
                 groupValues.put(Groups.ACCOUNT_NAME, account.name);
                 groupValues.put(Groups.ACCOUNT_TYPE, account.type);
                 groupValues.put(Groups.SOURCE_ID, sourceId);
                 long groupId = db.insert(Tables.GROUPS, Groups.ACCOUNT_NAME, groupValues);
                 if (groupId < 0) {
                     throw new IllegalStateException("unable to create a new group with "
                             + "this sourceid: " + groupValues);
                 }
                 return groupId;
             }
         } finally {
             c.close();
         }
     }
 
     /**
      * Delete data row by row so that fixing of primaries etc work correctly.
      */
     private int deleteData(String selection, String[] selectionArgs,
             boolean markRawContactAsDirty) {
         int count = 0;
 
         // Note that the query will return data according to the access restrictions,
         // so we don't need to worry about deleting data we don't have permission to read.
         Cursor c = query(Data.CONTENT_URI, DataDeleteQuery.COLUMNS, selection, selectionArgs, null);
         try {
             while(c.moveToNext()) {
                 long rawContactId = c.getLong(DataDeleteQuery.RAW_CONTACT_ID);
                 String mimeType = c.getString(DataDeleteQuery.MIMETYPE);
                 DataRowHandler rowHandler = getDataRowHandler(mimeType);
                 count += rowHandler.delete(mDb, c);
                 if (markRawContactAsDirty) {
                     setRawContactDirty(rawContactId);
                     if (rowHandler.isAggregationRequired()) {
                         triggerAggregation(rawContactId);
                     }
                 }
             }
         } finally {
             c.close();
         }
 
         return count;
     }
 
     /**
      * Delete a data row provided that it is one of the allowed mime types.
      */
     public int deleteData(long dataId, String[] allowedMimeTypes) {
 
         // Note that the query will return data according to the access restrictions,
         // so we don't need to worry about deleting data we don't have permission to read.
         Cursor c = query(Data.CONTENT_URI, DataDeleteQuery.COLUMNS, Data._ID + "=" + dataId, null,
                 null);
 
         try {
             if (!c.moveToFirst()) {
                 return 0;
             }
 
             String mimeType = c.getString(DataDeleteQuery.MIMETYPE);
             boolean valid = false;
             for (int i = 0; i < allowedMimeTypes.length; i++) {
                 if (TextUtils.equals(mimeType, allowedMimeTypes[i])) {
                     valid = true;
                     break;
                 }
             }
 
             if (!valid) {
                 throw new IllegalArgumentException("Data type mismatch: expected "
                         + Lists.newArrayList(allowedMimeTypes));
             }
 
             DataRowHandler rowHandler = getDataRowHandler(mimeType);
             int count = rowHandler.delete(mDb, c);
             long rawContactId = c.getLong(DataDeleteQuery.RAW_CONTACT_ID);
             if (rowHandler.isAggregationRequired()) {
                 triggerAggregation(rawContactId);
             }
             return count;
         } finally {
             c.close();
         }
     }
 
     /**
      * Inserts an item in the groups table
      */
     private long insertGroup(ContentValues values, Account account, boolean markAsDirty) {
         ContentValues overriddenValues = new ContentValues(values);
         if (!resolveAccount(overriddenValues, account)) {
             return -1;
         }
 
         // Replace package with internal mapping
         final String packageName = overriddenValues.getAsString(Groups.RES_PACKAGE);
         if (packageName != null) {
             overriddenValues.put(GroupsColumns.PACKAGE_ID, mOpenHelper.getPackageId(packageName));
         }
         overriddenValues.remove(Groups.RES_PACKAGE);
 
         if (markAsDirty) {
             overriddenValues.put(Groups.DIRTY, 1);
         }
 
         long result = mDb.insert(Tables.GROUPS, Groups.TITLE, overriddenValues);
 
         if (overriddenValues.containsKey(Groups.GROUP_VISIBLE)) {
             mOpenHelper.updateAllVisible();
         }
 
         return result;
     }
 
     private long insertSettings(ContentValues values) {
         final long id = mDb.insert(Tables.SETTINGS, null, values);
         if (values.containsKey(Settings.UNGROUPED_VISIBLE)) {
             mOpenHelper.updateAllVisible();
         }
         return id;
     }
 
     /**
      * Inserts a presence update.
      */
     public long insertPresence(ContentValues values) {
         final String handle = values.getAsString(Presence.IM_HANDLE);
         if (TextUtils.isEmpty(handle) || !values.containsKey(Presence.PROTOCOL)) {
             throw new IllegalArgumentException("PROTOCOL and IM_HANDLE are required");
         }
 
         final long protocol = values.getAsLong(Presence.PROTOCOL);
         String customProtocol = null;
 
         if (protocol == Im.PROTOCOL_CUSTOM) {
             customProtocol = values.getAsString(Presence.CUSTOM_PROTOCOL);
             if (TextUtils.isEmpty(customProtocol)) {
                 throw new IllegalArgumentException(
                         "CUSTOM_PROTOCOL is required when PROTOCOL=PROTOCOL_CUSTOM");
             }
         }
 
         // TODO: generalize to allow other providers to match against email
         boolean matchEmail = Im.PROTOCOL_GOOGLE_TALK == protocol;
 
         StringBuilder selection = new StringBuilder();
         String[] selectionArgs;
         if (matchEmail) {
             selection.append(
                     "((" + MimetypesColumns.MIMETYPE + "='" + Im.CONTENT_ITEM_TYPE + "'"
                     + " AND " + Im.PROTOCOL + "=?"
                     + " AND " + Im.DATA + "=?");
             if (customProtocol != null) {
                 selection.append(" AND " + Im.CUSTOM_PROTOCOL + "=");
                 DatabaseUtils.appendEscapedSQLString(selection, customProtocol);
             }
             selection.append(") OR ("
                     + MimetypesColumns.MIMETYPE + "='" + Email.CONTENT_ITEM_TYPE + "'"
                     + " AND " + Email.DATA + "=?"
                     + "))");
             selectionArgs = new String[] { String.valueOf(protocol), handle, handle };
         } else {
             selection.append(
                     MimetypesColumns.MIMETYPE + "='" + Im.CONTENT_ITEM_TYPE + "'"
                     + " AND " + Im.PROTOCOL + "=?"
                     + " AND " + Im.DATA + "=?");
             if (customProtocol != null) {
                 selection.append(" AND " + Im.CUSTOM_PROTOCOL + "=");
                 DatabaseUtils.appendEscapedSQLString(selection, customProtocol);
             }
 
             selectionArgs = new String[] { String.valueOf(protocol), handle };
         }
 
         if (values.containsKey(Presence.DATA_ID)) {
             selection.append(" AND " + DataColumns.CONCRETE_ID + "=")
                     .append(values.getAsLong(Presence.DATA_ID));
         }
 
         selection.append(" AND ").append(getContactsRestrictions());
 
         long dataId = -1;
         long rawContactId = -1;
         long contactId = -1;
 
         Cursor cursor = null;
         try {
             cursor = mDb.query(DataContactsQuery.TABLE, DataContactsQuery.PROJECTION,
                     selection.toString(), selectionArgs, null, null, null);
             if (cursor.moveToFirst()) {
                 dataId = cursor.getLong(DataContactsQuery.DATA_ID);
                 rawContactId = cursor.getLong(DataContactsQuery.RAW_CONTACT_ID);
                 contactId = cursor.getLong(DataContactsQuery.CONTACT_ID);
             } else {
                 // No contact found, return a null URI
                 return -1;
             }
         } finally {
             if (cursor != null) {
                 cursor.close();
             }
         }
 
         values.put(Presence.DATA_ID, dataId);
         values.put(PresenceColumns.RAW_CONTACT_ID, rawContactId);
 
         // Insert the presence update
         long presenceId = mDb.replace(Tables.PRESENCE, null, values);
 
         if (contactId != -1) {
             if (values.containsKey(Presence.PRESENCE_STATUS)) {
                 mAggregatedPresenceReplace.bindLong(1, contactId);
                 mAggregatedPresenceReplace.bindLong(2, contactId);
                 mAggregatedPresenceReplace.execute();
             }
             String status = values.getAsString(Presence.PRESENCE_CUSTOM_STATUS);
             if (status != null) {
                 mAggregatedPresenceStatusUpdate.bindString(1, status);
                 mAggregatedPresenceStatusUpdate.bindLong(2, contactId);
                 mAggregatedPresenceStatusUpdate.execute();
             }
         }
         return presenceId;
     }
 
     @Override
     protected int deleteInTransaction(Uri uri, String selection, String[] selectionArgs) {
         if (Log.isLoggable(TAG, Log.VERBOSE)) {
             Log.v(TAG, "deleteInTransaction: " + uri);
         }
         flushTransactionalChanges();
         final int match = sUriMatcher.match(uri);
         switch (match) {
             case SYNCSTATE:
                 return mOpenHelper.getSyncState().delete(mDb, selection, selectionArgs);
 
             case SYNCSTATE_ID:
                 String selectionWithId =
                         (SyncStateContract.Columns._ID + "=" + ContentUris.parseId(uri) + " ")
                         + (selection == null ? "" : " AND (" + selection + ")");
                 return mOpenHelper.getSyncState().delete(mDb, selectionWithId, selectionArgs);
 
             case CONTACTS_ID: {
                 long contactId = ContentUris.parseId(uri);
 
                 // Remove references to the contact first
                 ContentValues values = new ContentValues();
                 values.putNull(RawContacts.CONTACT_ID);
                 mDb.update(Tables.RAW_CONTACTS, values,
                         RawContacts.CONTACT_ID + "=" + contactId, null);
 
                 return mDb.delete(Tables.CONTACTS, BaseColumns._ID + "=" + contactId, null);
             }
 
             case RAW_CONTACTS: {
                 final boolean permanently =
                         readBooleanQueryParameter(uri, RawContacts.DELETE_PERMANENTLY, false);
                 int numDeletes = 0;
                 Cursor c = mDb.query(Tables.RAW_CONTACTS, new String[]{RawContacts._ID},
                         appendAccountToSelection(uri, selection), selectionArgs, null, null, null);
                 try {
                     while (c.moveToNext()) {
                         final long rawContactId = c.getLong(0);
                         numDeletes += deleteRawContact(rawContactId, permanently);
                     }
                 } finally {
                     c.close();
                 }
                 return numDeletes;
             }
 
             case RAW_CONTACTS_ID: {
                 final boolean permanently =
                         readBooleanQueryParameter(uri, RawContacts.DELETE_PERMANENTLY, false);
                 final long rawContactId = ContentUris.parseId(uri);
                 return deleteRawContact(rawContactId, permanently);
             }
 
             case DATA: {
                 return deleteData(appendAccountToSelection(uri, selection), selectionArgs,
                         shouldMarkRawContactAsDirty(uri));
             }
 
             case DATA_ID: {
                 long dataId = ContentUris.parseId(uri);
                 return deleteData(Data._ID + "=" + dataId, null, shouldMarkRawContactAsDirty(uri));
             }
 
             case GROUPS_ID: {
                 boolean markAsDirty = shouldMarkGroupAsDirty(uri);
                 final boolean deletePermanently =
                         readBooleanQueryParameter(uri, Groups.DELETE_PERMANENTLY, false);
                 return deleteGroup(ContentUris.parseId(uri), markAsDirty, deletePermanently);
             }
 
             case GROUPS: {
                 boolean markAsDirty = shouldMarkGroupAsDirty(uri);
                 final boolean permanently =
                         readBooleanQueryParameter(uri, RawContacts.DELETE_PERMANENTLY, false);
                 int numDeletes = 0;
                 Cursor c = mDb.query(Tables.GROUPS, new String[]{Groups._ID},
                         appendAccountToSelection(uri, selection), selectionArgs, null, null, null);
                 try {
                     while (c.moveToNext()) {
                         numDeletes += deleteGroup(c.getLong(0), markAsDirty, permanently);
                     }
                 } finally {
                     c.close();
                 }
                 return numDeletes;
             }
 
             case SETTINGS: {
                 return deleteSettings(selection, selectionArgs);
             }
 
             case PRESENCE: {
                 return mDb.delete(Tables.PRESENCE, selection, selectionArgs);
             }
 
             default:
                 return mLegacyApiSupport.delete(uri, selection, selectionArgs);
         }
     }
 
     private boolean readBooleanQueryParameter(Uri uri, String name, boolean defaultValue) {
         final String flag = uri.getQueryParameter(name);
         return flag == null
                 ? defaultValue
                 : (!"false".equals(flag.toLowerCase()) && !"0".equals(flag.toLowerCase()));
     }
 
     private int deleteGroup(long groupId, boolean markAsDirty, boolean permanently) {
         final long groupMembershipMimetypeId = mOpenHelper
                 .getMimeTypeId(GroupMembership.CONTENT_ITEM_TYPE);
         mDb.delete(Tables.DATA, DataColumns.MIMETYPE_ID + "="
                 + groupMembershipMimetypeId + " AND " + GroupMembership.GROUP_ROW_ID + "="
                 + groupId, null);
 
         try {
             if (permanently) {
                 return mDb.delete(Tables.GROUPS, Groups._ID + "=" + groupId, null);
             } else {
                 mValues.clear();
                 mValues.put(Groups.DELETED, 1);
                 if (markAsDirty) {
                     mValues.put(Groups.DIRTY, 1);
                 }
                 return mDb.update(Tables.GROUPS, mValues, Groups._ID + "=" + groupId, null);
             }
         } finally {
             mOpenHelper.updateAllVisible();
         }
     }
 
     private int deleteSettings(String selection, String[] selectionArgs) {
         final int count = mDb.delete(Tables.SETTINGS, selection, selectionArgs);
         if (count > 0) {
             mOpenHelper.updateAllVisible();
         }
         return count;
     }
 
     public int deleteRawContact(long rawContactId, boolean permanently) {
         if (permanently) {
             mDb.delete(Tables.PRESENCE, PresenceColumns.RAW_CONTACT_ID + "=" + rawContactId, null);
             return mDb.delete(Tables.RAW_CONTACTS, RawContacts._ID + "=" + rawContactId, null);
         } else {
             mOpenHelper.removeContactIfSingleton(rawContactId);
 
             mValues.clear();
             mValues.put(RawContacts.DELETED, 1);
             mValues.put(RawContacts.AGGREGATION_MODE, RawContacts.AGGREGATION_MODE_DISABLED);
             mValues.put(RawContactsColumns.AGGREGATION_NEEDED, 1);
             mValues.putNull(RawContacts.CONTACT_ID);
             mValues.put(RawContacts.DIRTY, 1);
             return updateRawContact(rawContactId, mValues, null, null);
         }
     }
 
     private static Account readAccountFromQueryParams(Uri uri) {
         final String name = uri.getQueryParameter(RawContacts.ACCOUNT_NAME);
         final String type = uri.getQueryParameter(RawContacts.ACCOUNT_TYPE);
         if (TextUtils.isEmpty(name) || TextUtils.isEmpty(type)) {
             return null;
         }
         return new Account(name, type);
     }
 
     @Override
     protected int updateInTransaction(Uri uri, ContentValues values, String selection,
             String[] selectionArgs) {
         if (Log.isLoggable(TAG, Log.VERBOSE)) {
             Log.v(TAG, "updateInTransaction: " + uri);
         }
 
         int count = 0;
 
         final int match = sUriMatcher.match(uri);
         if (match == SYNCSTATE_ID && selection == null) {
             long rowId = ContentUris.parseId(uri);
             Object data = values.get(ContactsContract.SyncStateColumns.DATA);
             mUpdatedSyncStates.put(rowId, data);
             return 1;
         }
         flushTransactionalChanges();
         switch(match) {
             case SYNCSTATE:
                 return mOpenHelper.getSyncState().update(mDb, values,
                         appendAccountToSelection(uri, selection), selectionArgs);
 
             case SYNCSTATE_ID: {
                 selection = appendAccountToSelection(uri, selection);
                 String selectionWithId =
                         (SyncStateContract.Columns._ID + "=" + ContentUris.parseId(uri) + " ")
                         + (selection == null ? "" : " AND (" + selection + ")");
                 return mOpenHelper.getSyncState().update(mDb, values,
                         selectionWithId, selectionArgs);
             }
 
             // TODO(emillar): We will want to disallow editing the contacts table at some point.
             case CONTACTS: {
                 count = mDb.update(Tables.CONTACTS, values,
                         appendAccountToSelection(uri, selection), selectionArgs);
                 break;
             }
 
             case CONTACTS_ID: {
                 count = updateContactData(ContentUris.parseId(uri), values);
                 break;
             }
 
             case DATA: {
                 count = updateData(uri, values, appendAccountToSelection(uri, selection),
                         selectionArgs, shouldMarkRawContactAsDirty(uri));
                 break;
             }
 
             case DATA_ID: {
                 count = updateData(uri, values, selection, selectionArgs,
                         shouldMarkRawContactAsDirty(uri));
                 break;
             }
 
             case RAW_CONTACTS: {
                 // TODO: security checks
                 count = mDb.update(Tables.RAW_CONTACTS, values,
                         appendAccountToSelection(uri, selection), selectionArgs);
 
                 if (values.containsKey(RawContacts.STARRED)) {
                     mContactAggregator.updateStarred(mDb, selection, selectionArgs);
                 }
                 break;
             }
 
             case RAW_CONTACTS_ID: {
                 long rawContactId = ContentUris.parseId(uri);
                 count = updateRawContact(rawContactId, values, selection, selectionArgs);
                 break;
             }
 
             case GROUPS: {
                 count = updateGroups(values, appendAccountToSelection(uri, selection),
                         selectionArgs, shouldMarkGroupAsDirty(uri));
                 break;
             }
 
             case GROUPS_ID: {
                 long groupId = ContentUris.parseId(uri);
                 String selectionWithId = (Groups._ID + "=" + groupId + " ")
                         + (selection == null ? "" : " AND " + selection);
                 count = updateGroups(values, selectionWithId, selectionArgs,
                         shouldMarkGroupAsDirty(uri));
                 break;
             }
 
             case AGGREGATION_EXCEPTIONS: {
                 count = updateAggregationException(mDb, values);
                 break;
             }
 
             case SETTINGS: {
                 count = updateSettings(values, selection, selectionArgs);
                 break;
             }
 
             default:
                 return mLegacyApiSupport.update(uri, values, selection, selectionArgs);
         }
 
         return count;
     }
 
     private int updateGroups(ContentValues values, String selectionWithId,
             String[] selectionArgs, boolean markAsDirty) {
 
         ContentValues updatedValues;
         if (markAsDirty && !values.containsKey(Groups.DIRTY)) {
             updatedValues = mValues;
             updatedValues.clear();
             updatedValues.putAll(values);
             updatedValues.put(Groups.DIRTY, 1);
         } else {
             updatedValues = values;
         }
 
         int count = mDb.update(Tables.GROUPS, updatedValues, selectionWithId, selectionArgs);
 
         // If changing visibility, then update contacts
         if (updatedValues.containsKey(Groups.GROUP_VISIBLE)) {
             mOpenHelper.updateAllVisible();
         }
         return count;
     }
 
     private int updateSettings(ContentValues values, String selection, String[] selectionArgs) {
         final int count = mDb.update(Tables.SETTINGS, values, selection, selectionArgs);
         if (values.containsKey(Settings.UNGROUPED_VISIBLE)) {
             mOpenHelper.updateAllVisible();
         }
         return count;
     }
 
     private int updateRawContact(long rawContactId, ContentValues values, String selection,
             String[] selectionArgs) {
 
         // TODO: security checks
         String selectionWithId = (RawContacts._ID + " = " + rawContactId + " ")
                 + (selection == null ? "" : " AND " + selection);
         int count = mDb.update(Tables.RAW_CONTACTS, values, selectionWithId, selectionArgs);
         if (count != 0) {
             if (values.containsKey(RawContacts.ACCOUNT_TYPE)
                     || values.containsKey(RawContacts.ACCOUNT_NAME)
                     || values.containsKey(RawContacts.SOURCE_ID)) {
                 triggerAggregation(rawContactId);
             }
 
             if (values.containsKey(RawContacts.STARRED)) {
                 mContactAggregator.updateStarred(mDb, selectionWithId, selectionArgs);
             }
             if (values.containsKey(RawContacts.SOURCE_ID)) {
                 mContactAggregator.updateLookupKey(mDb, rawContactId);
             }
         }
         return count;
     }
 
     private int updateData(Uri uri, ContentValues values, String selection,
             String[] selectionArgs, boolean markRawContactAsDirty) {
         mValues.clear();
         mValues.putAll(values);
         mValues.remove(Data._ID);
         mValues.remove(Data.RAW_CONTACT_ID);
         mValues.remove(Data.MIMETYPE);
 
         String packageName = values.getAsString(Data.RES_PACKAGE);
         if (packageName != null) {
             mValues.remove(Data.RES_PACKAGE);
             mValues.put(DataColumns.PACKAGE_ID, mOpenHelper.getPackageId(packageName));
         }
 
         boolean containsIsSuperPrimary = mValues.containsKey(Data.IS_SUPER_PRIMARY);
         boolean containsIsPrimary = mValues.containsKey(Data.IS_PRIMARY);
 
         // Remove primary or super primary values being set to 0. This is disallowed by the
         // content provider.
         if (containsIsSuperPrimary && mValues.getAsInteger(Data.IS_SUPER_PRIMARY) == 0) {
             containsIsSuperPrimary = false;
             mValues.remove(Data.IS_SUPER_PRIMARY);
         }
         if (containsIsPrimary && mValues.getAsInteger(Data.IS_PRIMARY) == 0) {
             containsIsPrimary = false;
             mValues.remove(Data.IS_PRIMARY);
         }
 
         int count = 0;
 
         // Note that the query will return data according to the access restrictions,
         // so we don't need to worry about updating data we don't have permission to read.
         Cursor c = query(uri, DataUpdateQuery.COLUMNS, selection, selectionArgs, null);
         try {
             while(c.moveToNext()) {
                 count += updateData(mValues, c, markRawContactAsDirty);
             }
         } finally {
             c.close();
         }
 
         return count;
     }
 
     private int updateData(ContentValues values, Cursor c, boolean markRawContactAsDirty) {
         if (values.size() == 0) {
             return 0;
         }
 
         final String mimeType = c.getString(DataUpdateQuery.MIMETYPE);
         DataRowHandler rowHandler = getDataRowHandler(mimeType);
         rowHandler.update(mDb, values, c, markRawContactAsDirty);
         long rawContactId = c.getLong(DataUpdateQuery.RAW_CONTACT_ID);
         if (rowHandler.isAggregationRequired()) {
             triggerAggregation(rawContactId);
         }
 
         return 1;
     }
 
     private int updateContactData(long contactId, ContentValues values) {
 
         // First update all constituent contacts
         ContentValues optionValues = new ContentValues(5);
         OpenHelper.copyStringValue(optionValues, RawContacts.CUSTOM_RINGTONE,
                 values, Contacts.CUSTOM_RINGTONE);
         OpenHelper.copyLongValue(optionValues, RawContacts.SEND_TO_VOICEMAIL,
                 values, Contacts.SEND_TO_VOICEMAIL);
         OpenHelper.copyLongValue(optionValues, RawContacts.LAST_TIME_CONTACTED,
                 values, Contacts.LAST_TIME_CONTACTED);
         OpenHelper.copyLongValue(optionValues, RawContacts.TIMES_CONTACTED,
                 values, Contacts.TIMES_CONTACTED);
         OpenHelper.copyLongValue(optionValues, RawContacts.STARRED,
                 values, Contacts.STARRED);
 
         // Nothing to update - just return
         if (optionValues.size() == 0) {
             return 0;
         }
 
         if (optionValues.containsKey(RawContacts.STARRED)) {
             // Mark dirty when changing starred to trigger sync
             optionValues.put(RawContacts.DIRTY, 1);
         }
 
         mDb.update(Tables.RAW_CONTACTS, optionValues,
                 RawContacts.CONTACT_ID + "=" + contactId, null);
         return mDb.update(Tables.CONTACTS, values, Contacts._ID + "=" + contactId, null);
     }
 
     public void updateContactTime(long contactId, long lastTimeContacted) {
         mLastTimeContactedUpdate.bindLong(1, lastTimeContacted);
         mLastTimeContactedUpdate.bindLong(2, contactId);
         mLastTimeContactedUpdate.execute();
     }
 
     private int updateAggregationException(SQLiteDatabase db, ContentValues values) {
         int exceptionType = values.getAsInteger(AggregationExceptions.TYPE);
         long rcId1 = values.getAsInteger(AggregationExceptions.RAW_CONTACT_ID1);
         long rcId2 = values.getAsInteger(AggregationExceptions.RAW_CONTACT_ID2);
 
         long rawContactId1, rawContactId2;
         if (rcId1 < rcId2) {
             rawContactId1 = rcId1;
             rawContactId2 = rcId2;
         } else {
             rawContactId2 = rcId1;
             rawContactId1 = rcId2;
         }
 
         ContentValues exceptionValues = new ContentValues(3);
         exceptionValues.put(AggregationExceptions.TYPE, exceptionType);
         if (exceptionType == AggregationExceptions.TYPE_AUTOMATIC) {
             db.delete(Tables.AGGREGATION_EXCEPTIONS,
                     AggregationExceptions.RAW_CONTACT_ID1 + "=" + rawContactId1 + " AND "
                     + AggregationExceptions.RAW_CONTACT_ID2 + "=" + rawContactId2, null);
         } else {
             exceptionValues.put(AggregationExceptions.RAW_CONTACT_ID1, rawContactId1);
             exceptionValues.put(AggregationExceptions.RAW_CONTACT_ID2, rawContactId2);
             db.replace(Tables.AGGREGATION_EXCEPTIONS, AggregationExceptions._ID,
                     exceptionValues);
         }
 
         long contactId1 = mOpenHelper.getContactId(rawContactId1);
         mContactAggregator.aggregateContact(db, rawContactId1, contactId1);
 
         long contactId2 = mOpenHelper.getContactId(rawContactId2);
         mContactAggregator.aggregateContact(db, rawContactId2, contactId2);
 
         // The return value is fake - we just confirm that we made a change, not count actual
         // rows changed.
         return 1;
     }
 
     public void onAccountsUpdated(Account[] accounts) {
         mDb = mOpenHelper.getWritableDatabase();
         if (mDb == null) return;
 
         Set<Account> validAccounts = Sets.newHashSet();
         for (Account account : accounts) {
             validAccounts.add(new Account(account.name, account.type));
         }
         ArrayList<Account> accountsToDelete = new ArrayList<Account>();
 
         mDb.beginTransaction();
         try {
             // Find all the accounts the contacts DB knows about, mark the ones that aren't in the
             // valid set for deletion.
             Cursor c = mDb.rawQuery("SELECT DISTINCT account_name, account_type from "
                     + Tables.RAW_CONTACTS, null);
             while (c.moveToNext()) {
                 if (c.getString(0) != null && c.getString(1) != null) {
                     Account currAccount = new Account(c.getString(0), c.getString(1));
                     if (!validAccounts.contains(currAccount)) {
                         accountsToDelete.add(currAccount);
                     }
                 }
             }
             c.close();
 
             for (Account account : accountsToDelete) {
                 String[] params = new String[]{account.name, account.type};
                 mDb.execSQL("DELETE FROM " + Tables.GROUPS
                         + " WHERE account_name = ? AND account_type = ?", params);
                 mDb.execSQL("DELETE FROM " + Tables.PRESENCE
                         + " WHERE " + PresenceColumns.RAW_CONTACT_ID + " IN (SELECT "
                         + RawContacts._ID + " FROM " + Tables.RAW_CONTACTS
                         + " WHERE account_name = ? AND account_type = ?)", params);
                 mDb.execSQL("DELETE FROM " + Tables.RAW_CONTACTS
                         + " WHERE account_name = ? AND account_type = ?", params);
             }
             mDb.setTransactionSuccessful();
         } finally {
             mDb.endTransaction();
         }
     }
 
     /**
      * Test all against {@link TextUtils#isEmpty(CharSequence)}.
      */
     private static boolean areAllEmpty(ContentValues values, String[] keys) {
         for (String key : keys) {
             if (!TextUtils.isEmpty(values.getAsString(key))) {
                 return false;
             }
         }
         return true;
     }
 
     @Override
     public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
             String sortOrder) {
 
         final SQLiteDatabase db = mOpenHelper.getReadableDatabase();
 
         SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
         String groupBy = null;
         String limit = getLimit(uri);
 
         // TODO: Consider writing a test case for RestrictionExceptions when you
         // write a new query() block to make sure it protects restricted data.
         final int match = sUriMatcher.match(uri);
         switch (match) {
             case SYNCSTATE:
                 return mOpenHelper.getSyncState().query(db, projection, selection,  selectionArgs,
                         sortOrder);
 
             case CONTACTS: {
                 setTablesAndProjectionMapForContacts(qb, projection);
                 break;
             }
 
             case CONTACTS_ID: {
                 long contactId = ContentUris.parseId(uri);
                 setTablesAndProjectionMapForContacts(qb, projection);
                 qb.appendWhere(Contacts._ID + "=" + contactId);
                 break;
             }
 
             case CONTACTS_LOOKUP:
             case CONTACTS_LOOKUP_ID: {
                 List<String> pathSegments = uri.getPathSegments();
                 int segmentCount = pathSegments.size();
                 if (segmentCount < 3) {
                     throw new IllegalArgumentException("URI " + uri + " is missing a lookup key");
                 }
                 String lookupKey = pathSegments.get(2);
                 if (segmentCount == 4) {
                     long contactId = Long.parseLong(pathSegments.get(3));
                     SQLiteQueryBuilder lookupQb = new SQLiteQueryBuilder();
                     setTablesAndProjectionMapForContacts(lookupQb, projection);
                     lookupQb.appendWhere(Contacts._ID + "=" + contactId + " AND " +
                             Contacts.LOOKUP_KEY + "=");
                     lookupQb.appendWhereEscapeString(lookupKey);
                     Cursor c = query(db, lookupQb, projection, selection, selectionArgs, sortOrder,
                             groupBy, limit);
                     if (c.getCount() != 0) {
                         return c;
                     }
 
                     c.close();
                 }
 
                 setTablesAndProjectionMapForContacts(qb, projection);
                 qb.appendWhere(Contacts._ID + "=" + lookupContactIdByLookupKey(db, lookupKey));
                 break;
             }
 
             case CONTACTS_FILTER: {
                 setTablesAndProjectionMapForContacts(qb, projection);
                 if (uri.getPathSegments().size() > 2) {
                     String filterParam = uri.getLastPathSegment();
                     StringBuilder sb = new StringBuilder();
                     sb.append(Contacts._ID + " IN ");
                     appendContactFilterAsNestedQuery(sb, filterParam);
                     qb.appendWhere(sb.toString());
                 }
                 break;
             }
 
             case CONTACTS_STREQUENT_FILTER:
             case CONTACTS_STREQUENT: {
                 String filterSql = null;
                 if (match == CONTACTS_STREQUENT_FILTER
                         && uri.getPathSegments().size() > 3) {
                     String filterParam = uri.getLastPathSegment();
                     StringBuilder sb = new StringBuilder();
                     sb.append(Contacts._ID + " IN ");
                     appendContactFilterAsNestedQuery(sb, filterParam);
                     filterSql = sb.toString();
                 }
 
                 setTablesAndProjectionMapForContacts(qb, projection);
 
                 // Build the first query for starred
                 if (filterSql != null) {
                     qb.appendWhere(filterSql);
                 }
                 final String starredQuery = qb.buildQuery(projection, Contacts.STARRED + "=1",
                         null, Contacts._ID, null, null, null);
 
                 // Build the second query for frequent
                 qb = new SQLiteQueryBuilder();
                 setTablesAndProjectionMapForContacts(qb, projection);
                 if (filterSql != null) {
                     qb.appendWhere(filterSql);
                 }
                 final String frequentQuery = qb.buildQuery(projection,
                         Contacts.TIMES_CONTACTED + " > 0 AND (" + Contacts.STARRED
                         + " = 0 OR " + Contacts.STARRED + " IS NULL)",
                         null, Contacts._ID, null, null, null);
 
                 // Put them together
                 final String query = qb.buildUnionQuery(new String[] {starredQuery, frequentQuery},
                         STREQUENT_ORDER_BY, STREQUENT_LIMIT);
                 Cursor c = db.rawQuery(query, null);
                 if (c != null) {
                     c.setNotificationUri(getContext().getContentResolver(),
                             ContactsContract.AUTHORITY_URI);
                 }
                 return c;
             }
 
             case CONTACTS_GROUP: {
                 setTablesAndProjectionMapForContacts(qb, projection);
                 if (uri.getPathSegments().size() > 2) {
                     qb.appendWhere(sContactsInGroupSelect);
                     selectionArgs = insertSelectionArg(selectionArgs, uri.getLastPathSegment());
                 }
                 break;
             }
 
             case CONTACTS_DATA: {
                 long contactId = Long.parseLong(uri.getPathSegments().get(1));
 
                 qb.setTables(mOpenHelper.getDataView());
                 qb.setProjectionMap(sDataProjectionMap);
                 appendAccountFromParameter(qb, uri);
                 qb.appendWhere(" AND " + RawContacts.CONTACT_ID + "=" + contactId);
                 break;
             }
 
             case CONTACTS_PHOTO: {
                 long contactId = Long.parseLong(uri.getPathSegments().get(1));
 
                 qb.setTables(mOpenHelper.getDataView());
                 qb.setProjectionMap(sDataProjectionMap);
                 appendAccountFromParameter(qb, uri);
                 qb.appendWhere(" AND " + RawContacts.CONTACT_ID + "=" + contactId);
                 qb.appendWhere(" AND " + Data._ID + "=" + Contacts.PHOTO_ID);
                 break;
             }
 
             case PHONES: {
                 qb.setTables(mOpenHelper.getDataView());
                 qb.setProjectionMap(sDataProjectionMap);
                 qb.appendWhere(Data.MIMETYPE + " = '" + Phone.CONTENT_ITEM_TYPE + "'");
                 break;
             }
 
             case PHONES_FILTER: {
                 qb.setTables(mOpenHelper.getDataView());
                 qb.setProjectionMap(sDistinctDataProjectionMap);
                 qb.appendWhere(Data.MIMETYPE + " = '" + Phone.CONTENT_ITEM_TYPE + "'");
                 if (uri.getPathSegments().size() > 2) {
                     String filterParam = uri.getLastPathSegment();
                     StringBuilder sb = new StringBuilder();
                     sb.append("(");
 
                     boolean orNeeded = false;
                     String normalizedName = NameNormalizer.normalize(filterParam);
                     if (normalizedName.length() > 0) {
                         sb.append(Data.RAW_CONTACT_ID + " IN ");
                         appendRawContactsByNormalizedNameFilter(sb, normalizedName, null);
                         orNeeded = true;
                     }
 
                     if (isPhoneNumber(filterParam)) {
                         if (orNeeded) {
                             sb.append(" OR ");
                         }
                         String number = PhoneNumberUtils.convertKeypadLettersToDigits(filterParam);
                         String reversed = PhoneNumberUtils.getStrippedReversed(number);
                         sb.append(Data._ID +
                                 " IN (SELECT " + PhoneLookupColumns.DATA_ID
                                   + " FROM " + Tables.PHONE_LOOKUP
                                   + " WHERE " + PhoneLookupColumns.NORMALIZED_NUMBER + " LIKE '%");
                         sb.append(reversed);
                         sb.append("')");
                     }
                     sb.append(")");
                     qb.appendWhere(" AND " + sb);
                 }
                 groupBy = PhoneColumns.NORMALIZED_NUMBER + "," + RawContacts.CONTACT_ID;
                 break;
             }
 
             case EMAILS: {
                 qb.setTables(mOpenHelper.getDataView());
                 qb.setProjectionMap(sDataProjectionMap);
                 qb.appendWhere(Data.MIMETYPE + " = '" + Email.CONTENT_ITEM_TYPE + "'");
                 break;
             }
 
             case EMAILS_LOOKUP: {
                 qb.setTables(mOpenHelper.getDataView());
                 qb.setProjectionMap(sDataProjectionMap);
                 qb.appendWhere(Data.MIMETYPE + " = '" + Email.CONTENT_ITEM_TYPE + "'");
                 if (uri.getPathSegments().size() > 2) {
                     qb.appendWhere(" AND " + Email.DATA + "=");
                     qb.appendWhereEscapeString(uri.getLastPathSegment());
                 }
                 break;
             }
 
             case EMAILS_FILTER: {
                 qb.setTables(mOpenHelper.getDataView());
                 qb.setProjectionMap(sDistinctDataProjectionMap);
                 qb.appendWhere(Data.MIMETYPE + " = '" + Email.CONTENT_ITEM_TYPE + "'");
                 if (uri.getPathSegments().size() > 2) {
                     String filterParam = uri.getLastPathSegment();
                     StringBuilder sb = new StringBuilder();
                     sb.append("(");
 
                     String normalizedName = NameNormalizer.normalize(filterParam);
                     if (normalizedName.length() > 0) {
                         sb.append(Data.RAW_CONTACT_ID + " IN ");
                         appendRawContactsByNormalizedNameFilter(sb, normalizedName, null);
                         sb.append(" OR ");
                     }
 
                     sb.append(Email.DATA + " LIKE ");
                     sb.append(DatabaseUtils.sqlEscapeString(filterParam));
                     sb.append(")");
                     qb.appendWhere(" AND " + sb);
                 }
                 groupBy = Email.DATA + "," + RawContacts.CONTACT_ID;
                 break;
             }
 
             case POSTALS: {
                 qb.setTables(mOpenHelper.getDataView());
                 qb.setProjectionMap(sDataProjectionMap);
                 qb.appendWhere(Data.MIMETYPE + " = '" + StructuredPostal.CONTENT_ITEM_TYPE + "'");
                 break;
             }
 
             case RAW_CONTACTS: {
                 qb.setTables(mOpenHelper.getRawContactView());
                 qb.setProjectionMap(sRawContactsProjectionMap);
                 break;
             }
 
             case RAW_CONTACTS_ID: {
                 long rawContactId = ContentUris.parseId(uri);
                 qb.setTables(mOpenHelper.getRawContactView());
                 qb.setProjectionMap(sRawContactsProjectionMap);
                 qb.appendWhere(RawContacts._ID + "=" + rawContactId);
                 break;
             }
 
             case RAW_CONTACTS_DATA: {
                 long rawContactId = Long.parseLong(uri.getPathSegments().get(1));
                 qb.setTables(mOpenHelper.getDataView());
                 qb.setProjectionMap(sDataProjectionMap);
                 qb.appendWhere(Data.RAW_CONTACT_ID + "=" + rawContactId);
                 break;
             }
 
             case DATA: {
                 qb.setTables(mOpenHelper.getDataView());
                 qb.setProjectionMap(sDataProjectionMap);
                 appendAccountFromParameter(qb, uri);
                 break;
             }
 
             case DATA_ID: {
                 qb.setTables(mOpenHelper.getDataView());
                 qb.setProjectionMap(sDataProjectionMap);
                 qb.appendWhere(Data._ID + "=" + ContentUris.parseId(uri));
                 break;
             }
 
             case DATA_WITH_PRESENCE: {
                 qb.setTables(mOpenHelper.getDataView() + " data"
                         + " LEFT OUTER JOIN " + Tables.AGGREGATED_PRESENCE
                         + " ON (" + AggregatedPresenceColumns.CONTACT_ID + "="
                                 + RawContacts.CONTACT_ID + ")");
                 qb.setProjectionMap(sDataWithPresenceProjectionMap);
                 appendAccountFromParameter(qb, uri);
                 break;
             }
 
             case PHONE_LOOKUP: {
 
                 if (TextUtils.isEmpty(sortOrder)) {
                     // Default the sort order to something reasonable so we get consistent
                     // results when callers don't request an ordering
                     sortOrder = RawContactsColumns.CONCRETE_ID;
                 }
 
                 String number = uri.getPathSegments().size() > 1 ? uri.getLastPathSegment() : "";
                 mOpenHelper.buildPhoneLookupAndContactQuery(qb, number);
                 qb.setProjectionMap(sPhoneLookupProjectionMap);
 
                 // Phone lookup cannot be combined with a selection
                 selection = null;
                 selectionArgs = null;
                 break;
             }
 
             case GROUPS: {
                 qb.setTables(Tables.GROUPS_JOIN_PACKAGES);
                 qb.setProjectionMap(sGroupsProjectionMap);
                 break;
             }
 
             case GROUPS_ID: {
                 long groupId = ContentUris.parseId(uri);
                 qb.setTables(Tables.GROUPS_JOIN_PACKAGES);
                 qb.setProjectionMap(sGroupsProjectionMap);
                 qb.appendWhere(GroupsColumns.CONCRETE_ID + "=" + groupId);
                 break;
             }
 
             case GROUPS_SUMMARY: {
                 qb.setTables(Tables.GROUPS_JOIN_PACKAGES);
                 qb.setProjectionMap(sGroupsSummaryProjectionMap);
                 groupBy = GroupsColumns.CONCRETE_ID;
                 break;
             }
 
             case AGGREGATION_EXCEPTIONS: {
                 qb.setTables(Tables.AGGREGATION_EXCEPTIONS);
                 qb.setProjectionMap(sAggregationExceptionsProjectionMap);
                 break;
             }
 
             case AGGREGATION_SUGGESTIONS: {
                 long contactId = Long.parseLong(uri.getPathSegments().get(1));
                 final int maxSuggestions;
                 if (limit != null) {
                     maxSuggestions = Integer.parseInt(limit);
                 } else {
                     maxSuggestions = DEFAULT_MAX_SUGGESTIONS;
                 }
 
                 return mContactAggregator.queryAggregationSuggestions(contactId, projection,
                         sContactsProjectionMap, maxSuggestions);
             }
 
             case SETTINGS: {
                 qb.setTables(Tables.SETTINGS);
                 qb.setProjectionMap(sSettingsProjectionMap);
 
                 // When requesting specific columns, this query requires
                 // late-binding of the GroupMembership MIME-type.
                 final String groupMembershipMimetypeId = Long.toString(mOpenHelper
                         .getMimeTypeId(GroupMembership.CONTENT_ITEM_TYPE));
                 if (mOpenHelper.isInProjection(projection, Settings.UNGROUPED_COUNT)) {
                     selectionArgs = insertSelectionArg(selectionArgs, groupMembershipMimetypeId);
                 }
                 if (mOpenHelper.isInProjection(projection, Settings.UNGROUPED_WITH_PHONES)) {
                     selectionArgs = insertSelectionArg(selectionArgs, groupMembershipMimetypeId);
                 }
 
                 break;
             }
 
             case PRESENCE: {
                 qb.setTables(Tables.PRESENCE);
                 qb.setProjectionMap(sPresenceProjectionMap);
                 break;
             }
 
             case PRESENCE_ID: {
                 qb.setTables(Tables.PRESENCE);
                 qb.setProjectionMap(sPresenceProjectionMap);
                 qb.appendWhere(Presence._ID + "=" + ContentUris.parseId(uri));
                 break;
             }
 
             case SEARCH_SUGGESTIONS: {
                 return mGlobalSearchSupport.handleSearchSuggestionsQuery(db, uri, limit);
             }
 
             case SEARCH_SHORTCUT: {
                 long contactId = ContentUris.parseId(uri);
                 return mGlobalSearchSupport.handleSearchShortcutRefresh(db, contactId, projection);
             }
 
             case LIVE_FOLDERS_CONTACTS:
                 qb.setTables(mOpenHelper.getContactView());
                 qb.setProjectionMap(sLiveFoldersProjectionMap);
                 break;
 
             case LIVE_FOLDERS_CONTACTS_WITH_PHONES:
                 qb.setTables(mOpenHelper.getContactView());
                 qb.setProjectionMap(sLiveFoldersProjectionMap);
                 qb.appendWhere(Contacts.HAS_PHONE_NUMBER + "=1");
                 break;
 
             case LIVE_FOLDERS_CONTACTS_FAVORITES:
                 qb.setTables(mOpenHelper.getContactView());
                 qb.setProjectionMap(sLiveFoldersProjectionMap);
                 qb.appendWhere(Contacts.STARRED + "=1");
                 break;
 
             case LIVE_FOLDERS_CONTACTS_GROUP_NAME:
                 qb.setTables(mOpenHelper.getContactView());
                 qb.setProjectionMap(sLiveFoldersProjectionMap);
                 qb.appendWhere(sContactsInGroupSelect);
                 selectionArgs = insertSelectionArg(selectionArgs, uri.getLastPathSegment());
                 break;
 
             default:
                 return mLegacyApiSupport.query(uri, projection, selection, selectionArgs,
                         sortOrder, limit);
         }
 
         return query(db, qb, projection, selection, selectionArgs, sortOrder, groupBy, limit);
     }
 
     private Cursor query(final SQLiteDatabase db, SQLiteQueryBuilder qb, String[] projection,
             String selection, String[] selectionArgs, String sortOrder, String groupBy,
             String limit) {
         if (projection != null && projection.length == 1
                 && BaseColumns._COUNT.equals(projection[0])) {
             qb.setProjectionMap(sCountProjectionMap);
         }
         final Cursor c = qb.query(db, projection, selection, selectionArgs, groupBy, null,
                 sortOrder, limit);
         if (c != null) {
             c.setNotificationUri(getContext().getContentResolver(), ContactsContract.AUTHORITY_URI);
         }
         return c;
     }
 
     private long lookupContactIdByLookupKey(SQLiteDatabase db, String lookupKey) {
         ContactLookupKey key = new ContactLookupKey();
         ArrayList<LookupKeySegment> segments = key.parse(lookupKey);
 
         long contactId = lookupContactIdBySourceIds(db, segments);
         if (contactId == -1) {
             contactId = lookupContactIdByDisplayNames(db, segments);
         }
 
         return contactId;
     }
 
     private interface LookupBySourceIdQuery {
         String TABLE = Tables.RAW_CONTACTS;
 
         String COLUMNS[] = {
                 RawContacts.CONTACT_ID,
                 RawContacts.ACCOUNT_TYPE,
                 RawContacts.ACCOUNT_NAME,
                 RawContacts.SOURCE_ID
         };
 
         int CONTACT_ID = 0;
         int ACCOUNT_TYPE = 1;
         int ACCOUNT_NAME = 2;
         int SOURCE_ID = 3;
     }
 
     private long lookupContactIdBySourceIds(SQLiteDatabase db,
                 ArrayList<LookupKeySegment> segments) {
         int sourceIdCount = 0;
         for (int i = 0; i < segments.size(); i++) {
             LookupKeySegment segment = segments.get(i);
             if (segment.sourceIdLookup) {
                 sourceIdCount++;
             }
         }
 
         if (sourceIdCount == 0) {
             return -1;
         }
 
         // First try sync ids
         StringBuilder sb = new StringBuilder();
         sb.append(RawContacts.SOURCE_ID + " IN (");
         for (int i = 0; i < segments.size(); i++) {
             LookupKeySegment segment = segments.get(i);
             if (segment.sourceIdLookup) {
                 DatabaseUtils.appendEscapedSQLString(sb, segment.key);
                 sb.append(",");
             }
         }
         sb.setLength(sb.length() - 1);      // Last comma
         sb.append(") AND " + RawContacts.CONTACT_ID + " NOT NULL");
 
         Cursor c = db.query(LookupBySourceIdQuery.TABLE, LookupBySourceIdQuery.COLUMNS,
                  sb.toString(), null, null, null, null);
         try {
             while (c.moveToNext()) {
                 String accountType = c.getString(LookupBySourceIdQuery.ACCOUNT_TYPE);
                 String accountName = c.getString(LookupBySourceIdQuery.ACCOUNT_NAME);
                 int accountHashCode =
                         ContactLookupKey.getAccountHashCode(accountType, accountName);
                 String sourceId = c.getString(LookupBySourceIdQuery.SOURCE_ID);
                 for (int i = 0; i < segments.size(); i++) {
                     LookupKeySegment segment = segments.get(i);
                     if (segment.sourceIdLookup && accountHashCode == segment.accountHashCode
                             && segment.key.equals(sourceId)) {
                         segment.contactId = c.getLong(LookupBySourceIdQuery.CONTACT_ID);
                         break;
                     }
                 }
             }
         } finally {
             c.close();
         }
 
         return getMostReferencedContactId(segments);
     }
 
     private interface LookupByDisplayNameQuery {
         String TABLE = Tables.NAME_LOOKUP_JOIN_RAW_CONTACTS;
 
         String COLUMNS[] = {
                 RawContacts.CONTACT_ID,
                 RawContacts.ACCOUNT_TYPE,
                 RawContacts.ACCOUNT_NAME,
                 NameLookupColumns.NORMALIZED_NAME
         };
 
         int CONTACT_ID = 0;
         int ACCOUNT_TYPE = 1;
         int ACCOUNT_NAME = 2;
         int NORMALIZED_NAME = 3;
     }
 
     private long lookupContactIdByDisplayNames(SQLiteDatabase db,
                 ArrayList<LookupKeySegment> segments) {
         int displayNameCount = 0;
         for (int i = 0; i < segments.size(); i++) {
             LookupKeySegment segment = segments.get(i);
             if (!segment.sourceIdLookup) {
                 displayNameCount++;
             }
         }
 
         if (displayNameCount == 0) {
             return -1;
         }
 
         // First try sync ids
         StringBuilder sb = new StringBuilder();
         sb.append(NameLookupColumns.NORMALIZED_NAME + " IN (");
         for (int i = 0; i < segments.size(); i++) {
             LookupKeySegment segment = segments.get(i);
             if (!segment.sourceIdLookup) {
                 DatabaseUtils.appendEscapedSQLString(sb, segment.key);
                 sb.append(",");
             }
         }
         sb.setLength(sb.length() - 1);      // Last comma
         sb.append(") AND " + NameLookupColumns.NAME_TYPE + "=" + NameLookupType.NAME_COLLATION_KEY
                 + " AND " + RawContacts.CONTACT_ID + " NOT NULL");
 
         Cursor c = db.query(LookupByDisplayNameQuery.TABLE, LookupByDisplayNameQuery.COLUMNS,
                  sb.toString(), null, null, null, null);
         try {
             while (c.moveToNext()) {
                 String accountType = c.getString(LookupByDisplayNameQuery.ACCOUNT_TYPE);
                 String accountName = c.getString(LookupByDisplayNameQuery.ACCOUNT_NAME);
                 int accountHashCode =
                         ContactLookupKey.getAccountHashCode(accountType, accountName);
                 String name = c.getString(LookupByDisplayNameQuery.NORMALIZED_NAME);
                 for (int i = 0; i < segments.size(); i++) {
                     LookupKeySegment segment = segments.get(i);
                     if (!segment.sourceIdLookup && accountHashCode == segment.accountHashCode
                             && segment.key.equals(name)) {
                         segment.contactId = c.getLong(LookupByDisplayNameQuery.CONTACT_ID);
                         break;
                     }
                 }
             }
         } finally {
             c.close();
         }
 
         return getMostReferencedContactId(segments);
     }
 
     /**
      * Returns the contact ID that is mentioned the highest number of times.
      */
     private long getMostReferencedContactId(ArrayList<LookupKeySegment> segments) {
         Collections.sort(segments);
 
         long bestContactId = -1;
         int bestRefCount = 0;
 
         long contactId = -1;
         int count = 0;
 
         int segmentCount = segments.size();
         for (int i = 0; i < segmentCount; i++) {
             LookupKeySegment segment = segments.get(i);
             if (segment.contactId != -1) {
                 if (segment.contactId == contactId) {
                     count++;
                 } else {
                     if (count > bestRefCount) {
                         bestContactId = contactId;
                         bestRefCount = count;
                     }
                     contactId = segment.contactId;
                     count = 1;
                 }
             }
         }
         if (count > bestRefCount) {
             return contactId;
         } else {
             return bestContactId;
         }
     }
 
     private void setTablesAndProjectionMapForContacts(SQLiteQueryBuilder qb, String[] projection) {
         String contactView = mOpenHelper.getContactView();
         boolean needsPresence = mOpenHelper.isInProjection(projection, Contacts.PRESENCE_STATUS,
                 Contacts.PRESENCE_CUSTOM_STATUS);
         if (!needsPresence) {
             qb.setTables(contactView);
             qb.setProjectionMap(sContactsProjectionMap);
         } else {
             qb.setTables(contactView + " LEFT OUTER JOIN " + Tables.AGGREGATED_PRESENCE + " ON ("
                     + Contacts._ID + " = " + AggregatedPresenceColumns.CONTACT_ID + ") ");
             qb.setProjectionMap(sContactsWithPresenceProjectionMap);
 
         }
     }
 
     private void appendAccountFromParameter(SQLiteQueryBuilder qb, Uri uri) {
         final String accountName = uri.getQueryParameter(RawContacts.ACCOUNT_NAME);
         final String accountType = uri.getQueryParameter(RawContacts.ACCOUNT_TYPE);
         if (!TextUtils.isEmpty(accountName)) {
             qb.appendWhere(RawContacts.ACCOUNT_NAME + "="
                     + DatabaseUtils.sqlEscapeString(accountName) + " AND "
                     + RawContacts.ACCOUNT_TYPE + "="
                     + DatabaseUtils.sqlEscapeString(accountType));
         } else {
             qb.appendWhere("1");
         }
     }
 
     private String appendAccountToSelection(Uri uri, String selection) {
         final String accountName = uri.getQueryParameter(RawContacts.ACCOUNT_NAME);
         final String accountType = uri.getQueryParameter(RawContacts.ACCOUNT_TYPE);
         if (!TextUtils.isEmpty(accountName)) {
             StringBuilder selectionSb = new StringBuilder(RawContacts.ACCOUNT_NAME + "="
                     + DatabaseUtils.sqlEscapeString(accountName) + " AND "
                     + RawContacts.ACCOUNT_TYPE + "="
                     + DatabaseUtils.sqlEscapeString(accountType));
             if (!TextUtils.isEmpty(selection)) {
                 selectionSb.append(" AND (");
                 selectionSb.append(selection);
                 selectionSb.append(')');
             }
             return selectionSb.toString();
         } else {
             return selection;
         }
     }
 
     /**
      * Gets the value of the "limit" URI query parameter.
      *
      * @return A string containing a non-negative integer, or <code>null</code> if
      *         the parameter is not set, or is set to an invalid value.
      */
     private String getLimit(Uri url) {
         String limitParam = url.getQueryParameter("limit");
         if (limitParam == null) {
             return null;
         }
         // make sure that the limit is a non-negative integer
         try {
             int l = Integer.parseInt(limitParam);
             if (l < 0) {
                 Log.w(TAG, "Invalid limit parameter: " + limitParam);
                 return null;
             }
             return String.valueOf(l);
         } catch (NumberFormatException ex) {
             Log.w(TAG, "Invalid limit parameter: " + limitParam);
             return null;
         }
     }
 
     /**
      * Returns true if all the characters are meaningful as digits
      * in a phone number -- letters, digits, and a few punctuation marks.
      */
     private boolean isPhoneNumber(CharSequence cons) {
         int len = cons.length();
 
         for (int i = 0; i < len; i++) {
             char c = cons.charAt(i);
 
             if ((c >= '0') && (c <= '9')) {
                 continue;
             }
             if ((c == ' ') || (c == '-') || (c == '(') || (c == ')') || (c == '.') || (c == '+')
                     || (c == '#') || (c == '*')) {
                 continue;
             }
             if ((c >= 'A') && (c <= 'Z')) {
                 continue;
             }
             if ((c >= 'a') && (c <= 'z')) {
                 continue;
             }
 
             return false;
         }
 
         return true;
     }
 
     String getContactsRestrictions() {
         if (mOpenHelper.hasRestrictedAccess()) {
             return "1";
         } else {
             return RawContacts.IS_RESTRICTED + "=0";
         }
     }
 
     public String getContactsRestrictionExceptionAsNestedQuery(String contactIdColumn) {
         if (mOpenHelper.hasRestrictedAccess()) {
             return "1";
         } else {
             return "(SELECT " + RawContacts.IS_RESTRICTED + " FROM " + Tables.RAW_CONTACTS
                     + " WHERE " + RawContactsColumns.CONCRETE_ID + "=" + contactIdColumn + ")=0";
         }
     }
 
     @Override
     public AssetFileDescriptor openAssetFile(Uri uri, String mode) throws FileNotFoundException {
         int match = sUriMatcher.match(uri);
         switch (match) {
             case CONTACTS_PHOTO:
                 if (!"r".equals(mode)) {
                     throw new FileNotFoundException("Mode " + mode + " not supported.");
                 }
 
                 long contactId = Long.parseLong(uri.getPathSegments().get(1));
 
                 String sql =
                         "SELECT " + Photo.PHOTO + " FROM " + mOpenHelper.getDataView() +
                         " WHERE " + Data._ID + "=" + Contacts.PHOTO_ID
                                 + " AND " + RawContacts.CONTACT_ID + "=" + contactId;
                 SQLiteDatabase db = mOpenHelper.getReadableDatabase();
                 return SQLiteContentHelper.getBlobColumnAsAssetFile(db, sql, null);
 
             default:
                 throw new FileNotFoundException("No file at: " + uri);
         }
     }
 
 
 
     /**
      * An implementation of EntityIterator that joins the contacts and data tables
      * and consumes all the data rows for a contact in order to build the Entity for a contact.
      */
     private static class ContactsEntityIterator implements EntityIterator {
         private final Cursor mEntityCursor;
         private volatile boolean mIsClosed;
 
         private static final String[] DATA_KEYS = new String[]{
                 Data.DATA1,
                 Data.DATA2,
                 Data.DATA3,
                 Data.DATA4,
                 Data.DATA5,
                 Data.DATA6,
                 Data.DATA7,
                 Data.DATA8,
                 Data.DATA9,
                 Data.DATA10,
                 Data.DATA11,
                 Data.DATA12,
                 Data.DATA13,
                 Data.DATA14,
                 Data.DATA15,
                 Data.SYNC1,
                 Data.SYNC2,
                 Data.SYNC3,
                 Data.SYNC4};
 
         private static final String[] PROJECTION = new String[]{
                 RawContacts.ACCOUNT_NAME,
                 RawContacts.ACCOUNT_TYPE,
                 RawContacts.SOURCE_ID,
                 RawContacts.VERSION,
                 RawContacts.DIRTY,
                 Data._ID,
                 Data.RES_PACKAGE,
                 Data.MIMETYPE,
                 Data.DATA1,
                 Data.DATA2,
                 Data.DATA3,
                 Data.DATA4,
                 Data.DATA5,
                 Data.DATA6,
                 Data.DATA7,
                 Data.DATA8,
                 Data.DATA9,
                 Data.DATA10,
                 Data.DATA11,
                 Data.DATA12,
                 Data.DATA13,
                 Data.DATA14,
                 Data.DATA15,
                 Data.SYNC1,
                 Data.SYNC2,
                 Data.SYNC3,
                 Data.SYNC4,
                 Data.RAW_CONTACT_ID,
                 Data.IS_PRIMARY,
                 Data.IS_SUPER_PRIMARY,
                 Data.DATA_VERSION,
                 GroupMembership.GROUP_SOURCE_ID,
                 RawContacts.SYNC1,
                 RawContacts.SYNC2,
                 RawContacts.SYNC3,
                 RawContacts.SYNC4,
                 RawContacts.DELETED,
                 RawContacts.CONTACT_ID,
                 RawContacts.STARRED};
 
         private static final int COLUMN_ACCOUNT_NAME = 0;
         private static final int COLUMN_ACCOUNT_TYPE = 1;
         private static final int COLUMN_SOURCE_ID = 2;
         private static final int COLUMN_VERSION = 3;
         private static final int COLUMN_DIRTY = 4;
         private static final int COLUMN_DATA_ID = 5;
         private static final int COLUMN_RES_PACKAGE = 6;
         private static final int COLUMN_MIMETYPE = 7;
         private static final int COLUMN_DATA1 = 8;
         private static final int COLUMN_RAW_CONTACT_ID = 27;
         private static final int COLUMN_IS_PRIMARY = 28;
         private static final int COLUMN_IS_SUPER_PRIMARY = 29;
         private static final int COLUMN_DATA_VERSION = 30;
         private static final int COLUMN_GROUP_SOURCE_ID = 31;
         private static final int COLUMN_SYNC1 = 32;
         private static final int COLUMN_SYNC2 = 33;
         private static final int COLUMN_SYNC3 = 34;
         private static final int COLUMN_SYNC4 = 35;
         private static final int COLUMN_DELETED = 36;
         private static final int COLUMN_CONTACT_ID = 37;
         private static final int COLUMN_STARRED = 38;
 
         public ContactsEntityIterator(ContactsProvider2 provider, String contactsIdString, Uri uri,
                 String selection, String[] selectionArgs, String sortOrder) {
             mIsClosed = false;
 
             final String updatedSortOrder = (sortOrder == null)
                     ? Data.RAW_CONTACT_ID
                     : (Data.RAW_CONTACT_ID + "," + sortOrder);
 
             final SQLiteDatabase db = provider.mOpenHelper.getReadableDatabase();
             final SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
             qb.setTables(Tables.CONTACT_ENTITIES);
             if (contactsIdString != null) {
                 qb.appendWhere(Data.RAW_CONTACT_ID + "=" + contactsIdString);
             }
             final String accountName = uri.getQueryParameter(RawContacts.ACCOUNT_NAME);
             final String accountType = uri.getQueryParameter(RawContacts.ACCOUNT_TYPE);
             if (!TextUtils.isEmpty(accountName)) {
                 qb.appendWhere(RawContacts.ACCOUNT_NAME + "="
                         + DatabaseUtils.sqlEscapeString(accountName) + " AND "
                         + RawContacts.ACCOUNT_TYPE + "="
                         + DatabaseUtils.sqlEscapeString(accountType));
             }
             mEntityCursor = qb.query(db, PROJECTION, selection, selectionArgs,
                     null, null, updatedSortOrder);
             mEntityCursor.moveToFirst();
         }
 
         public void reset() throws RemoteException {
             if (mIsClosed) {
                 throw new IllegalStateException("calling reset() when the iterator is closed");
             }
             mEntityCursor.moveToFirst();
         }
 
         public void close() {
             if (mIsClosed) {
                 throw new IllegalStateException("closing when already closed");
             }
             mIsClosed = true;
             mEntityCursor.close();
         }
 
         public boolean hasNext() throws RemoteException {
             if (mIsClosed) {
                 throw new IllegalStateException("calling hasNext() when the iterator is closed");
             }
 
             return !mEntityCursor.isAfterLast();
         }
 
         public Entity next() throws RemoteException {
             if (mIsClosed) {
                 throw new IllegalStateException("calling next() when the iterator is closed");
             }
             if (!hasNext()) {
                 throw new IllegalStateException("you may only call next() if hasNext() is true");
             }
 
             final SQLiteCursor c = (SQLiteCursor) mEntityCursor;
 
             final long rawContactId = c.getLong(COLUMN_RAW_CONTACT_ID);
 
             // we expect the cursor is already at the row we need to read from
             ContentValues contactValues = new ContentValues();
             contactValues.put(RawContacts.ACCOUNT_NAME, c.getString(COLUMN_ACCOUNT_NAME));
             contactValues.put(RawContacts.ACCOUNT_TYPE, c.getString(COLUMN_ACCOUNT_TYPE));
             contactValues.put(RawContacts._ID, rawContactId);
             contactValues.put(RawContacts.DIRTY, c.getLong(COLUMN_DIRTY));
             contactValues.put(RawContacts.VERSION, c.getLong(COLUMN_VERSION));
             contactValues.put(RawContacts.SOURCE_ID, c.getString(COLUMN_SOURCE_ID));
             contactValues.put(RawContacts.SYNC1, c.getString(COLUMN_SYNC1));
             contactValues.put(RawContacts.SYNC2, c.getString(COLUMN_SYNC2));
             contactValues.put(RawContacts.SYNC3, c.getString(COLUMN_SYNC3));
             contactValues.put(RawContacts.SYNC4, c.getString(COLUMN_SYNC4));
             contactValues.put(RawContacts.DELETED, c.getLong(COLUMN_DELETED));
             contactValues.put(RawContacts.CONTACT_ID, c.getLong(COLUMN_CONTACT_ID));
             contactValues.put(RawContacts.STARRED, c.getLong(COLUMN_STARRED));
             Entity contact = new Entity(contactValues);
 
             // read data rows until the contact id changes
             do {
                 if (rawContactId != c.getLong(COLUMN_RAW_CONTACT_ID)) {
                     break;
                 }
                 // add the data to to the contact
                 ContentValues dataValues = new ContentValues();
                 dataValues.put(Data._ID, c.getString(COLUMN_DATA_ID));
                 dataValues.put(Data.RES_PACKAGE, c.getString(COLUMN_RES_PACKAGE));
                 dataValues.put(Data.MIMETYPE, c.getString(COLUMN_MIMETYPE));
                 dataValues.put(Data.IS_PRIMARY, c.getString(COLUMN_IS_PRIMARY));
                 dataValues.put(Data.IS_SUPER_PRIMARY, c.getString(COLUMN_IS_SUPER_PRIMARY));
                 dataValues.put(Data.DATA_VERSION, c.getLong(COLUMN_DATA_VERSION));
                 if (!c.isNull(COLUMN_GROUP_SOURCE_ID)) {
                     dataValues.put(GroupMembership.GROUP_SOURCE_ID,
                             c.getString(COLUMN_GROUP_SOURCE_ID));
                 }
                 dataValues.put(Data.DATA_VERSION, c.getLong(COLUMN_DATA_VERSION));
                 for (int i = 0; i < DATA_KEYS.length; i++) {
                     final int columnIndex = i + COLUMN_DATA1;
                     String key = DATA_KEYS[i];
                     if (c.isNull(columnIndex)) {
                         // don't put anything
                     } else if (c.isLong(columnIndex)) {
                         dataValues.put(key, c.getLong(columnIndex));
                     } else if (c.isFloat(columnIndex)) {
                         dataValues.put(key, c.getFloat(columnIndex));
                     } else if (c.isString(columnIndex)) {
                         dataValues.put(key, c.getString(columnIndex));
                     } else if (c.isBlob(columnIndex)) {
                         dataValues.put(key, c.getBlob(columnIndex));
                     }
                 }
                 contact.addSubValue(Data.CONTENT_URI, dataValues);
             } while (mEntityCursor.moveToNext());
 
             return contact;
         }
     }
 
     /**
      * An implementation of EntityIterator that joins the contacts and data tables
      * and consumes all the data rows for a contact in order to build the Entity for a contact.
      */
     private static class GroupsEntityIterator implements EntityIterator {
         private final Cursor mEntityCursor;
         private volatile boolean mIsClosed;
 
         private static final String[] PROJECTION = new String[]{
                 Groups._ID,
                 Groups.ACCOUNT_NAME,
                 Groups.ACCOUNT_TYPE,
                 Groups.SOURCE_ID,
                 Groups.DIRTY,
                 Groups.VERSION,
                 Groups.RES_PACKAGE,
                 Groups.TITLE,
                 Groups.TITLE_RES,
                 Groups.GROUP_VISIBLE,
                 Groups.SYNC1,
                 Groups.SYNC2,
                 Groups.SYNC3,
                 Groups.SYNC4,
                 Groups.SYSTEM_ID,
                 Groups.NOTES,
                 Groups.DELETED};
 
         private static final int COLUMN_ID = 0;
         private static final int COLUMN_ACCOUNT_NAME = 1;
         private static final int COLUMN_ACCOUNT_TYPE = 2;
         private static final int COLUMN_SOURCE_ID = 3;
         private static final int COLUMN_DIRTY = 4;
         private static final int COLUMN_VERSION = 5;
         private static final int COLUMN_RES_PACKAGE = 6;
         private static final int COLUMN_TITLE = 7;
         private static final int COLUMN_TITLE_RES = 8;
         private static final int COLUMN_GROUP_VISIBLE = 9;
         private static final int COLUMN_SYNC1 = 10;
         private static final int COLUMN_SYNC2 = 11;
         private static final int COLUMN_SYNC3 = 12;
         private static final int COLUMN_SYNC4 = 13;
         private static final int COLUMN_SYSTEM_ID = 14;
         private static final int COLUMN_NOTES = 15;
         private static final int COLUMN_DELETED = 16;
 
         public GroupsEntityIterator(ContactsProvider2 provider, String groupIdString, Uri uri,
                 String selection, String[] selectionArgs, String sortOrder) {
             mIsClosed = false;
 
             final String updatedSortOrder = (sortOrder == null)
                     ? Groups._ID
                     : (Groups._ID + "," + sortOrder);
 
             final SQLiteDatabase db = provider.mOpenHelper.getReadableDatabase();
             final SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
             qb.setTables(Tables.GROUPS_JOIN_PACKAGES);
             qb.setProjectionMap(sGroupsProjectionMap);
             if (groupIdString != null) {
                 qb.appendWhere(Groups._ID + "=" + groupIdString);
             }
             final String accountName = uri.getQueryParameter(Groups.ACCOUNT_NAME);
             final String accountType = uri.getQueryParameter(Groups.ACCOUNT_TYPE);
             if (!TextUtils.isEmpty(accountName)) {
                 qb.appendWhere(Groups.ACCOUNT_NAME + "="
                         + DatabaseUtils.sqlEscapeString(accountName) + " AND "
                         + Groups.ACCOUNT_TYPE + "="
                         + DatabaseUtils.sqlEscapeString(accountType));
             }
             mEntityCursor = qb.query(db, PROJECTION, selection, selectionArgs,
                     null, null, updatedSortOrder);
             mEntityCursor.moveToFirst();
         }
 
         public void close() {
             if (mIsClosed) {
                 throw new IllegalStateException("closing when already closed");
             }
             mIsClosed = true;
             mEntityCursor.close();
         }
 
         public boolean hasNext() throws RemoteException {
             if (mIsClosed) {
                 throw new IllegalStateException("calling hasNext() when the iterator is closed");
             }
 
             return !mEntityCursor.isAfterLast();
         }
 
         public void reset() throws RemoteException {
             if (mIsClosed) {
                 throw new IllegalStateException("calling reset() when the iterator is closed");
             }
             mEntityCursor.moveToFirst();
         }
 
         public Entity next() throws RemoteException {
             if (mIsClosed) {
                 throw new IllegalStateException("calling next() when the iterator is closed");
             }
             if (!hasNext()) {
                 throw new IllegalStateException("you may only call next() if hasNext() is true");
             }
 
             final SQLiteCursor c = (SQLiteCursor) mEntityCursor;
 
             final long groupId = c.getLong(COLUMN_ID);
 
             // we expect the cursor is already at the row we need to read from
             ContentValues groupValues = new ContentValues();
             groupValues.put(Groups.ACCOUNT_NAME, c.getString(COLUMN_ACCOUNT_NAME));
             groupValues.put(Groups.ACCOUNT_TYPE, c.getString(COLUMN_ACCOUNT_TYPE));
             groupValues.put(Groups._ID, groupId);
             groupValues.put(Groups.DIRTY, c.getLong(COLUMN_DIRTY));
             groupValues.put(Groups.VERSION, c.getLong(COLUMN_VERSION));
             groupValues.put(Groups.SOURCE_ID, c.getString(COLUMN_SOURCE_ID));
             groupValues.put(Groups.RES_PACKAGE, c.getString(COLUMN_RES_PACKAGE));
             groupValues.put(Groups.TITLE, c.getString(COLUMN_TITLE));
             groupValues.put(Groups.TITLE_RES, c.getString(COLUMN_TITLE_RES));
             groupValues.put(Groups.GROUP_VISIBLE, c.getLong(COLUMN_GROUP_VISIBLE));
             groupValues.put(Groups.SYNC1, c.getString(COLUMN_SYNC1));
             groupValues.put(Groups.SYNC2, c.getString(COLUMN_SYNC2));
             groupValues.put(Groups.SYNC3, c.getString(COLUMN_SYNC3));
             groupValues.put(Groups.SYNC4, c.getString(COLUMN_SYNC4));
             groupValues.put(Groups.SYSTEM_ID, c.getString(COLUMN_SYSTEM_ID));
             groupValues.put(Groups.DELETED, c.getLong(COLUMN_DELETED));
             groupValues.put(Groups.NOTES, c.getString(COLUMN_NOTES));
             Entity group = new Entity(groupValues);
 
             mEntityCursor.moveToNext();
 
             return group;
         }
     }
 
     @Override
     public EntityIterator queryEntities(Uri uri, String selection, String[] selectionArgs,
             String sortOrder) {
         waitForAccess();
 
         final int match = sUriMatcher.match(uri);
         switch (match) {
             case RAW_CONTACTS:
             case RAW_CONTACTS_ID:
                 String contactsIdString = null;
                 if (match == RAW_CONTACTS_ID) {
                     contactsIdString = uri.getPathSegments().get(1);
                 }
 
                 return new ContactsEntityIterator(this, contactsIdString,
                         uri, selection, selectionArgs, sortOrder);
             case GROUPS:
             case GROUPS_ID:
                 String idString = null;
                 if (match == GROUPS_ID) {
                     idString = uri.getPathSegments().get(1);
                 }
 
                 return new GroupsEntityIterator(this, idString,
                         uri, selection, selectionArgs, sortOrder);
             default:
                 throw new UnsupportedOperationException("Unknown uri: " + uri);
         }
     }
 
     @Override
     public String getType(Uri uri) {
         final int match = sUriMatcher.match(uri);
         switch (match) {
             case CONTACTS:
             case CONTACTS_LOOKUP:
                 return Contacts.CONTENT_TYPE;
             case CONTACTS_ID:
             case CONTACTS_LOOKUP_ID:
                 return Contacts.CONTENT_ITEM_TYPE;
             case RAW_CONTACTS:
                 return RawContacts.CONTENT_TYPE;
             case RAW_CONTACTS_ID:
                 return RawContacts.CONTENT_ITEM_TYPE;
             case DATA_ID:
                 return mOpenHelper.getDataMimeType(ContentUris.parseId(uri));
             case AGGREGATION_EXCEPTIONS:
                 return AggregationExceptions.CONTENT_TYPE;
             case AGGREGATION_EXCEPTION_ID:
                 return AggregationExceptions.CONTENT_ITEM_TYPE;
             case SETTINGS:
                 return Settings.CONTENT_TYPE;
             case AGGREGATION_SUGGESTIONS:
                 return Contacts.CONTENT_TYPE;
             case SEARCH_SUGGESTIONS:
                 return SearchManager.SUGGEST_MIME_TYPE;
             case SEARCH_SHORTCUT:
                 return SearchManager.SHORTCUT_MIME_TYPE;
             default:
                 return mLegacyApiSupport.getType(uri);
         }
     }
 
     private void setDisplayName(long rawContactId, String displayName, int bestDisplayNameSource) {
         if (displayName != null) {
             mRawContactDisplayNameUpdate.bindString(1, displayName);
         } else {
             mRawContactDisplayNameUpdate.bindNull(1);
         }
         mRawContactDisplayNameUpdate.bindLong(2, bestDisplayNameSource);
         mRawContactDisplayNameUpdate.bindLong(3, rawContactId);
         mRawContactDisplayNameUpdate.execute();
     }
 
     /**
      * Checks the {@link Data#MARK_AS_DIRTY} query parameter.
      *
      * Returns true if the parameter is missing or is either "true" or "1".
      */
     private boolean shouldMarkRawContactAsDirty(Uri uri) {
         if (mImportMode) {
             return false;
         }
 
         String param = uri.getQueryParameter(Data.MARK_AS_DIRTY);
         return param == null || (!param.equalsIgnoreCase("false") && !param.equals("0"));
     }
 
     /**
      * Sets the {@link RawContacts#DIRTY} for the specified raw contact.
      */
     private void setRawContactDirty(long rawContactId) {
         mRawContactDirtyUpdate.bindLong(1, rawContactId);
         mRawContactDirtyUpdate.execute();
     }
 
     /**
      * Checks the {@link Groups#MARK_AS_DIRTY} query parameter.
      *
      * Returns true if the parameter is missing or is either "true" or "1".
      */
     private boolean shouldMarkGroupAsDirty(Uri uri) {
         if (mImportMode) {
             return false;
         }
 
         return readBooleanQueryParameter(uri, Groups.MARK_AS_DIRTY, true);
     }
 
     /*
      * Sets the given dataId record in the "data" table to primary, and resets all data records of
      * the same mimetype and under the same contact to not be primary.
      *
      * @param dataId the id of the data record to be set to primary.
      */
     private void setIsPrimary(long rawContactId, long dataId, long mimeTypeId) {
         mSetPrimaryStatement.bindLong(1, dataId);
         mSetPrimaryStatement.bindLong(2, mimeTypeId);
         mSetPrimaryStatement.bindLong(3, rawContactId);
         mSetPrimaryStatement.execute();
     }
 
     /*
      * Sets the given dataId record in the "data" table to "super primary", and resets all data
      * records of the same mimetype and under the same aggregate to not be "super primary".
      *
      * @param dataId the id of the data record to be set to primary.
      */
     private void setIsSuperPrimary(long rawContactId, long dataId, long mimeTypeId) {
         mSetSuperPrimaryStatement.bindLong(1, dataId);
         mSetSuperPrimaryStatement.bindLong(2, mimeTypeId);
         mSetSuperPrimaryStatement.bindLong(3, rawContactId);
         mSetSuperPrimaryStatement.execute();
     }
 
     private void appendContactFilterAsNestedQuery(StringBuilder sb, String filterParam) {
         sb.append("(SELECT DISTINCT " + RawContacts.CONTACT_ID + " FROM " + Tables.RAW_CONTACTS
                 + " JOIN name_lookup ON(" + RawContactsColumns.CONCRETE_ID + "=raw_contact_id)"
                 + " WHERE normalized_name GLOB '");
         sb.append(NameNormalizer.normalize(filterParam));
         sb.append("*')");
     }
 
     public String getRawContactsByFilterAsNestedQuery(String filterParam) {
         StringBuilder sb = new StringBuilder();
         appendRawContactsByFilterAsNestedQuery(sb, filterParam, null);
         return sb.toString();
     }
 
     public void appendRawContactsByFilterAsNestedQuery(StringBuilder sb, String filterParam,
             String limit) {
         appendRawContactsByNormalizedNameFilter(sb, NameNormalizer.normalize(filterParam), limit);
     }
 
     private void appendRawContactsByNormalizedNameFilter(StringBuilder sb, String normalizedName,
             String limit) {
         sb.append("(SELECT DISTINCT raw_contact_id FROM name_lookup WHERE normalized_name GLOB '");
         sb.append(normalizedName);
         sb.append("*'");
         if (limit != null) {
             sb.append(" LIMIT ").append(limit);
         }
         sb.append(")");
     }
 
     /**
      * Inserts an argument at the beginning of the selection arg list.
      */
     private String[] insertSelectionArg(String[] selectionArgs, String arg) {
         if (selectionArgs == null) {
             return new String[] {arg};
         } else {
             int newLength = selectionArgs.length + 1;
             String[] newSelectionArgs = new String[newLength];
             newSelectionArgs[0] = arg;
             System.arraycopy(selectionArgs, 0, newSelectionArgs, 1, selectionArgs.length);
             return newSelectionArgs;
         }
     }
 
     protected Account getDefaultAccount() {
         AccountManager accountManager = AccountManager.get(getContext());
         try {
             Account[] accounts = accountManager.getAccountsByTypeAndFeatures(DEFAULT_ACCOUNT_TYPE,
                     new String[] {FEATURE_LEGACY_HOSTED_OR_GOOGLE}, null, null).getResult();
             if (accounts != null && accounts.length > 0) {
                 return accounts[0];
             }
         } catch (Throwable e) {
             Log.e(TAG, "Cannot determine the default account for contacts compatibility", e);
         }
         return null;
     }
 }
