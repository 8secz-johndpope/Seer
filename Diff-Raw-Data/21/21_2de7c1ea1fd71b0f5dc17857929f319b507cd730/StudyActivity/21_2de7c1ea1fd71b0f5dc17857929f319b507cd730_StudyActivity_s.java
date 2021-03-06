 /*
 Copyright (C) 2012 Haowen Ning
 
 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.
 
 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 
 See the GNU General Public License for more details.
 
 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 
 */
 package org.liberty.android.fantastischmemo.ui;
 
 import java.util.Date;
 import java.util.HashMap;
 
 import javax.inject.Inject;
 
 import org.apache.commons.lang3.StringUtils;
 import org.liberty.android.fantastischmemo.R;
 import org.liberty.android.fantastischmemo.aspect.LogInvocation;
 import org.liberty.android.fantastischmemo.dao.CardDao;
 import org.liberty.android.fantastischmemo.dao.CategoryDao;
 import org.liberty.android.fantastischmemo.dao.LearningDataDao;
 import org.liberty.android.fantastischmemo.domain.Card;
 import org.liberty.android.fantastischmemo.domain.Category;
 import org.liberty.android.fantastischmemo.domain.LearningData;
 import org.liberty.android.fantastischmemo.domain.Option;
 import org.liberty.android.fantastischmemo.domain.Setting;
 import org.liberty.android.fantastischmemo.queue.LearnQueueManager;
 import org.liberty.android.fantastischmemo.queue.QueueManager;
 import org.liberty.android.fantastischmemo.scheduler.Scheduler;
 import org.liberty.android.fantastischmemo.ui.CategoryEditorFragment.CategoryEditorResultListener;
 import org.liberty.android.fantastischmemo.utils.AnyMemoExecutor;
 import org.liberty.android.fantastischmemo.utils.DictionaryUtil;
 import org.liberty.android.fantastischmemo.utils.ShareUtil;
 
 import roboguice.util.RoboAsyncTask;
 
 import android.app.Activity;
 import android.app.AlertDialog;
 import android.app.ProgressDialog;
 import android.content.Context;
 import android.content.DialogInterface;
 import android.content.Intent;
 import android.net.Uri;
 import android.os.AsyncTask;
 import android.os.Bundle;
 import android.support.v4.app.FragmentTransaction;
 import android.util.Log;
 import android.view.ContextMenu;
 import android.view.Menu;
 import android.view.MenuInflater;
 import android.view.MenuItem;
 import android.view.View;
 import android.widget.Toast;
 
 import com.example.android.apis.graphics.FingerPaint;
 
 public class StudyActivity extends QACardActivity {
     public static String EXTRA_DBPATH = "dbpath";
     public static String EXTRA_CATEGORY_ID = "category_id";
     public static String EXTRA_START_CARD_ID = "start_card_id";
 
     private final int ACTIVITY_FILTER = 10;
     private final int ACTIVITY_EDIT = 11;
     private final int ACTIVITY_GOTO_PREV = 14;
     private final int ACTIVITY_SETTINGS = 15;
     private final int ACTIVITY_DETAIL = 16;
     private final static String WEBSITE_HELP_MEMO="http://anymemo.org/wiki/index.php?title=Learning_screen";
 
     /* State objects */
     private Card prevCard = null;
     private String dbPath = "";
     private int filterCategoryId = -1;
     private Category filterCategory;
     private int startCardId = -1;
 
     private QueueManager queueManager;
 
     private CardDao cardDao;
     private LearningDataDao learningDataDao;
     private CategoryDao categoryDao;
 
     private Setting setting;
 
     private Option option;
 
     /* Schedulers */
     private Scheduler scheduler = null;
 
     /* current states */
     private long schedluledCardCount = 0;
     private long newCardCount = 0;
 
     boolean initialized = false;
 
     private WaitDbTask waitDbTask;
 
     private DictionaryUtil dictionaryUtil;
 
     private ShareUtil shareUtil;
 
     private GradeButtonsFragment gradeButtonsFragment;
 
     @Inject
     public void setScheduler(Scheduler scheduler) {
         this.scheduler = scheduler;
     }
 
     @Inject
     public void setDictionaryUtil(DictionaryUtil dictionaryUtil) {
         this.dictionaryUtil = dictionaryUtil;
     }
 
     @Inject
     public void setShareUtil(ShareUtil shareUtil) {
         this.shareUtil = shareUtil;
     }
 
     @Override
     public void onCreate(Bundle savedInstanceState){
         Bundle extras = getIntent().getExtras();
         if (extras != null) {
             dbPath = extras.getString(EXTRA_DBPATH);
             filterCategoryId = extras.getInt(EXTRA_CATEGORY_ID, -1);
             startCardId = extras.getInt(EXTRA_START_CARD_ID, -1);
         }
         setTitle(R.string.gestures_text);
         super.onCreate(savedInstanceState);
     }
 
     @Override
     public int getContentView() {
         return R.layout.qa_card_layout_study;
     }
 
     @Override
     public boolean onCreateOptionsMenu(Menu menu){
         MenuInflater inflater = getMenuInflater();
         inflater.inflate(R.menu.study_activity_menu, menu);
         return true;
     }
 
     @Override
     public boolean onOptionsItemSelected(MenuItem item) {
         switch (item.getItemId()) {
             case R.id.menu_memo_help:
             {
                 gotoHelp();
                 return true;
             }
             case R.id.menuspeakquestion:
             {
                 return speakQuestion();
             }
 
             case R.id.menuspeakanswer:
             {
                 return speakAnswer();
             }
 
             case R.id.menusettings:
             {
                 gotoSettings();
                 return true;
             }
 
             case R.id.menudetail:
             {
                 gotoDetail();
                 return true;
             }
 
             case R.id.menuundo:
             {
                 undoCard();
                 return true;
             }
 
             case R.id.menu_memo_category:
             {
                 showCategoriesDialog();
                 return true;
             }
 
             case R.id.menu_context_edit:
             {
                 showEditDialog();
                 return true;
             }
             case R.id.menu_context_delete:
             {
                 showDeleteDialog();
                 return true;
 
             }
             case R.id.menu_context_skip:
             {
                 showSkipDialog();
                 return true;
             }
             case R.id.menu_context_gotoprev:
             {
                 gotoPreviewEdit();
                 return true;
             }
 
             case R.id.menu_context_lookup:
             {
                 if(getCurrentCard() == null){
                     return false;
                 }
                 // Look up words in both question and answer
                 lookupDictionary();
 
                 return true;
 
             }
 
             case R.id.menu_gestures:
             {
                 showGesturesDialog();
                 return true;
             }
 
             case R.id.menu_context_paint:
             {
                 gotoPaint();
                 return true;
             }
 
             case R.id.menu_share:
             {
                 shareUtil.shareCard(getCurrentCard());
                 return true;
             }
 
         }
 
         return false;
     }
 
     @Override
     public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo){
         super.onCreateContextMenu(menu, v, menuInfo);
         MenuInflater inflater = getMenuInflater();
         inflater.inflate(R.menu.memoscreen_context_menu, menu);
         menu.setHeaderTitle(R.string.menu_text);
     }
 
     @Override
     public void onActivityResult(int requestCode, int resultCode, Intent data){
         super.onActivityResult(requestCode, resultCode, data);
 
         if (resultCode == Activity.RESULT_CANCELED) {
             return;
         }
 
         switch(requestCode){
             case ACTIVITY_FILTER:
             {
                 Bundle extras = data.getExtras();
                 filterCategoryId = extras.getInt(EXTRA_CATEGORY_ID);
                 restartActivity();
                 break;
             }
             case ACTIVITY_EDIT:
             {
                 restartActivity();
                 break;
             }
             case ACTIVITY_GOTO_PREV:
             {
                 restartActivity();
                 break;
             }
 
             case ACTIVITY_SETTINGS:
             {
                 restartActivity();
                 break;
             }
 
             case ACTIVITY_DETAIL:
             {
                 restartActivity();
                 break;
             }
 
         }
     }
 
     @Override
     public void onBackPressed() {
         Log.v(TAG, "back button pressed");
         FinishTask task = new FinishTask();
         task.execute((Void)null);
     }
 
     @Override
     public void onPause() {
         super.onPause();
         AnyMemoExecutor.submit(flushDatabaseTask);
     }
 
     @Override
     public void onDestroy() {
         super.onDestroy();
     }
 
     @Override
     public void onResume() {
         super.onResume();
         // Only if the initTask has been finished and no waitDbTask is waiting.
         if (initialized && (waitDbTask == null || !AsyncTask.Status.RUNNING.equals(waitDbTask.getStatus()))) {
             waitDbTask = new WaitDbTask();
             waitDbTask.execute((Void)null);
         } else {
             Log.i(TAG, "There is another task running. Do not run tasks");
         }
     }
 
     @Override
     public void restartActivity(){
 
         RestartTask task = new RestartTask();
         task.execute((Void)null);
     }
 
 
     @Override
     public void onInit() throws Exception {
         cardDao = getDbOpenHelper().getCardDao();
         learningDataDao = getDbOpenHelper().getLearningDataDao();
         categoryDao = getDbOpenHelper().getCategoryDao();
         setting = getSetting();
         option = getOption();
 
 
         // The query of filter cateogry should happen before createQueue
         // because creatQueue needs to use it.
         if (filterCategoryId != -1) {
             filterCategory = categoryDao.queryForId(filterCategoryId);
             assert filterCategory != null : "Query filter id: " + filterCategoryId +". Get null";
         }
 
         createQueue();
 
         /* Run the learnQueue init in a separate thread */
         if (startCardId != -1) {
             setCurrentCard(queueManager.dequeuePosition(startCardId));
         } else {
             setCurrentCard(queueManager.dequeue());
         }
         refreshStatInfo();
     }
 
     @Override
     public void onPostInit() {
         // If the db does not contain any cards. Show no item dialog.
         if (getCurrentCard() == null) {
             showNoItemDialog();
             return;
         }
         setupGradeButtons();
         displayCard(false);
         initialized = true;
         setSmallTitle(getActivityTitleString());
         setTitle(getDbName());
     }
 
     @Override
     public void onPostDisplayCard() {
         // When displaying new card, we should stop the TTS reading.
         getCardTTSUtil().stopSpeak();
         if (isAnswerShown()) {
             gradeButtonsFragment.setVisibility(View.VISIBLE);
         } else {
             // The grade button should be gone for double sided cards.
             if (setting.getCardStyle() ==  Setting.CardStyle.DOUBLE_SIDED) {
                 gradeButtonsFragment.setVisibility(View.GONE);
             } else {
                 gradeButtonsFragment.setVisibility(View.INVISIBLE);
             }
         }
 
         // Auto speak after displaying a card.
         if (option.getSpeakingType() == Option.SpeakingType.AUTO
             || option.getSpeakingType() ==Option.SpeakingType.AUTOTAP) {
             autoSpeak();
         }
     }
 
     @Override
     protected void onGestureDetected(GestureName gestureName) {
         switch (gestureName) {
             case O_SHAPE:
                 lookupDictionary();
                 break;
 
             case S_SHAPE:
                 gotoPaint();
                 break;
             default:
                 break;
 
         }
     }
 
     @Override
     protected void onClickQuestionText() {
         if ((option.getSpeakingType() == Option.SpeakingType.AUTOTAP
                 || option.getSpeakingType() == Option.SpeakingType.TAP)) {
             speakQuestion();
         } else {
             onClickQuestionView();
         }
     }
 
     @Override
     protected void onClickAnswerText() {
         if (!isAnswerShown()) {
             onClickAnswerView();
         } else {
             if ((option.getSpeakingType() == Option.SpeakingType.AUTOTAP
                         || option.getSpeakingType() == Option.SpeakingType.TAP)) {
                 speakAnswer();
             } else {
                 onClickAnswerView();
             }
         }
     }
 
     @Override
     protected void onClickQuestionView() {
         if (!isAnswerShown()) {
             displayCard(true);
         }
     }
 
     @Override
     protected void onClickAnswerView() {
         if (!isAnswerShown()) {
             displayCard(true);
         } else if (setting.getCardStyle() == Setting.CardStyle.DOUBLE_SIDED && isAnswerShown()) {
             displayCard(false);
         }
     }
 
     @Override
     protected boolean onVolumeUpKeyPressed() {
         if (isAnswerShown()) {
             gradeButtonsFragment.gradeCurrentCard(0);
             Toast.makeText(this, getString(R.string.grade_text) + " 0", Toast.LENGTH_SHORT).show();
         } else {
             displayCard(true);
         }
 
         return true;
     }
 
     @Override
     protected boolean onVolumeDownKeyPressed() {
         if (isAnswerShown()) {
             gradeButtonsFragment.gradeCurrentCard(3);
             Toast.makeText(this, getString(R.string.grade_text) + " 3", Toast.LENGTH_SHORT).show();
         } else {
             displayCard(true);
         }
         return true;
     }
 
     private void showNoItemDialog(){
         new AlertDialog.Builder(this)
             .setTitle(this.getString(R.string.memo_no_item_title))
             .setMessage(this.getString(R.string.memo_no_item_message))
             .setNeutralButton(getString(R.string.back_menu_text), new DialogInterface.OnClickListener() {
                 @Override
                 public void onClick(DialogInterface arg0, int arg1) {
                     /* Finish the current activity and go back to the last activity.
                      * It should be the open screen. */
                         onBackPressed();
                     }
                 })
             .setOnCancelListener(new DialogInterface.OnCancelListener(){
                 public void onCancel(DialogInterface dialog){
                         onBackPressed();
                     }
                 })
             .create()
             .show();
     }
 
     /* Create the queue manager. */
     private void createQueue() {
         int queueSize = option.getQueueSize();
         LearnQueueManager.Builder builder = new LearnQueueManager.Builder()
             .setDbOpenHelper(getDbOpenHelper())
             .setScheduler(scheduler)
             .setLearnQueueSize(queueSize)
             .setCacheSize(50)
             .setFilterCategory(filterCategory);
         if (option.getShuffleType() == Option.ShuffleType.LOCAL) {
             builder.setShuffle(true);
         } else {
             builder.setShuffle(false);
         }
         queueManager = builder.build();
     }
 
 
     private void autoSpeak() {
         if (getCurrentCard() != null) {
             if(!isAnswerShown()){
                 // Make sure the TTS is stop, or it will speak nothing.
                 speakQuestion();
             } else {
                 // Make sure the TTS is stop
                 speakAnswer();
             }
         }
     }
 
     private void refreshStatInfo() {
        newCardCount = cardDao.getNewCardCount(filterCategory);
        schedluledCardCount = cardDao.getScheduledCardCount(filterCategory);
     }
 
     private void showCategoriesDialog() {
         CategoryEditorFragment df = new CategoryEditorFragment();
         df.setResultListener(categoryResultListener);
         Bundle b = new Bundle();
         b.putString(CategoryEditorFragment.EXTRA_DBPATH, dbPath);
         if (filterCategory == null) {
             b.putInt(CategoryEditorFragment.EXTRA_CATEGORY_ID, getCurrentCard().getCategory().getId());
         } else {
             // If we use the category filer, we can just use the currentCategory
             // This will handle the new card situation.
             b.putInt(CategoryEditorFragment.EXTRA_CATEGORY_ID, filterCategory.getId());
         }
         df.setArguments(b);
         df.show(getSupportFragmentManager(), "CategoryEditDialog");
         getSupportFragmentManager().findFragmentByTag("CategoryEditDialog");
     }
 
 
 
     @LogInvocation
     private void setupGradeButtons() {
         gradeButtonsFragment = new GradeButtonsFragment();
 
         FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
         ft.replace(R.id.grade_buttons_fragment, gradeButtonsFragment);
         ft.commit();
 
         gradeButtonsFragment.setOnCardChangedListener(onCardChangedListener);
     }
 
     private class WaitDbTask extends AsyncTask<Void, Void, Void> {
         private ProgressDialog progressDialog;
 
         @Override
         public void onPreExecute(){
             super.onPreExecute();
             progressDialog = new ProgressDialog(StudyActivity.this);
             progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
             progressDialog.setTitle(getString(R.string.loading_please_wait));
             progressDialog.setMessage(getString(R.string.loading_save));
             progressDialog.show();
         }
 
         @Override
         public Void doInBackground(Void... nothing) {
             AnyMemoExecutor.submit(flushDatabaseTask);
             AnyMemoExecutor.waitAllTasks();
             Log.v(TAG, "DB task completed.");
             return null;
         }
 
         @Override
         public void onPostExecute(Void result){
             super.onPostExecute(result);
             if (!isCancelled()) {
                 progressDialog.dismiss();
             }
         }
     }
 
     private class FinishTask extends WaitDbTask {
         @Override
         public void onPostExecute(Void result){
             super.onPostExecute(result);
 
             finish();
         }
     }
 
     /*
      * When the user select the undo from the menu
      * this is what to do
      */
     private void undoCard(){
         if (prevCard != null) {
             // We don't want the queueManager to flush the card
             // instead we update the previous learning data
             // manually.
 
             queueManager.remove(prevCard);
             queueManager.update(prevCard);
             setCurrentCard(prevCard);
             restartActivity();
         } else {
             new AlertDialog.Builder(this)
                 .setTitle(getString(R.string.undo_fail_text))
                 .setMessage(getString(R.string.undo_fail_message))
                 .setNeutralButton(R.string.ok_text, null)
                 .create()
                 .show();
         }
     }
 
     /* When restarting an activity, we have to flush db first. */
     private class RestartTask extends WaitDbTask {
         @Override
         public void onPostExecute(Void result){
             super.onPostExecute(result);
 
             finish();
             Intent myIntent = new Intent(StudyActivity.this, StudyActivity.class);
             myIntent.putExtra(EXTRA_DBPATH, dbPath);
             myIntent.putExtra(EXTRA_CATEGORY_ID, filterCategoryId);
 
             if (getCurrentCard() != null ) {
                 myIntent.putExtra(EXTRA_START_CARD_ID, getCurrentCard().getId());
             }
 
             startActivity(myIntent);
         }
     }
 
     // Task to change the card after a card is graded
     // It needs to update the old card and dequeue the new card
     // and display it.
     private class ChangeCardTask extends RoboAsyncTask<Card> {
 
         private Card updatedCard;
 
         public ChangeCardTask(Context context, Card updatedCard) {
             super(context);
             this.updatedCard = updatedCard;
         }
 
         @Override
         public Card call() throws Exception {
             queueManager.update(updatedCard);
 
             Card nextCard = queueManager.dequeue();
 
             return nextCard;
         }
 
         public void onSuccess(Card result) {
             if (result == null) {
                 showNoItemDialog();
             } else {
                 setCurrentCard(result);
                 displayCard(false);
             }
         }
     }
 
     private Runnable flushDatabaseTask = new Runnable() {
         public void run() {
             queueManager.flush();
         }
     };
 
     // When a category is selected in category fragment.
     private CategoryEditorResultListener categoryResultListener =
         new CategoryEditorResultListener() {
             public void onReceiveCategory(Category c) {
                 assert c != null : "Receive null category";
                 filterCategoryId = c.getId();
 
                 // Do not restart with the current card
                 setCurrentCard(null);
                 restartActivity();
             }
         };
 
     private GradeButtonsFragment.OnCardChangedListener onCardChangedListener =
         new GradeButtonsFragment.OnCardChangedListener() {
             public void onCardChanged(Card prevCard, Card updatedCard) {
                 StudyActivity.this.prevCard = prevCard;
                 gradeButtonsFragment.setVisibility(View.INVISIBLE);
 
                 // Set the stat info in the title
                 if (scheduler.isCardNew(prevCard.getLearningData())) {
                     newCardCount -= 1;
                     if (!scheduler.isCardLearned(updatedCard.getLearningData())) {
                         schedluledCardCount += 1;
                     }
                 } else {
                     if (scheduler.isCardLearned(updatedCard.getLearningData())) {
                         schedluledCardCount -= 1;
                     }
                 }
                 setSmallTitle(getActivityTitleString());
 
                 // Run the task to update the updatedCard in the queue
                 // and dequeue the next card 
                 ChangeCardTask task = new ChangeCardTask(StudyActivity.this, updatedCard);
                 task.execute(); 
             }
         };
 
     private String getActivityTitleString() {
         StringBuilder sb = new StringBuilder();
         sb.append(getString(R.string.new_text) + ": " + newCardCount + " ");
         sb.append(getString(R.string.review_short_text) + ": " + schedluledCardCount + " ");
         sb.append(getString(R.string.id_text) + ": " + getCurrentCard().getId() + " ");
         if (StringUtils.isNotEmpty(getCurrentCard().getCategory().getName())) {
             sb.append(getString(R.string.category_short_text) + ": " + getCurrentCard().getCategory().getName());
         }
         return sb.toString();
     }
 
     private void skipCurrentCard() {
         if(getCurrentCard() != null) {
             LearningData ld = getCurrentCard().getLearningData();
             ld.setNextLearnDate(new Date(Long.MAX_VALUE));
             ld.setAcqReps(1);
             learningDataDao.update(ld);
             // Do not restart this card
             setCurrentCard(null);
             restartActivity();
         }
     }
 
     private void showGesturesDialog() {
         final HashMap<String, String> gestureNameDescriptionMap
             = new HashMap<String, String>();
         gestureNameDescriptionMap.put(GestureName.O_SHAPE.getName(), getString(R.string.look_up_text));
         gestureNameDescriptionMap.put(GestureName.S_SHAPE.getName(), getString(R.string.paint_text));
 
 
         GestureSelectionDialogFragment df = new GestureSelectionDialogFragment();
         Bundle args = new Bundle();
         args.putSerializable(GestureSelectionDialogFragment.EXTRA_GESTURE_NAME_DESCRIPTION_MAP, gestureNameDescriptionMap);
         df.setArguments(args);
         df.show(getSupportFragmentManager(), "GestureSelectionDialog");
     }
 
     private void lookupDictionary() {
         dictionaryUtil.showLookupListDialog(getCurrentCard().getQuestion(), getCurrentCard().getAnswer());
     }
 
     private void showEditDialog() {
         Intent myIntent = new Intent(this, CardEditor.class);
         myIntent.putExtra(CardEditor.EXTRA_DBPATH, this.dbPath);
         myIntent.putExtra(CardEditor.EXTRA_CARD_ID, getCurrentCard().getId());
         myIntent.putExtra(CardEditor.EXTRA_IS_EDIT_NEW, false);
         startActivityForResult(myIntent, ACTIVITY_EDIT);
     }
 
     private void showDeleteDialog() {
         new AlertDialog.Builder(this)
             .setTitle(R.string.delete_text)
             .setMessage(R.string.delete_warning)
             .setPositiveButton(R.string.ok_text, new DialogInterface.OnClickListener(){
                 public void onClick(DialogInterface arg0, int arg1) {
                     if(getCurrentCard() != null){
                         cardDao.delete(getCurrentCard());
                         // Do not restart with this card
                         setCurrentCard(null);
                         restartActivity();
                     }
                 }
             })
         .setNegativeButton(R.string.cancel_text, null)
             .show();
     }
 
     private void showSkipDialog() {
         new AlertDialog.Builder(this)
             .setTitle(R.string.skip_text)
             .setMessage(R.string.skip_warning)
             .setPositiveButton(R.string.ok_text, new DialogInterface.OnClickListener(){
                 public void onClick(DialogInterface arg0, int arg1) {
                     skipCurrentCard();
                 }
             })
         .setNegativeButton(R.string.cancel_text, null)
             .show();
     }
 
     private void gotoPreviewEdit() {
         Intent myIntent = new Intent();
         myIntent.setClass(this, PreviewEditActivity.class);
         myIntent.putExtra(PreviewEditActivity.EXTRA_DBPATH, dbPath);
         if (getCurrentCard() != null) {
             myIntent.putExtra(PreviewEditActivity.EXTRA_CARD_ID, getCurrentCard().getId());
         }
 
         startActivity(myIntent);
     }
 
     private void gotoPaint() {
         Intent myIntent = new Intent(this, FingerPaint.class);
         startActivity(myIntent);
     }
 
     private void gotoDetail() {
         Intent myIntent = new Intent(this, DetailScreen.class);
         myIntent.putExtra(DetailScreen.EXTRA_DBPATH, this.dbPath);
         myIntent.putExtra(DetailScreen.EXTRA_CARD_ID, getCurrentCard().getId());
         startActivityForResult(myIntent, ACTIVITY_DETAIL);
     }
 
     private void gotoSettings() {
         Intent myIntent = new Intent(this, SettingsScreen.class);
         myIntent.putExtra(SettingsScreen.EXTRA_DBPATH, dbPath);
         startActivityForResult(myIntent, ACTIVITY_SETTINGS);
     }
 
     private void gotoHelp() {
         Intent myIntent = new Intent();
         myIntent.setAction(Intent.ACTION_VIEW);
         myIntent.addCategory(Intent.CATEGORY_BROWSABLE);
         myIntent.setData(Uri.parse(WEBSITE_HELP_MEMO));
         startActivity(myIntent);
     }
 }
