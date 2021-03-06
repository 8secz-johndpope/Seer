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
 
 package com.android.providers.contacts;
 
 import android.accounts.Account;
 import android.content.Context;
 import android.database.sqlite.SQLiteDatabase;
 
 /**
  * A version of {@link ContactsProvider2} class that performs aggregation
  * synchronously and wipes all data at construction time.
  */
 public class SynchronousContactsProvider2 extends ContactsProvider2 {
     private static Boolean sDataWiped = false;
     private static OpenHelper mOpenHelper;
     private boolean mDataWipeEnabled = true;
     private Account mAccount;
 
     public SynchronousContactsProvider2() {
         this(new SynchronousAggregationScheduler());
     }
 
     public SynchronousContactsProvider2(ContactAggregationScheduler scheduler) {
         super(scheduler);
     }
 
     @Override
     protected OpenHelper getOpenHelper(final Context context) {
         if (mOpenHelper == null) {
             mOpenHelper = new OpenHelper(context);
         }
         return mOpenHelper;
     }
 
     public static void resetOpenHelper() {
         mOpenHelper = null;
     }
 
     public void setDataWipeEnabled(boolean flag) {
         mDataWipeEnabled = flag;
     }
 
     @Override
     public boolean onCreate() {
         boolean created = super.onCreate();
         if (mDataWipeEnabled) {
             synchronized (sDataWiped) {
                 if (!sDataWiped) {
                     sDataWiped = true;
                     wipeData();
                 }
             }
         }
         return created;
     }
 
     @Override
     protected Account getDefaultAccount() {
         if (mAccount == null) {
             mAccount = new Account("androidtest@gmail.com", "com.google.GAIA");
         }
         return mAccount;
     }
 
     public void prepareForFullAggregation(int maxContact) {
         SQLiteDatabase db = getOpenHelper().getWritableDatabase();
        db.execSQL("UPDATE raw_contacts SET contact_id = NULL, aggregation_mode=0;");
         db.execSQL("DELETE FROM contacts;");
         db.execSQL("DELETE FROM name_lookup;");
         long rowId =
             db.compileStatement("SELECT _id FROM raw_contacts LIMIT 1 OFFSET " + maxContact)
                 .simpleQueryForLong();
         db.execSQL("DELETE FROM raw_contacts WHERE _id > " + rowId + ";");
     }
 
     public long getRawContactCount() {
         SQLiteDatabase db = getOpenHelper().getReadableDatabase();
         return db.compileStatement("SELECT COUNT(*) FROM raw_contacts").simpleQueryForLong();
     }
 
     public long getContactCount() {
         SQLiteDatabase db = getOpenHelper().getReadableDatabase();
         return db.compileStatement("SELECT COUNT(*) FROM contacts").simpleQueryForLong();
     }
 
     @Override
     protected boolean isLegacyContactImportNeeded() {
 
         // We have an explicit test for data conversion - no need to do it every time
         return false;
     }
 
     private static class SynchronousAggregationScheduler extends ContactAggregationScheduler {
 
         @Override
         public void start() {
         }
 
         @Override
         public void stop() {
         }
 
         @Override
         long currentTime() {
             return 0;
         }
 
         @Override
         void runDelayed() {
             super.run();
         }
 
     }
 }
