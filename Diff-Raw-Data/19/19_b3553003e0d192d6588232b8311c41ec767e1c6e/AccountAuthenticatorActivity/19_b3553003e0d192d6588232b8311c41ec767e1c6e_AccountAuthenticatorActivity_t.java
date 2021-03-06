 /*
  * Copyright (C) 2012 Brian Muramatsu
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
 
 package com.btmura.android.reddit.accounts;
 
 import android.app.FragmentTransaction;
 import android.os.Bundle;
 
 import com.btmura.android.reddit.R;
 import com.btmura.android.reddit.app.AddAccountFragment;
 import com.btmura.android.reddit.app.AddAccountFragment.OnAccountAddedListener;
 
 public class AccountAuthenticatorActivity extends android.accounts.AccountAuthenticatorActivity
         implements OnAccountAddedListener {
 
     public static final String TAG = "AccountAuthenticatorActivity";
 
     @Override
     protected void onCreate(Bundle savedInstanceState) {
         super.onCreate(savedInstanceState);
         setContentView(R.layout.account_authenticator);
 
         if (savedInstanceState == null) {
             FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.account_authenticator_container, AddAccountFragment.newInstance());
             ft.commit();
         }
     }
 
     public void onAccountAdded(Bundle result) {
         setAccountAuthenticatorResult(result);
         setResult(RESULT_OK);
         finish();
     }
 
     public void onAccountCancelled() {
         finish();
     }
 }
