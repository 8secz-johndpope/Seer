 /*
 Copyright (C) 2010 Haowen Ning
 
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
 
 import org.liberty.android.fantastischmemo.AMActivity;
 import org.liberty.android.fantastischmemo.AMGUIUtility;
 import org.liberty.android.fantastischmemo.AMUtil;
 import org.liberty.android.fantastischmemo.AnyMemoDBOpenHelper;
 import org.liberty.android.fantastischmemo.AnyMemoDBOpenHelperManager;
 import org.liberty.android.fantastischmemo.AnyMemoExecutor;
 import org.liberty.android.fantastischmemo.DetailScreen;
 import org.liberty.android.fantastischmemo.R;
 import org.liberty.android.fantastischmemo.SettingsScreen;
 
 import org.liberty.android.fantastischmemo.dao.CardDao;
 import org.liberty.android.fantastischmemo.dao.CategoryDao;
 import org.liberty.android.fantastischmemo.dao.LearningDataDao;
 import org.liberty.android.fantastischmemo.dao.SettingDao;
 
 import org.liberty.android.fantastischmemo.domain.Card;
 import org.liberty.android.fantastischmemo.domain.Filter;
 import org.liberty.android.fantastischmemo.domain.LearningData;
 import org.liberty.android.fantastischmemo.domain.Option;
 import org.liberty.android.fantastischmemo.domain.Setting;
 
 import org.liberty.android.fantastischmemo.queue.QueueManager;
 
 import java.util.Locale;
 import java.util.Map;
 
 import java.sql.SQLException;
 
 import org.liberty.android.fantastischmemo.queue.QueueManagerFactory;
 
 import org.liberty.android.fantastischmemo.scheduler.DefaultScheduler;
 
 import org.liberty.android.fantastischmemo.tts.AnyMemoTTS;
 import org.liberty.android.fantastischmemo.tts.AnyMemoTTSPlatform;
 import org.liberty.android.fantastischmemo.tts.AudioFileTTS;
 
 import com.example.android.apis.graphics.FingerPaint;
 
 import android.os.AsyncTask;
 import android.app.Activity;
 import android.app.AlertDialog;
 import android.app.ProgressDialog;
 import android.app.Dialog;
 import android.content.DialogInterface;
 import android.content.Intent;
 import android.content.DialogInterface.OnClickListener;
 import android.os.Bundle;
 import android.text.ClipboardManager;
 
 import android.os.Environment;
 import android.view.Gravity;
 import android.view.Menu;
 import android.view.ContextMenu;
 import android.view.MenuInflater;
 import android.view.MenuItem;
 import android.view.View;
 import android.view.Window;
 import android.view.KeyEvent;
 import android.widget.Button;
 import android.os.Handler;
 import android.widget.LinearLayout;
 import android.widget.LinearLayout.LayoutParams;
 import android.widget.Toast;
 import android.util.Log;
 import android.net.Uri;
 
 public class MemoScreen extends AMActivity {
     public static String EXTRA_DBPATH = "dbpath";
     public static String EXTRA_CATEGORY = "category";
     private AnyMemoTTS questionTTS = null;
     private AnyMemoTTS answerTTS = null;
     private final int DIALOG_LOADING_PROGRESS = 100;
     private final int ACTIVITY_FILTER = 10;
     private final int ACTIVITY_EDIT = 11;
     private final int ACTIVITY_GOTO_PREV = 14;
     private final int ACTIVITY_SETTINGS = 15;
     private final int ACTIVITY_DETAIL = 16;
     private final static String WEBSITE_HELP_MEMO="http://anymemo.org/wiki/index.php?title=Learning_screen";
     private WaitDbTask waitDbTask;
 
     /* This is useful to determine which view is click or long clicked */
     private View activeView = null;
 
     Handler mHandler;
     Card currentCard = null;
     Card prevCard = null;
     String dbPath = "";
     String dbName = "";
     String activeCategory = "";
     FlashcardDisplay flashcardDisplay;
 
     SettingDao settingDao;
     CardDao cardDao;
     LearningDataDao learningDataDao;
     CategoryDao categoryDao;
     Option option;
     Setting setting;
     
     ControlButtons controlButtons;
     QueueManager queueManager;
     volatile boolean buttonDisabled = false;
     /* Maintain this task for the exiting sync purpose*/
     GradeTask gradeTask = null;
     InitTask initTask = null;
     DefaultScheduler scheduler = null;
     private long schedluledCardCount = 0;
     private long newCardCount = 0;
 
 
     @Override
 	public void onCreate(Bundle savedInstanceState){
         super.onCreate(savedInstanceState);
         initTask = new InitTask();
         initTask.execute((Void)null);
     }
 
     void createQueue(){
         queueManager = QueueManagerFactory.buildLearnQueueManager(
                 cardDao,
                 learningDataDao,
                 10,
                 50) ;
     }
 
     @Override
     public void onResume() {
         super.onResume();
         // Only if the initTask has been finished and no waitDbTask is waiting.
         if ((initTask != null && AsyncTask.Status.FINISHED.equals(initTask.getStatus()))
                 && (waitDbTask == null || !AsyncTask.Status.RUNNING.equals(waitDbTask.getStatus()))) {
             waitDbTask = new WaitDbTask();
             waitDbTask.execute((Void)null);
         } else {
             Log.i(TAG, "There is another task running. Do not run tasks");
         }
     }
 
     @Override
     public void onPause(){
         super.onPause();
         AnyMemoExecutor.submit(flushDatabaseTask);
     }
 
     @Override
     public void onDestroy(){
         Log.v(TAG, "onDestroy now!");
         AnyMemoDBOpenHelperManager.releaseHelper(dbPath);
         if(questionTTS != null){
             questionTTS.shutdown();
         }
         if(answerTTS != null){
             answerTTS.shutdown();
         }
         // TODO: Need another way to update widgets
         /* Update the widget because MemoScreen can be accessed though widget*/
         //Intent myIntent = new Intent(this, AnyMemoService.class);
         //myIntent.putExtra("request_code", AnyMemoService.CANCEL_NOTIFICATION | AnyMemoService.UPDATE_WIDGET);
         //startService(myIntent);
        super.onDestroy();
     }
 
     @Override
     public boolean onCreateOptionsMenu(Menu menu){
         MenuInflater inflater = getMenuInflater();
         inflater.inflate(R.menu.memo_screen_menu, menu);
         return true;
     }
 
     @Override
     public boolean onOptionsItemSelected(MenuItem item) {
         switch (item.getItemId()) {
             case R.id.menu_memo_help:
             {
                 Intent myIntent = new Intent();
                 myIntent.setAction(Intent.ACTION_VIEW);
                 myIntent.addCategory(Intent.CATEGORY_BROWSABLE);
                 myIntent.setData(Uri.parse(WEBSITE_HELP_MEMO));
                 startActivity(myIntent);
                 return true;
             }
             case R.id.menuspeakquestion:
             {
                 if(questionTTS != null && currentCard != null){
                     questionTTS.sayText(currentCard.getQuestion());
                 }
                 return true;
             }
 
             case R.id.menuspeakanswer:
             {
                 if(answerTTS != null && currentCard != null){
                     answerTTS.sayText(currentCard.getAnswer());
                 }
                 return true;
             }
 
             case R.id.menusettings:
             {
                 Intent myIntent = new Intent(this, SettingsScreen.class);
                 myIntent.putExtra("dbname", dbName);
                 myIntent.putExtra("dbpath", dbPath);
                 startActivityForResult(myIntent, ACTIVITY_SETTINGS);
                 return true;
             }
 
             case R.id.menudetail:
             {
                 Intent myIntent = new Intent(this, DetailScreen.class);
                 myIntent.putExtra("dbname", this.dbName);
                 myIntent.putExtra("dbpath", this.dbPath);
                 myIntent.putExtra("itemid", currentCard.getId());
                 startActivityForResult(myIntent, ACTIVITY_DETAIL);
                 return true;
             }
 
             case R.id.menuundo:
             {
                 undoCard();
                 return true;
             }
 
             case R.id.menu_memo_filter:
             {
                 Intent myIntent = new Intent(this, Filter.class);
                 myIntent.putExtra("dbname", dbName);
                 myIntent.putExtra("dbpath", dbPath);
                 startActivityForResult(myIntent, ACTIVITY_FILTER);
                 return true;
             }
         }
 
         return false;
     }
 
     /* 
      * When the user select the undo from the menu
      * this is what to do
      */
     protected void undoCard(){
         if(prevCard != null){
             currentCard = prevCard;
             prevCard = null;
             updateFlashcardView(false);
             hideButtons();
         }
         else{
             new AlertDialog.Builder(this)
                 .setTitle(getString(R.string.undo_fail_text))
                 .setMessage(getString(R.string.undo_fail_message))
                 .setNeutralButton(R.string.ok_text, null)
                 .create()
                 .show();
         }
     }
 
 
     @Override
     public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo){
         super.onCreateContextMenu(menu, v, menuInfo);
         MenuInflater inflater = getMenuInflater();
         inflater.inflate(R.menu.memoscreen_context_menu, menu);
         menu.setHeaderTitle(R.string.menu_text);
     }
 
     @Override
     public boolean onContextItemSelected(MenuItem menuitem) {
         switch(menuitem.getItemId()) {
             case R.id.menu_context_edit:
             {
                 Intent myIntent = new Intent(this, CardEditor.class);
                 myIntent.putExtra(CardEditor.EXTRA_DBPATH, this.dbPath);
                 myIntent.putExtra(CardEditor.EXTRA_CARD_ID, currentCard.getId());
                 myIntent.putExtra(CardEditor.EXTRA_IS_EDIT_NEW, false);
                 startActivityForResult(myIntent, ACTIVITY_EDIT);
                 return true;
             }
             case R.id.menu_context_delete:
             {
                 if(currentCard != null){
                     // TODO: Cascade delete
                     try {
                         cardDao.delete(currentCard);
                     } catch (SQLException e) {
                         Log.e(TAG, "Delete card error", e);
                     }
 
                 }
                 return true;
 
             }
             case R.id.menu_context_skip:
             {
                 // TODO: SKIP
                 return true;
             }
             case R.id.menu_context_gotoprev:
             {
                 // TODO: Edit screen
                 //Intent myIntent = new Intent(this, EditScreen.class);
                 //myIntent.putExtra("dbname", this.dbName);
                 //myIntent.putExtra("dbpath", this.dbPath);
                 //myIntent.putExtra("id", currentCard.getId());
                 //startActivityForResult(myIntent, ACTIVITY_GOTO_PREV);
                 return true;
             }
 
             case R.id.menu_context_lookup:
             {
                 if(currentCard == null){
                     return false;
                 }
                 /* default word to lookup is question */
                 String lookupWord = currentCard.getQuestion();
 
                 if(flashcardDisplay.getAnswerView() == activeView){
                     lookupWord = currentCard.getAnswer();
                 }
                     
 
                 if(setting.getDictApp() == Setting.DictApp.COLORDICT){
                     System.out.println("Get COLORDICT");
                     Intent intent = new Intent("colordict.intent.action.SEARCH");
                     intent.putExtra("EXTRA_QUERY", lookupWord);
                     intent.putExtra("EXTRA_FULLSCREEN", false);
                     //intent.putExtra(EXTRA_HEIGHT, 400); //400pixel, if you don't specify, fill_parent"
                     intent.putExtra("EXTRA_GRAVITY", Gravity.BOTTOM);
                     //intent.putExtra(EXTRA_MARGIN_LEFT, 100);
                     try{
                         startActivity(intent);
                     }
                     catch(Exception e){
                         Log.e(TAG, "Error opening ColorDict", e);
                         AMGUIUtility.displayException(this, getString(R.string.error_text), getString(R.string.dict_colordict) + " " + getString(R.string.error_no_dict), e);
                     }
                 }
                 if(setting.getDictApp() == Setting.DictApp.FORA){
                     System.out.println("Get FORA");
                     Intent intent = new Intent("com.ngc.fora.action.LOOKUP");
                     intent.putExtra("HEADWORD", lookupWord);
                     try{
                         startActivity(intent);
                     }
                     catch(Exception e){
                         Log.e(TAG, "Error opening Fora", e);
                         AMGUIUtility.displayException(this, getString(R.string.error_text), getString(R.string.dict_fora) + " " + getString(R.string.error_no_dict), e);
                     }
                 }
 
                 return true;
 
             }
 
             case R.id.menu_context_paint:
             {
                 Intent myIntent = new Intent(this, FingerPaint.class);
                 startActivity(myIntent);
             }
 
             default:
             {
                 return super.onContextItemSelected(menuitem);
             }
         }
     }
 
     @Override
     public void onActivityResult(int requestCode, int resultCode, Intent data){
         super.onActivityResult(requestCode, resultCode, data);
 
         if(resultCode ==Activity.RESULT_CANCELED){
             return;
         }
         switch(requestCode){
             case ACTIVITY_FILTER:
             {
                 Bundle extras = data.getExtras();
                 activeCategory = extras.getString("filter");
                 restartActivity();
                 break;
             }
             case ACTIVITY_EDIT:
             {
                 // TODO: Need new way
                 //Bundle extras = data.getExtras();
                 //Item item = extras.getParcelable("item");
                 //if(item != null){
                 //    currentItem = item;
                 //    updateFlashcardView(false);
                 //    queueManager.updateQueueItem(currentItem);
                 //}
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
     public Dialog onCreateDialog(int id){
         switch(id){
             case DIALOG_LOADING_PROGRESS:{
                 ProgressDialog progressDialog = new ProgressDialog(MemoScreen.this);
                 progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                 progressDialog.setTitle(getString(R.string.loading_please_wait));
                 progressDialog.setMessage(getString(R.string.loading_database));
                 progressDialog.setCancelable(false);
 
                 return progressDialog;
             }
             default:
                 return super.onCreateDialog(id);
 
         }
     }
 
     @Override
     public boolean onKeyDown(int keyCode, KeyEvent event){
         if(option.getVolumeKeyShortcut()){
             if(keyCode == KeyEvent.KEYCODE_VOLUME_UP){
                 return true;
             }
             else if(keyCode == KeyEvent.KEYCODE_VOLUME_DOWN){
                 return true;
             }
         }
         if ((keyCode == KeyEvent.KEYCODE_BACK)) {
             Log.v(TAG, "back button pressed");
             WaitDbTaskFinish task = new WaitDbTaskFinish();
             task.execute((Void)null);
         }
 
         return super.onKeyDown(keyCode, event);
     }
     @Override
     public boolean onKeyUp(int keyCode, KeyEvent event){
         /* Short press to scroe the card */
 
         if(option.getVolumeKeyShortcut()){
             if(keyCode == KeyEvent.KEYCODE_VOLUME_UP){
                 if(flashcardDisplay.isAnswerShown() == false){
                     updateFlashcardView(true);
                     showButtons();
                 }
                 else{
                     /* Grade 0 for up key */
                     getGradeButtonListener(0).onClick(null);
                     Toast.makeText(this, getString(R.string.grade_text) + " 0", Toast.LENGTH_SHORT).show();
                 }
                 return true;
             }
             if(keyCode == KeyEvent.KEYCODE_VOLUME_DOWN){
                 if(flashcardDisplay.isAnswerShown() == false){
                     updateFlashcardView(true);
                 }
                 else{
                     /* Grade 3 for down key */
                     getGradeButtonListener(3).onClick(null);
                     Toast.makeText(this, getString(R.string.grade_text) + " 3", Toast.LENGTH_SHORT).show();
                 }
                 return true;
             }
         }
         return super.onKeyUp(keyCode, event);
     }
 
     void updateFlashcardView(boolean showAnswer){
         if(currentCard == null){
             Log.e(TAG, "current card is null in updateFlashcardView");
             return;
         }
         flashcardDisplay.updateView(currentCard, showAnswer);
         /* Also update the visibility of buttons */
         if(showAnswer){
             showButtons();
         }
         else{
             hideButtons();
         }
         autoSpeak();
         if(!buttonDisabled){
             setGradeButtonTitle();
             setGradeButtonListeners();
         }
         /* Automatic copy the current question to clipboard */
         if(option.getCopyClipboard()){
             ClipboardManager cm = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
             cm.setText(currentCard.getQuestion());
         }
     }
 
 
     void setViewListeners(){
         View.OnClickListener showAnswerListener = new View.OnClickListener(){
             public void onClick(View v){
                 if(currentCard!= null){
                     /* Double sided card, the click will toggle question and answer */
                     if(setting.getCardStyle() == Setting.CardStyle.DOUBLE_SIDED){
                         if(flashcardDisplay.isAnswerShown()){
                             updateFlashcardView(false);
                             hideButtons();
                         }
                         else{
                             updateFlashcardView(true);
                             showButtons();
                         }
 
                     }
                     else{
                         /* For single sided card */
                         updateFlashcardView(true);
                         showButtons();
                     }
                 }
             }
         };
         View.OnClickListener speakQuestionListener = new View.OnClickListener(){
             public void onClick(View v){
                 if(currentCard != null && questionTTS != null){
                     questionTTS.sayText(currentCard.getQuestion());
                 }
             }
         };
         View.OnClickListener speakAnswerListener = new View.OnClickListener(){
             public void onClick(View v){
                 if (currentCard!= null) {
                     if(!flashcardDisplay.isAnswerShown()){
                         updateFlashcardView(true);
                         showButtons();
                     }
                     else{
                         if(answerTTS != null){
                             answerTTS.sayText(currentCard.getAnswer());
                         }
                     }
                 }
             }
         };
         View.OnLongClickListener openContextMenuListener = new View.OnLongClickListener(){
             public boolean onLongClick(View v){
                 MemoScreen.this.openContextMenu(flashcardDisplay.getView());
                 /* To determine which view is long clicked */
                 activeView = v;
                 Log.v(TAG, "Open Menu!");
                 return true;
             }
         };
         flashcardDisplay.setQuestionLayoutLongClickListener(openContextMenuListener);
         flashcardDisplay.setAnswerLayoutLongClickListener(openContextMenuListener);
 
         flashcardDisplay.setQuestionLayoutClickListener(showAnswerListener);
         flashcardDisplay.setAnswerLayoutClickListener(showAnswerListener);
         if(setting.getSpeakingType() == Setting.SpeakingType.TAP || setting.getSpeakingType() == Setting.SpeakingType.AUTOTAP){
             flashcardDisplay.setQuestionTextClickListener(speakQuestionListener);
             flashcardDisplay.setAnswerTextClickListener(speakAnswerListener);
         }
         else{
             flashcardDisplay.setQuestionTextClickListener(showAnswerListener);
             flashcardDisplay.setAnswerTextClickListener(showAnswerListener);
         }
         LinearLayout memoRoot = (LinearLayout)findViewById(R.id.memo_screen_root);
         memoRoot.setOnClickListener(showAnswerListener);
     }
 
     void setGradeButtonTitle() {
         Map<String, Button> hm = controlButtons.getButtons();
         if(option.getButtonStyle() == Option.ButtonStyle.MNEMOSYNE) {
             hm.get("0").setText(getString(R.string.memo_btn0_brief_text));
             hm.get("1").setText(getString(R.string.memo_btn1_brief_text));
             hm.get("2").setText(getString(R.string.memo_btn2_brief_text));
             hm.get("3").setText(getString(R.string.memo_btn3_brief_text));
             hm.get("4").setText(getString(R.string.memo_btn4_brief_text));
             hm.get("5").setText(getString(R.string.memo_btn5_brief_text));
         } else if(option.getButtonStyle() == Option.ButtonStyle.ANKI) {
             hm.get("0").setText(getString(R.string.memo_btn0_anki_text) + "\n+" + scheduler.getInterval(currentCard.getLearningData(), 0));
             hm.get("1").setText(getString(R.string.memo_btn1_anki_text) + "\n+" + scheduler.getInterval(currentCard.getLearningData(), 1));
             hm.get("2").setText(getString(R.string.memo_btn2_anki_text) + "\n+" + scheduler.getInterval(currentCard.getLearningData(), 2));
             hm.get("3").setText(getString(R.string.memo_btn3_anki_text) + "\n+" + scheduler.getInterval(currentCard.getLearningData(), 3));
             hm.get("4").setText(getString(R.string.memo_btn4_anki_text) + "\n+" + scheduler.getInterval(currentCard.getLearningData(), 4));
             hm.get("5").setText(getString(R.string.memo_btn5_anki_text) + "\n+" + scheduler.getInterval(currentCard.getLearningData(), 5));
         } else {
             hm.get("0").setText(getString(R.string.memo_btn0_text) + "\n+" + scheduler.getInterval(currentCard.getLearningData(), 0));
             hm.get("1").setText(getString(R.string.memo_btn1_text) + "\n+" + scheduler.getInterval(currentCard.getLearningData(), 1));
             hm.get("2").setText(getString(R.string.memo_btn2_text) + "\n+" + scheduler.getInterval(currentCard.getLearningData(), 2));
             hm.get("3").setText(getString(R.string.memo_btn3_text) + "\n+" + scheduler.getInterval(currentCard.getLearningData(), 3));
             hm.get("4").setText(getString(R.string.memo_btn4_text) + "\n+" + scheduler.getInterval(currentCard.getLearningData(), 4));
             hm.get("5").setText(getString(R.string.memo_btn5_text) + "\n+" + scheduler.getInterval(currentCard.getLearningData(), 5));
         }
     }
 
     void setGradeButtonListeners(){
         Map<String, Button> hm = controlButtons.getButtons();
         for(int i = 0; i < 6; i++){
             Button b = hm.get(Integer.valueOf(i).toString());
             b.setOnClickListener(getGradeButtonListener(i));
             b.setOnLongClickListener(getGradeButtonLongClickListener(i));
         }
     }
 
     String getActivityTitleString(){
         StringBuilder sb = new StringBuilder();
         sb.append(getString(R.string.new_text) + ": " + newCardCount + " ");
         sb.append(getString(R.string.review_short_text) + ": " + schedluledCardCount + " ");
         sb.append(getString(R.string.id_text) + ": " + currentCard.getId() + " ");
         sb.append(currentCard.getCategory().getName());
         return sb.toString();
     }
 
 
     private View.OnClickListener getGradeButtonListener(final int grade){
         return new View.OnClickListener(){
             public void onClick(View v){
                 // TODO: Undo need to rework.
                 gradeTask = new GradeTask();
                 gradeTask.execute(grade);
                 if(questionTTS != null){
                     questionTTS.stop();
                 }
                 if(answerTTS != null){
                     answerTTS.stop();
                 }
             }
         };
     }
 
     private View.OnLongClickListener getGradeButtonLongClickListener(final int grade){
         return new View.OnLongClickListener(){
             public boolean onLongClick(View v){
                 String[] helpText = {getString(R.string.memo_btn0_help_text),getString(R.string.memo_btn1_help_text),getString(R.string.memo_btn2_help_text),getString(R.string.memo_btn3_help_text),getString(R.string.memo_btn4_help_text),getString(R.string.memo_btn5_help_text)};
                 Toast.makeText(MemoScreen.this, helpText[grade], Toast.LENGTH_SHORT).show();
                 return true;
             }
         };
     }
 
     private void hideButtons(){
         /* Does it take the place holder space? */
         if(setting.getCardStyle() == Setting.CardStyle.DOUBLE_SIDED){
             controlButtons.getView().setVisibility(View.GONE);
         }
         else{
             controlButtons.getView().setVisibility(View.INVISIBLE);
         }
 
     }
 
     private void showButtons(){
         if(!buttonDisabled){
             controlButtons.getView().setVisibility(View.VISIBLE);
         }
     }
 
 
     private void composeViews(){
         LinearLayout memoRoot = (LinearLayout)findViewById(R.id.memo_screen_root);
 
         LinearLayout flashcardDisplayView = (LinearLayout)flashcardDisplay.getView();
         LinearLayout controlButtonsView = (LinearLayout)controlButtons.getView();
         /* This li is make the background of buttons the same as answer */
         LinearLayout li = new LinearLayout(this);
         li.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.FILL_PARENT));
         Integer color = setting.getAnswerBackgroundColor();
         if(color != null){
             li.setBackgroundColor(color);
         }
 
         /* 
          * -1: Match parent -2: Wrap content
          * This is necessary or the view will not be 
          * stetched
          */
         memoRoot.addView(flashcardDisplayView, -1, -1);
         li.addView(controlButtonsView, -1, -2);
         memoRoot.addView(li, -1, -2);
         flashcardDisplayView.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, 1.0f));
     }
 
     @Override
     public void restartActivity(){
         Intent myIntent = new Intent(this, MemoScreen.class);
         myIntent.putExtra("dbname", dbName);
         myIntent.putExtra("dbpath", dbPath);
         myIntent.putExtra("category", activeCategory);
 
         finish();
         startActivity(myIntent);
     }
 
     void showNoItemDialog(){
         new AlertDialog.Builder(this)
             .setTitle(this.getString(R.string.memo_no_item_title))
             .setMessage(this.getString(R.string.memo_no_item_message))
             .setNeutralButton(getString(R.string.back_menu_text), new DialogInterface.OnClickListener() {
                 @Override
                 public void onClick(DialogInterface arg0, int arg1) {
                     /* Finish the current activity and go back to the last activity.
                      * It should be the open screen. */
                     finish();
                     }
                 })
             .setNegativeButton(getString(R.string.learn_ahead), new OnClickListener(){
                 public void onClick(DialogInterface arg0, int arg1) {
                     finish();
                     Intent myIntent = new Intent();
                     // TODO: Cram
                     //myIntent.setClass(MemoScreen.this, CramMemoScreen.class);
                     //myIntent.putExtra("dbname", dbName);
                     //myIntent.putExtra("dbpath", dbPath);
                     //startActivity(myIntent);
                 }
             })
             .setOnCancelListener(new DialogInterface.OnCancelListener(){
                 public void onCancel(DialogInterface dialog){
                     finish();
                     }
                 })
             .create()
             .show();
     }
 
     void autoSpeak(){
         if (currentCard != null) {
 
             if(setting.getSpeakingType() == Setting.SpeakingType.AUTOTAP || setting.getSpeakingType() == Setting.SpeakingType.AUTO){
                 if(!flashcardDisplay.isAnswerShown()){
                     if(questionTTS != null){
                         questionTTS.sayText(currentCard.getQuestion());
                     }
                 }
                 else{
                     if(answerTTS != null){
                         answerTTS.sayText(currentCard.getAnswer());
                     }
                 }
             }
         }
     }
 
     void refreshStatInfo() {
        newCardCount = learningDataDao.getNewCardCount();
        schedluledCardCount = learningDataDao.getScheduledCardCount();
     }
 
     private void initTTS(){
         String defaultLocation =
             Environment.getExternalStorageDirectory().getAbsolutePath()
             + getString(R.string.default_audio_dir);
         // TODO: This couldn't be null but be wary
         String qa = setting.getQuestionAudio();
         String aa = setting.getAnswerAudio();
 
         // TODO: Pay attention to null pointer
         if(!setting.getQuestionAudioLocation().equals("")){
             questionTTS = new AudioFileTTS(defaultLocation, dbName);
         }
         else if(qa != null && !qa.equals("")){
             questionTTS = new AnyMemoTTSPlatform(this, new Locale(qa));
         }
         else{
             questionTTS = null;
         }
 
         // TODO: Pay attention to null pointer
         if(!setting.getAnswerAudioLocation().equals("")){
             answerTTS = new AudioFileTTS(defaultLocation, dbName);
         }
         else if(qa != null && !qa.equals("")){
             answerTTS = new AnyMemoTTSPlatform(this, new Locale(aa));
         }
         else{
             answerTTS = null;
         }
     }
 
     private class InitTask extends AsyncTask<Void, Void, Card> {
 
 		@Override
         public void onPreExecute() {
             requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
 
             setContentView(R.layout.memo_screen_layout);
 
             Bundle extras = getIntent().getExtras();
             mHandler = new Handler();
             if (extras != null) {
                 dbPath = extras.getString(EXTRA_DBPATH);
                 activeCategory = extras.getString(EXTRA_CATEGORY);
             }
             option = new Option(MemoScreen.this);
 
             scheduler = new DefaultScheduler();
             // Strip leading path!
             dbName = AMUtil.getFilenameFromPath(dbPath);
             showDialog(DIALOG_LOADING_PROGRESS);
         }
 
         @Override
         public Card doInBackground(Void... params) {
             try {
                 AnyMemoDBOpenHelper helper =
                     AnyMemoDBOpenHelperManager.getHelper(MemoScreen.this, dbPath);
                 
                 cardDao = helper.getCardDao();
                 learningDataDao = helper.getLearningDataDao();
                 settingDao = helper.getSettingDao();
                 setting = settingDao.queryForId(1);
                 /* Run the learnQueue init in a separate thread */
                 createQueue();
                 return queueManager.dequeue();
             } catch (Exception e) {
                 return null;
             }
         }
         
         @Override
         public void onCancelled(){
             return;
         }
 
         @Override
         public void onPostExecute(Card result){
             flashcardDisplay = new SingleSidedCardDisplay(MemoScreen.this, dbName, setting, option);
             if (option.getButtonStyle() == Option.ButtonStyle.ANKI) {
                 controlButtons = new AnkiGradeButtons(MemoScreen.this);
             }
             else if (option.getButtonStyle() == Option.ButtonStyle.MNEMOSYNE){
                 controlButtons = new MnemosyneGradeButtons(MemoScreen.this);
             }
             else{
                 controlButtons = new AnyMemoGradeButtons(MemoScreen.this);
             }
 
             currentCard = result;
             
             initTTS();
             composeViews();
             setViewListeners();
             hideButtons();
             registerForContextMenu(flashcardDisplay.getView());
 
             refreshStatInfo();
             setTitle(getActivityTitleString());
             removeDialog(DIALOG_LOADING_PROGRESS);
             updateFlashcardView(false);
         }
     }
 
     /*
      * Use AsyncTask to update the database and update the statistics
      * information
      */
     private class GradeTask extends AsyncTask<Integer, Void, Card>{
         private boolean isNewCard = false;
 
         @Override
         public void onPreExecute() {
             super.onPreExecute();
             setProgressBarIndeterminateVisibility(true);
             hideButtons();
         }
 
         @Override
         public Card doInBackground(Integer... grades) {
             assert grades.length == 1 : "Grade more than 1 time";
             int grade = grades[0];
             prevCard = currentCard;
             LearningData ld = currentCard.getLearningData();
             if (ld.getAcqReps() == 0) {
                 isNewCard = true;
             }
             scheduler.schedule(ld, grade, true);
             currentCard.setLearningData(ld);
             queueManager.update(currentCard);
             Card nextCard = queueManager.dequeue();
             return nextCard;
         }
 
         @Override
         public void onCancelled() {
             return;
         }
 
         @Override
         public void onPostExecute(Card result){
             super.onPostExecute(result);
             setProgressBarIndeterminateVisibility(false);
             currentCard = result;
             if(currentCard == null){
                 showNoItemDialog();
                 return;
             }
 
             if (isNewCard) {
                 newCardCount -= 1;
                 // TODO: need more generic abstraction
                 if (prevCard.getLearningData().getGrade() < 2) {
                     schedluledCardCount += 1;
                 }
             } else {
                 if (prevCard.getLearningData().getGrade() >= 2) {
                     schedluledCardCount -= 1;
                 }
             }
             updateFlashcardView(false);
 
             setTitle(getActivityTitleString());
         }
     }
 
     /*
      * Use AsyncTask to make sure there is no running task for a db 
      */
     private class WaitDbTask extends AsyncTask<Void, Void, Void>{
         private ProgressDialog progressDialog;
 
         @Override
         public void onPreExecute(){
             super.onPreExecute();
             progressDialog = new ProgressDialog(MemoScreen.this);
             progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
             progressDialog.setTitle(getString(R.string.loading_please_wait));
             progressDialog.setMessage(getString(R.string.loading_database));
             progressDialog.setCancelable(true);
             progressDialog.show();
         }
 
         @Override
         public Void doInBackground(Void... nothing){
             AnyMemoExecutor.waitAllTasks();
             return null;
         }
 
         @Override
         public void onCancelled(){
             return;
         }
 
         @Override
         public void onPostExecute(Void result){
             super.onPostExecute(result);
             if (!isCancelled()) {
                 progressDialog.dismiss();
             }
         }
     }
 
     /*
      * Like the wait db task but finish the current activity
      */
     private class WaitDbTaskFinish extends WaitDbTask {
         @Override
         public void onPostExecute(Void result){
             super.onPostExecute(result);
             finish();
         }
     }
 
     Runnable flushDatabaseTask = new Runnable() {
         public void run() {
             queueManager.flush();
         }
     };
 
 }
