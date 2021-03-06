 package com.jbirdvegas.mgerrit;
 
 /*
  * Copyright (C) 2013 Android Open Kang Project (AOKP)
  *  Author: Jon Stanford (JBirdVegas), 2013
  *
  *  Licensed under the Apache License, Version 2.0 (the "License");
  *  you may not use this file except in compliance with the License.
  *  You may obtain a copy of the License at
  *
  *       http://www.apache.org/licenses/LICENSE-2.0
  *
  *  Unless required by applicable law or agreed to in writing, software
  *  distributed under the License is distributed on an "AS IS" BASIS,
  *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  *  See the License for the specific language governing permissions and
  *  limitations under the License.
  */
 
 import android.app.Activity;
 import android.content.BroadcastReceiver;
 import android.content.Context;
 import android.content.Intent;
 import android.content.IntentFilter;
 import android.os.Bundle;
 import android.support.v4.app.Fragment;
 import android.support.v4.content.LocalBroadcastManager;
 import android.util.Log;
 import android.view.ContextMenu;
 import android.view.LayoutInflater;
 import android.view.MenuItem;
 import android.view.View;
 import android.view.ViewGroup;
 import android.widget.Button;
 import android.widget.ViewSwitcher;
 
 import com.android.volley.RequestQueue;
 import com.android.volley.toolbox.Volley;
 import com.fima.cardsui.views.CardUI;
 import com.google.analytics.tracking.android.EasyTracker;
 import com.google.analytics.tracking.android.MapBuilder;
 import com.jbirdvegas.mgerrit.cards.PatchSetChangesCard;
 import com.jbirdvegas.mgerrit.cards.PatchSetCommentsCard;
 import com.jbirdvegas.mgerrit.cards.PatchSetMessageCard;
 import com.jbirdvegas.mgerrit.cards.PatchSetPropertiesCard;
 import com.jbirdvegas.mgerrit.cards.PatchSetReviewersCard;
 import com.jbirdvegas.mgerrit.database.Changes;
 import com.jbirdvegas.mgerrit.database.SelectedChange;
 import com.jbirdvegas.mgerrit.helpers.Tools;
 import com.jbirdvegas.mgerrit.message.ChangeLoadingFinished;
 import com.jbirdvegas.mgerrit.message.StatusSelected;
 import com.jbirdvegas.mgerrit.objects.CommitterObject;
 import com.jbirdvegas.mgerrit.objects.GerritURL;
 import com.jbirdvegas.mgerrit.objects.JSONCommit;
 import com.jbirdvegas.mgerrit.tasks.GerritService;
 import com.jbirdvegas.mgerrit.tasks.GerritTask;
 
 import org.json.JSONArray;
 import org.json.JSONException;
 
 /**
  * Class handles populating the screen with several
  * cards each giving more information about the patchset
  * <p/>
  * All cards are located at jbirdvegas.mgerrit.cards.*
  */
 public class PatchSetViewerFragment extends Fragment {
     private static final String TAG = PatchSetViewerFragment.class.getSimpleName();
 
     private ViewSwitcher mViewSwitcher;
     private CardUI mCardsUI;
     private RequestQueue mRequestQueue;
     private Activity mParent;
     private Context mContext;
 
     private GerritURL mUrl;
     private String mSelectedChange;
     private String mStatus;
 
     public static final String NEW_CHANGE_SELECTED = "Change Selected";
     public static final String EXPAND_TAG = "expand";
     public static final String CHANGE_ID = "changeID";
     public static final String STATUS = "queryStatus";
 
     private final BroadcastReceiver mStatusReceiver = new BroadcastReceiver() {
         @Override
         public void onReceive(Context context, Intent intent) {
 
             String action = intent.getAction();
             String status = intent.getStringExtra(StatusSelected.STATUS);
 
             /* We may have got a broadcast saying that data from another tab
              *  has been loaded. */
             if (compareStatus(status, getStatus()) || action.equals(StatusSelected.ACTION)) {
                 setStatus(status);
                 loadChange(false);
             }
         }
     };
 
     @Override
     public View onCreateView(LayoutInflater inflater, ViewGroup container,
                              Bundle savedInstanceState) {
         return inflater.inflate(R.layout.patch_set_list, container, false);
     }
 
     @Override
     public void onActivityCreated(Bundle savedInstanceState) {
         super.onActivityCreated(savedInstanceState);
         init();
     }
 
     @Override
     public void onCreate(Bundle savedInstanceState) {
         super.onCreate(savedInstanceState);
         mParent = this.getActivity();
         mContext = mParent.getApplicationContext();
     }
 
     private void init() {
         View currentFragment = this.getView();
 
         mCardsUI = (CardUI) currentFragment.findViewById(R.id.commit_cards);
         mViewSwitcher = (ViewSwitcher) currentFragment.findViewById(R.id.vs_patchset);
 
         mRequestQueue = Volley.newRequestQueue(mParent);
         mUrl = new GerritURL();
 
         Button retryButton = (Button) currentFragment.findViewById(R.id.btn_retry);
         retryButton.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View v) {
                 executeGerritTask(mUrl.toString());
             }
         });
 
         if (getArguments() == null) {
             /** This should be the default value of {@link ChangeListFragment.mSelectedStatus } */
             setStatus(JSONCommit.Status.NEW.toString());
             loadChange(true);
         } else {
             setStatus(getArguments().getString(STATUS));
             String changeid = getArguments().getString(CHANGE_ID);
             if (changeid != null && !changeid.isEmpty()) {
                 setSelectedChange(changeid);
             }
         }
     }
 
     private void executeGerritTask(final String query) {
         // If we aren't connected, there's nothing to do here
         if (!switchViews()) return;
 
         final long start = System.currentTimeMillis();
         new GerritTask(mParent) {
             @Override
             public void onJSONResult(String s) {
                 try {
                     mCardsUI.clearCards();
                     addCards(mCardsUI,
                             JSONCommit.getInstance(
                                     new JSONArray(s).getJSONObject(0),
                                     mContext));
                     EasyTracker.getInstance(mParent).send(
                             MapBuilder.createTiming(
                                     AnalyticsConstants.GA_PERFORMANCE,
                                     System.currentTimeMillis() - start,
                                     AnalyticsConstants.GA_TIME_TO_LOAD,
                                     AnalyticsConstants.GA_CARDS_LOAD_TIME)
                              .build()
                     );
                 } catch (JSONException e) {
                     Log.d(TAG, "Response from "
                             + query + " could not be parsed into cards :(", e);
                 }
             }
         }.execute(query);
     }
 
     private void addCards(CardUI ui, JSONCommit jsonCommit) {
         if (!Prefs.isTabletMode(mParent)) {
             String s = mParent.getResources().getString(R.string.change_detail_heading);
             mParent.setTitle(String.format(s, jsonCommit.getCommitNumber()));
         }
 
         // Properties card
         Log.d(TAG, "Loading Properties Card...");
         ui.addCard(new PatchSetPropertiesCard(jsonCommit, this, mRequestQueue, mParent), true);
 
         // Message card
         Log.d(TAG, "Loading Message Card...");
         ui.addCard(new PatchSetMessageCard(jsonCommit), true);
 
         // Changed files card
         if (jsonCommit.getChangedFiles() != null
                 && !jsonCommit.getChangedFiles().isEmpty()) {
             Log.d(TAG, "Loading Changes Card...");
             ui.addCard(new PatchSetChangesCard(jsonCommit, mParent), true);
         }
 
         // Code reviewers card
         if (jsonCommit.getCodeReviewers() != null
                 && !jsonCommit.getCodeReviewers().isEmpty()) {
             Log.d(TAG, "Loading Reviewers Card...");
             ui.addCard(new PatchSetReviewersCard(jsonCommit, mRequestQueue, mParent), true);
         } else {
             Log.d(TAG, "No reviewers found! Not adding reviewers card");
         }
 
         // Comments Card
         if (jsonCommit.getMessagesList() != null
                 && !jsonCommit.getMessagesList().isEmpty()) {
             Log.d(TAG, "Loading Comments Card...");
             ui.addCard(new PatchSetCommentsCard(jsonCommit, this, mRequestQueue), true);
         } else {
             Log.d(TAG, "No commit comments found! Not adding comments card");
         }
     }
 
     /**
      * Set the change id to load details for and load the change
      * @param changeID A valid change id
      */
     public void setSelectedChange(String changeID) {
         if (changeID == null || changeID.length() < 0) {
             return; // Invalid changeID
         } else if (changeID.equals(mSelectedChange)) {
             return; // Same change selected, no need to do anything.
         }
 
         SelectedChange.setSelectedChange(mContext, changeID);
         this.mSelectedChange = changeID;
         mUrl.setChangeID(mSelectedChange);
         mUrl.requestChangeDetail(true);
         executeGerritTask(mUrl.toString());
 
         /*
          * Requires Gerrit version 2.8
          * /changes/{change-id}/detail with arguments was introduced in version 2.8,
          * so this will not be able to get the files changed or the full commit message
          * in prior Gerrit versions.
         GerritService.sendRequest(mParent, GerritService.DataType.Commit, mUrl);
        */
     }
 
     /**
      * Determine the changeid to load and send an intent to load the change.
      *  By sending an intent, the main activity is notified (GerritControllerActivity
      *  on tablets. This can then tell the change list adapter that we have selected
      *  a change.
      *  @param direct true: load this change directly, false: send out an intent
      */
     private void loadChange(boolean direct) {
         if (mStatus == null) {
             // Without the status we cannot find a changeid to load data for
             return;
         }
 
         String changeID = SelectedChange.getSelectedChange(mContext, mStatus);
         if (changeID == null || changeID.isEmpty()) {
             changeID = Changes.getMostRecentChange(mParent, mStatus);
             if (changeID == null || changeID.isEmpty()) {
                 // No changes to load data from
                 EasyTracker.getInstance(mParent).send(
                         MapBuilder.createEvent(
                                 "PatchSetViewerFragment",
                                 "load_change",
                                 "null_changeID",
                                 null)
                         .build());
                 return;
             }
         }
 
         if (direct) setSelectedChange(changeID);
         else {
             Intent intent = new Intent(PatchSetViewerFragment.NEW_CHANGE_SELECTED);
             intent.putExtra(PatchSetViewerFragment.CHANGE_ID, changeID);
             intent.putExtra(PatchSetViewerFragment.STATUS, mStatus);
             intent.putExtra(PatchSetViewerFragment.EXPAND_TAG, true);
             LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
         }
     }
 
     /**
      * Use this to set the status to ensure we only use the database status
      * @param status A valid change status string (database or web format)
      */
     public void setStatus(String status) {
         this.mStatus = JSONCommit.Status.getStatusString(status);
     }
 
     public boolean compareStatus(String status1, String status2) {
         return (JSONCommit.Status.getStatusString(status1)).equals(JSONCommit.Status.getStatusString(status2));
     }
 
     /**
      * Helper function to get the selected status in the change list fragment.
      *  If it is phone mode (the parent is not the main activity), then this will
      *  return null.
      * @return The selected status in the change list fragment, or null if not available
      */
     private String getStatus() {
         if (mParent instanceof GerritControllerActivity) {
             GerritControllerActivity controllerActivity = (GerritControllerActivity) mParent;
             return controllerActivity.getChangeList().getStatus();
         }
         return null;
     }
 
     @Override
     public void onResume() {
         super.onResume();
         LocalBroadcastManager.getInstance(mParent).registerReceiver(mStatusReceiver,
                 new IntentFilter(StatusSelected.ACTION));
 
         // If we cannot get the status, it is likely phone mode.
         if (getStatus() != null) {
             LocalBroadcastManager.getInstance(mParent).registerReceiver(mStatusReceiver,
                     new IntentFilter(ChangeLoadingFinished.ACTION));
         }
     }
 
     @Override
     public void onStart() {
         super.onStart();
         EasyTracker.getInstance(mParent).activityStart(mParent);
     }
 
     @Override
     public void onStop() {
         super.onStop();
         EasyTracker.getInstance(mParent).activityStop(mParent);
     }
 
     @Override
     public void onPause() {
         super.onPause();
         LocalBroadcastManager.getInstance(mParent).unregisterReceiver(mStatusReceiver);
     }
 
     /*
     Possible cards
 
     --Patch Set--
     Select patchset number to display in these cards
     -------------
 
     --Times Card--
     Original upload time
     Most recent update
     --------------
 
     --Inline comments Card?--
     Show all comments inlined on code view pages
     **may be kind of pointless without context of surrounding code**
     * maybe a webview for each if possible? *
     -------------------------
 
      */
 
     private CommitterObject committerObject = null;
 
     public void registerViewForContextMenu(View view) {
         registerForContextMenu(view);
     }
 
     private static final int OWNER = 0;
     private static final int REVIEWER = 1;
 
     @Override
     public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
         committerObject = (CommitterObject) v.getTag();
         menu.setHeaderTitle(R.string.developers_role);
         menu.add(0, v.getId(), OWNER, v.getContext().getString(R.string.context_menu_owner));
         menu.add(0, v.getId(), REVIEWER, v.getContext().getString(R.string.context_menu_reviewer));
     }
 
     @Override
     public boolean onContextItemSelected(MenuItem item) {
         String tab = null;
         switch (item.getOrder()) {
             case OWNER:
                 tab = CardsFragment.KEY_OWNER;
                 break;
             case REVIEWER:
                 tab = CardsFragment.KEY_REVIEWER;
         }
         committerObject.setState(tab);
         Intent intent = new Intent(mParent, ReviewTab.class);
         intent.putExtra(CardsFragment.KEY_DEVELOPER, committerObject);
         intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY);
         startActivity(intent);
         return true;
     }
 
     private boolean switchViews() {
         boolean isconn = Tools.isConnected(mParent);
         if (isconn) {
             // Switch to first child
             if (mViewSwitcher.getDisplayedChild() != 0) mViewSwitcher.showPrevious();
         } else {
             // Switch to second child
             if (mViewSwitcher.getDisplayedChild() != 1) mViewSwitcher.showNext();
         }
         return isconn;
     }
 }
