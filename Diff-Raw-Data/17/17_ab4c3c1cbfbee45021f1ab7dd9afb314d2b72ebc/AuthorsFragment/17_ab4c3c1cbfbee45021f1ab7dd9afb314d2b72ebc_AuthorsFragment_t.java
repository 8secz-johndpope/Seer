 /*
  * Copyright 2013 Gleb Godonoga.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *    http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 package com.andrada.sitracker.ui.fragment;
 
 import android.content.Intent;
 import android.net.ConnectivityManager;
 import android.net.NetworkInfo;
 import android.os.Bundle;
 import android.support.v4.app.Fragment;
 import android.support.v7.app.ActionBar;
 import android.support.v7.app.ActionBarActivity;
 import android.support.v7.view.ActionMode;
 import android.view.LayoutInflater;
 import android.view.Menu;
 import android.view.MenuInflater;
 import android.view.MenuItem;
 import android.view.View;
 import android.view.ViewGroup;
 import android.view.ViewStub;
 import android.view.animation.AnimationUtils;
 import android.widget.ListView;
 
 import com.andrada.sitracker.Constants;
 import com.andrada.sitracker.R;
 import com.andrada.sitracker.contracts.AuthorUpdateStatusListener;
 import com.andrada.sitracker.db.beans.Author;
 import com.andrada.sitracker.events.AuthorAddedEvent;
 import com.andrada.sitracker.events.AuthorSelectedEvent;
 import com.andrada.sitracker.events.AuthorSortMethodChanged;
 import com.andrada.sitracker.events.ProgressBarToggleEvent;
 import com.andrada.sitracker.events.PublicationMarkedAsReadEvent;
 import com.andrada.sitracker.tasks.UpdateAuthorsTask_;
 import com.andrada.sitracker.ui.MultiSelectionUtil;
 import com.andrada.sitracker.ui.fragment.adapters.AuthorsAdapter;
 import com.google.analytics.tracking.android.EasyTracker;
 
 import org.androidannotations.annotations.AfterViews;
 import org.androidannotations.annotations.Bean;
 import org.androidannotations.annotations.EFragment;
 import org.androidannotations.annotations.InstanceState;
 import org.androidannotations.annotations.ItemClick;
 import org.androidannotations.annotations.OptionsItem;
 import org.androidannotations.annotations.OptionsMenu;
 import org.androidannotations.annotations.SystemService;
 import org.androidannotations.annotations.ViewById;
 import org.apache.commons.lang3.ArrayUtils;
 
 import java.util.ArrayList;
 import java.util.Timer;
 import java.util.TimerTask;
 
 import de.greenrobot.event.EventBus;
 import de.keyboardsurfer.android.widget.crouton.Configuration;
 import de.keyboardsurfer.android.widget.crouton.Crouton;
 import de.keyboardsurfer.android.widget.crouton.Style;
 
 @EFragment(R.layout.fragment_listview_with_empty)
 @OptionsMenu(R.menu.authors_menu)
 public class AuthorsFragment extends Fragment implements AuthorUpdateStatusListener,
         MultiSelectionUtil.MultiChoiceModeListener, View.OnClickListener {
 
     @ViewById
     ListView list;
 
     @ViewById
     ViewStub empty;
 
     @Bean
     AuthorsAdapter adapter;
 
     @SystemService
     ConnectivityManager connectivityManager;
 
     @InstanceState
     long currentAuthorIndex = -1;
 
     private Crouton mNoNetworkCrouton;
 
     private boolean mIsUpdating = false;
 
     @InstanceState
     long[] checkedItems;
 
     private final ArrayList<Long> mSelectedAuthors = new ArrayList<Long>();
 
     private MultiSelectionUtil.Controller mMultiSelectionController;
 
     //region Fragment lifecycle overrides
 
     @Override
     public void onCreate(Bundle savedInstanceState) {
         super.onCreate(savedInstanceState);
         setRetainInstance(true);
         EventBus.getDefault().register(this);
     }
 
     @Override
     public void onStart() {
         super.onStart();
         mIsUpdating = false;
         getActivity().supportInvalidateOptionsMenu();
         currentAuthorIndex = currentAuthorIndex == -1 ? adapter.getFirstAuthorId() : currentAuthorIndex;
         EventBus.getDefault().post(new AuthorSelectedEvent(currentAuthorIndex));
         // Set the item as checked to be highlighted
         adapter.setSelectedItem(currentAuthorIndex);
         adapter.notifyDataSetChanged();
     }
 
     @Override
     public void onDestroy() {
         super.onDestroy();
         EventBus.getDefault().unregister(this);
         if (mMultiSelectionController != null) {
             mMultiSelectionController.finish();
         }
         mMultiSelectionController = null;
     }
 
     //endregion
 
     @Override
     public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
         if (mIsUpdating) {
             menu.removeItem(R.id.action_refresh);
         }
         super.onCreateOptionsMenu(menu, inflater);
     }
 
     //region Menu item tap handlers
     @OptionsItem(R.id.action_add)
     void menuAddSelected() {
         AddAuthorDialog authorDialog = new AddAuthorDialog();
         authorDialog.show(getActivity().getSupportFragmentManager(),
                 Constants.DIALOG_ADD_AUTHOR);
         EasyTracker.getTracker().sendView(Constants.GA_SCREEN_ADD_DIALOG);
     }
 
     @OptionsItem(R.id.action_refresh)
     void menuRefreshSelected() {
         if (!mIsUpdating && adapter.getCount() > 0) {
             final NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
             if (activeNetwork != null && activeNetwork.isConnected()) {
                 Intent updateIntent = new Intent(getActivity(), UpdateAuthorsTask_.class);
                 updateIntent.putExtra(Constants.UPDATE_IGNORES_NETWORK, true);
                 getActivity().startService(updateIntent);
                 EasyTracker.getTracker().sendEvent(
                         Constants.GA_UI_CATEGORY,
                         Constants.GA_EVENT_AUTHORS_MANUAL_REFRESH,
                         Constants.GA_EVENT_AUTHORS_MANUAL_REFRESH, null);
                 EasyTracker.getInstance().dispatch();
                 toggleUpdatingState();
             } else {
                 //Surface crouton that network is unavailable
                 showNoNetworkCroutonMessage();
             }
         }
     }
 
     //endregion
 
     @Override
     /**
      * Crouton click handler
      */
     public void onClick(View view) {
         if (view.getId() == R.id.retryUpdateButton) {
             if (this.mNoNetworkCrouton != null) {
                 Crouton.hide(this.mNoNetworkCrouton);
                 this.mNoNetworkCrouton = null;
             }
             Timer timer = new Timer();
             timer.schedule(new TimerTask() {
                 @Override
                 public void run() {
                     menuRefreshSelected();
                 }
             }, 1500);
         }
 
     }
 
     @AfterViews
     void bindAdapter() {
         list.setAdapter(adapter);
         mMultiSelectionController = MultiSelectionUtil.attachMultiSelectionController(
                 list,
                 (ActionBarActivity) getActivity(),
                 this);
         //ActionMode.setMultiChoiceMode(list, getActivity(), this);
         list.setBackgroundResource(R.drawable.authors_list_background);
         empty.setLayoutResource(R.layout.empty_authors);
         list.setEmptyView(empty);
         mMultiSelectionController.tryRestoreInstanceState(checkedItems);
     }
 
     @ItemClick
     public void listItemClicked(int position) {
         // Notify the parent activity of selected item
         currentAuthorIndex = list.getItemIdAtPosition(position);
         EventBus.getDefault().post(new AuthorSelectedEvent(currentAuthorIndex));
         // Set the item as checked to be highlighted
         adapter.setSelectedItem(currentAuthorIndex);
         adapter.notifyDataSetChanged();
 
     }
 
     private void toggleUpdatingState() {
         mIsUpdating = !mIsUpdating;
         ActionBar bar = ((ActionBarActivity) getActivity()).getSupportActionBar();
         bar.setDisplayShowHomeEnabled(!mIsUpdating);
         bar.setDisplayShowTitleEnabled(!mIsUpdating);
         bar.setDisplayShowCustomEnabled(mIsUpdating);
 
         EventBus.getDefault().post(new ProgressBarToggleEvent(mIsUpdating));
         if (mIsUpdating) {
             View mLogoView = LayoutInflater.from(getActivity()).inflate(R.layout.updating_actionbar_layout, null);
 
             bar.setCustomView(mLogoView, new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                     ViewGroup.LayoutParams.MATCH_PARENT));
             mLogoView.clearAnimation();
             mLogoView.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.ab_custom_view_anim));
         }
         getActivity().supportInvalidateOptionsMenu();
     }
 
 
     //region Public methods
     public boolean isUpdating() {
         return mIsUpdating;
     }
 
     public String getCurrentSelectedAuthorName() {
         String name = "";
         if (adapter.getCurrentlySelectedAuthor() != null) {
             name = adapter.getCurrentlySelectedAuthor().getName();
         }
         return name;
     }
 
     //endregion
 
     //region AuthorUpdateStatusListener callbacks
     @Override
     public void onAuthorsUpdated() {
         if (isUpdating()) {
             toggleUpdatingState();
         }
         adapter.reloadAuthors();
     }
 
     @Override
     public void onAuthorsUpdateFailed() {
         toggleUpdatingState();
         //surface crouton that update failed
         Crouton.makeText(getActivity(),
                 getResources().getText(R.string.update_failed_crouton_message),
                 Style.ALERT).show();
     }
     //endregion
 
     //region CABListener
 
     @Override
     public void onItemCheckedStateChanged(ActionMode mode,
                                           int position, long id, boolean checked) {
         if (checked) {
             mSelectedAuthors.add(((Author) adapter.getItem(position)).getId());
         } else {
             mSelectedAuthors.remove(((Author) adapter.getItem(position)).getId());
         }
         int numSelectedAuthors = mSelectedAuthors.size();
         mode.setTitle(getResources().getQuantityString(
                 R.plurals.authors_selected,
                 numSelectedAuthors, numSelectedAuthors));
         checkedItems = ArrayUtils.toPrimitive(mSelectedAuthors.toArray(new Long[mSelectedAuthors.size()]));
     }
 
     @Override
     public boolean onCreateActionMode(ActionMode mode, Menu menu) {
         MenuInflater inflater = mode.getMenuInflater();
         inflater.inflate(R.menu.context_authors, menu);
         mSelectedAuthors.clear();
         return true;
     }
 
     @Override
     public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
         return true;
     }
 
     @Override
     public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
         mode.finish();
         if (item.getItemId() == R.id.action_remove) {
             EasyTracker.getTracker().sendEvent(
                     Constants.GA_UI_CATEGORY,
                     Constants.GA_EVENT_AUTHOR_REMOVED,
                     Constants.GA_EVENT_AUTHOR_REMOVED, (long) mSelectedAuthors.size());
             EasyTracker.getInstance().dispatch();
             adapter.removeAuthors(mSelectedAuthors);
             currentAuthorIndex = adapter.getSelectedAuthorId();
             EventBus.getDefault().post(new AuthorSelectedEvent(currentAuthorIndex));
             return true;
        } else if (item.getItemId() == R.id.action_mark_read) {
             adapter.markAuthorsRead(mSelectedAuthors);
             return true;
         }
         return false;
     }
 
     @Override
     public void onDestroyActionMode(ActionMode mode) {
         checkedItems = null;
     }
 
     //endregion
 
     public void onEvent(PublicationMarkedAsReadEvent event) {
         //ensure we update the new status of the author if he has no new publications
         EasyTracker.getTracker().sendEvent(
                 Constants.GA_UI_CATEGORY,
                 Constants.GA_EVENT_AUTHOR_MANUAL_READ,
                 Constants.GA_EVENT_AUTHOR_MANUAL_READ, null);
         EasyTracker.getInstance().dispatch();
         if (event.refreshAuthor) {
             adapter.reloadAuthors();
         }
     }
 
 
     //region AuthorAddedEvent handler
 
     @SuppressWarnings("UnusedParameters")
     public void onEvent(AuthorSortMethodChanged event) {
         adapter.reloadAuthors();
     }
 
     public void onEvent(AuthorAddedEvent event) {
 
         EventBus.getDefault().post(new ProgressBarToggleEvent(false));
         String message = event.message;
 
         EasyTracker.getTracker().sendEvent(
                 Constants.GA_UI_CATEGORY,
                 Constants.GA_EVENT_AUTHOR_ADDED,
                 Constants.GA_EVENT_AUTHOR_ADDED, (long) message.length());
         EasyTracker.getInstance().dispatch();
 
         //Stop progress bar
 
         Style.Builder alertStyle = new Style.Builder()
                 .setTextAppearance(android.R.attr.textAppearanceLarge)
                 .setPaddingInPixels(25);
 
         if (message.length() == 0) {
             //This is success
             adapter.reloadAuthors();
             alertStyle.setBackgroundColorValue(Style.holoGreenLight);
             message = getResources().getString(R.string.author_add_success_crouton_message);
         } else {
             alertStyle.setBackgroundColorValue(Style.holoRedLight);
         }
         Crouton.makeText(getActivity(), message, alertStyle.build()).show();
 
         if (currentAuthorIndex == -1) {
             currentAuthorIndex = adapter.getFirstAuthorId();
             EventBus.getDefault().post(new AuthorSelectedEvent(currentAuthorIndex));
             // Set the item as checked to be highlighted
             adapter.setSelectedItem(currentAuthorIndex);
             adapter.notifyDataSetChanged();
         }
     }
 
     //endregion
 
 
     private void showNoNetworkCroutonMessage() {
         View view = getLayoutInflater(null).inflate(R.layout.crouton_no_network, null);
         view.findViewById(R.id.retryUpdateButton).setOnClickListener(this);
         Configuration croutonConfiguration = new Configuration.Builder()
                 .setDuration(Configuration.DURATION_LONG).build();
         this.mNoNetworkCrouton = Crouton.make(getActivity(), view);
         this.mNoNetworkCrouton.setConfiguration(croutonConfiguration);
         this.mNoNetworkCrouton.show();
     }
 
     public AuthorsAdapter getAdapter() {
         return adapter;
     }
 
     public ListView getListView() {
         return list;
     }
 
 }
