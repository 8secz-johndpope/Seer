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
  * limitations under the License
  */
 
 package com.android.contacts.activities;
 
 import com.android.contacts.ContactsSearchManager;
 import com.android.contacts.R;
 import com.android.contacts.interactions.ContactDeletionInteraction;
 import com.android.contacts.util.DialogManager;
 import com.android.contacts.views.editor.ContactEditorFragment;
 
 import android.app.Activity;
 import android.app.Dialog;
 import android.content.ContentValues;
 import android.content.Intent;
 import android.net.Uri;
 import android.os.Bundle;
 import android.provider.ContactsContract.Intents.Insert;
 import android.util.Log;
 import android.view.View;
 import android.view.View.OnClickListener;
 import android.widget.Button;
 
 import java.util.ArrayList;
 
 public class ContactEditorActivity extends Activity implements
         DialogManager.DialogShowingViewActivity {
     private static final String TAG = "ContactEditorActivity";
 
     private ContactEditorFragment mFragment;
     private ContactDeletionInteraction mContactDeletionInteraction;
     private Button mDoneButton;
     private Button mRevertButton;
 
     private DialogManager mDialogManager = new DialogManager(this);
 
     @Override
     public void onCreate(Bundle savedState) {
         super.onCreate(savedState);
 
         setContentView(R.layout.contact_editor_activity);
 
         mFragment = (ContactEditorFragment) getFragmentManager().findFragmentById(
                 R.id.contact_editor_fragment);
         mFragment.setListener(mFragmentListener);
         mFragment.load(getIntent().getAction(), getIntent().getData(),
                 getIntent().resolveType(getContentResolver()), getIntent().getExtras());
 
         // Depending on the use-case, this activity has Done and Revert buttons or not.
         mDoneButton = (Button) findViewById(R.id.done);
         mRevertButton = (Button) findViewById(R.id.revert);
         if (mDoneButton != null) mDoneButton.setOnClickListener(new OnClickListener() {
             @Override
             public void onClick(View v) {
                 mFragment.save(true);
             }
         });
         if (mRevertButton != null) mRevertButton.setOnClickListener(new OnClickListener() {
             @Override
             public void onClick(View v) {
                 finish();
             }
         });
 
         Log.i(TAG, getIntent().getData().toString());
     }
 
     @Override
     protected Dialog onCreateDialog(int id, Bundle args) {
         if (DialogManager.isManagedId(id)) return mDialogManager.onCreateDialog(id, args);
 
        Dialog dialog = getContactDeletionInteraction().onCreateDialog(id, args);
        if (dialog != null) return dialog;

         // Nobody knows about the Dialog
         Log.w(TAG, "Unknown dialog requested, id: " + id + ", args: " + args);
         return null;
     }
 
     @Override
    protected void onPrepareDialog(int id, Dialog dialog, Bundle args) {
        if (getContactDeletionInteraction().onPrepareDialog(id, dialog, args)) {
            return;
        }
    }

    @Override
     public void startSearch(String initialQuery, boolean selectInitialQuery, Bundle appSearchData,
             boolean globalSearch) {
         if (globalSearch) {
             super.startSearch(initialQuery, selectInitialQuery, appSearchData, globalSearch);
         } else {
             ContactsSearchManager.startSearch(this, initialQuery);
         }
     }
 
     @Override
     public void onBackPressed() {
         mFragment.save(true);
     }
 
     private ContactDeletionInteraction getContactDeletionInteraction() {
         if (mContactDeletionInteraction == null) {
             mContactDeletionInteraction = new ContactDeletionInteraction();
             mContactDeletionInteraction.attachToActivity(this);
         }
         return mContactDeletionInteraction;
     }
 
     private final ContactEditorFragment.Listener mFragmentListener =
             new ContactEditorFragment.Listener() {
         @Override
         public void onReverted() {
             finish();
         }
 
         @Override
         public void onSaveFinished(int resultCode, Intent resultIntent) {
             setResult(resultCode, resultIntent);
             finish();
         }
 
         @Override
         public void onContactSplit(Uri newLookupUri) {
             finish();
         }
 
         @Override
         public void onAccountSelectorAborted() {
             finish();
         }
 
         @Override
         public void onContactNotFound() {
             setResult(Activity.RESULT_CANCELED, null);
             finish();
         }
 
         @Override
         public void setTitleTo(int resourceId) {
             setTitle(resourceId);
         }
 
         @Override
         public void onDeleteRequested(Uri lookupUri) {
             getContactDeletionInteraction().deleteContact(lookupUri);
         }
 
         @Override
         public void onEditOtherContactRequested(
                 Uri contactLookupUri, ArrayList<ContentValues> values) {
             Intent intent = new Intent(Intent.ACTION_EDIT, contactLookupUri);
             intent.setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                     | Intent.FLAG_ACTIVITY_FORWARD_RESULT);
 
             // Pass on all the data that has been entered so far
             if (values != null && values.size() != 0) {
                 intent.putParcelableArrayListExtra(Insert.DATA, values);
             }
 
             startActivity(intent);
             finish();
         }
     };
 
     @Override
     public DialogManager getDialogManager() {
         return mDialogManager;
     }
 }
