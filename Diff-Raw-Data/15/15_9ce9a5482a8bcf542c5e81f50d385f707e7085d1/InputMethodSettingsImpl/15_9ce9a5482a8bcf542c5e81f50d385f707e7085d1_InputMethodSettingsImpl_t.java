 /*
  * Copyright (C) 2011 The Android Open Source Project
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
 
 package com.android.inputmethodcommon;
 
 import android.content.Context;
 import android.content.Intent;
 import android.graphics.drawable.Drawable;
 import android.preference.Preference;
 import android.preference.Preference.OnPreferenceClickListener;
 import android.preference.PreferenceCategory;
 import android.preference.PreferenceScreen;
 import android.provider.Settings;
 import android.text.TextUtils;
 import android.view.inputmethod.InputMethodInfo;
 import android.view.inputmethod.InputMethodManager;
 
 import java.util.List;
 
 /* package private */ class InputMethodSettingsImpl implements InputMethodSettingsInterface {
     private Preference mSubtypeEnablerPreference;
     private int mInputMethodSettingsCategoryTitleRes;
     private CharSequence mInputMethodSettingsCategoryTitle;
     private int mSubtypeEnablerTitleRes;
     private CharSequence mSubtypeEnablerTitle;
     private int mSubtypeEnablerSummaryRes;
     private CharSequence mSubtypeEnablerSummary;
     private int mSubtypeEnablerIconRes;
     private Drawable mSubtypeEnablerIcon;
 
     /**
      * Initialize internal states of this object.
      * @param context the context for this application.
      * @param prefScreen a PreferenceScreen of PreferenceActivity or PreferenceFragment.
      * @return true if this application is an IME and has two or more subtypes, false otherwise.
      */
     public boolean init(final Context context, final PreferenceScreen prefScreen) {
         final InputMethodManager imm =
                 (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
         final InputMethodInfo imi = getMyImi(context, imm);
         if (imi == null || imi.getSubtypeCount() <= 1) {
             return false;
         }
         mSubtypeEnablerPreference = new Preference(context);
         mSubtypeEnablerPreference
                 .setOnPreferenceClickListener(new OnPreferenceClickListener() {
                     @Override
                     public boolean onPreferenceClick(Preference preference) {
                         final CharSequence title = getSubtypeEnablerTitle(context);
                         final Intent intent =
                                 new Intent(Settings.ACTION_INPUT_METHOD_SUBTYPE_SETTINGS);
                         intent.putExtra(Settings.EXTRA_INPUT_METHOD_ID, imi.getId());
                         if (!TextUtils.isEmpty(title)) {
                             intent.putExtra(Intent.EXTRA_TITLE, title);
                         }
                         intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                 | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                                 | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                         context.startActivity(intent);
                         return true;
                     }
                 });
        prefScreen.addPreference(mSubtypeEnablerPreference);
         updateSubtypeEnabler();
         return true;
     }
 
     private static InputMethodInfo getMyImi(Context context, InputMethodManager imm) {
         final List<InputMethodInfo> imis = imm.getInputMethodList();
         for (int i = 0; i < imis.size(); ++i) {
             final InputMethodInfo imi = imis.get(i);
             if (imis.get(i).getPackageName().equals(context.getPackageName())) {
                 return imi;
             }
         }
         return null;
     }
 
     /**
      * {@inheritDoc}
      */
     @Override
     public void setInputMethodSettingsCategoryTitle(int resId) {
         mInputMethodSettingsCategoryTitleRes = resId;
         updateSubtypeEnabler();
     }
 
     /**
      * {@inheritDoc}
      */
     @Override
     public void setInputMethodSettingsCategoryTitle(CharSequence title) {
         mInputMethodSettingsCategoryTitleRes = 0;
         mInputMethodSettingsCategoryTitle = title;
         updateSubtypeEnabler();
     }
 
     /**
      * {@inheritDoc}
      */
     @Override
     public void setSubtypeEnablerTitle(int resId) {
         mSubtypeEnablerTitleRes = resId;
         updateSubtypeEnabler();
     }
 
     /**
      * {@inheritDoc}
      */
     @Override
     public void setSubtypeEnablerTitle(CharSequence title) {
         mSubtypeEnablerTitleRes = 0;
         mSubtypeEnablerTitle = title;
         updateSubtypeEnabler();
     }
 
     /**
      * {@inheritDoc}
      */
     @Override
     public void setSubtypeEnablerSummary(int resId) {
         mSubtypeEnablerSummaryRes = resId;
         updateSubtypeEnabler();
     }
 
     /**
      * {@inheritDoc}
      */
     @Override
     public void setSubtypeEnablerSummary(CharSequence summary) {
         mSubtypeEnablerSummaryRes = 0;
         mSubtypeEnablerSummary = summary;
         updateSubtypeEnabler();
     }
 
     /**
      * {@inheritDoc}
      */
     @Override
     public void setSubtypeEnablerIcon(int resId) {
         mSubtypeEnablerIconRes = resId;
         updateSubtypeEnabler();
     }
 
     /**
      * {@inheritDoc}
      */
     @Override
     public void setSubtypeEnablerIcon(Drawable drawable) {
         mSubtypeEnablerIconRes = 0;
         mSubtypeEnablerIcon = drawable;
         updateSubtypeEnabler();
     }
 
     private CharSequence getSubtypeEnablerTitle(Context context) {
         if (mSubtypeEnablerTitleRes != 0) {
             return context.getString(mSubtypeEnablerTitleRes);
         } else {
             return mSubtypeEnablerTitle;
         }
     }
 
     private void updateSubtypeEnabler() {
         if (mSubtypeEnablerPreference != null) {
             if (mSubtypeEnablerTitleRes != 0) {
                 mSubtypeEnablerPreference.setTitle(mSubtypeEnablerTitleRes);
             } else if (!TextUtils.isEmpty(mSubtypeEnablerTitle)) {
                 mSubtypeEnablerPreference.setTitle(mSubtypeEnablerTitle);
             }
             if (mSubtypeEnablerSummaryRes != 0) {
                 mSubtypeEnablerPreference.setSummary(mSubtypeEnablerSummaryRes);
             } else if (!TextUtils.isEmpty(mSubtypeEnablerSummary)) {
                 mSubtypeEnablerPreference.setSummary(mSubtypeEnablerSummary);
             }
             if (mSubtypeEnablerIconRes != 0) {
                 mSubtypeEnablerPreference.setIcon(mSubtypeEnablerIconRes);
             } else if (mSubtypeEnablerIcon != null) {
                 mSubtypeEnablerPreference.setIcon(mSubtypeEnablerIcon);
             }
         }
     }
 }
