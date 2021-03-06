 package com.erakk.lnreader.activity;
 
 import java.util.ArrayList;
 
 import android.annotation.SuppressLint;
 import android.app.AlertDialog;
 import android.app.ListActivity;
 import android.app.ProgressDialog;
 import android.content.DialogInterface;
 import android.content.Intent;
 import android.os.AsyncTask;
 import android.os.Build;
 import android.os.Bundle;
 import android.preference.PreferenceManager;
 import android.util.Log;
 import android.view.ContextMenu;
 import android.view.ContextMenu.ContextMenuInfo;
 import android.view.Gravity;
 import android.view.LayoutInflater;
 import android.view.Menu;
 import android.view.MenuInflater;
 import android.view.MenuItem;
 import android.view.View;
 import android.widget.AdapterView.AdapterContextMenuInfo;
 import android.widget.EditText;
 import android.widget.ListView;
 import android.widget.TextView;
 import android.widget.Toast;
 
 import com.erakk.lnreader.Constants;
 import com.erakk.lnreader.LNReaderApplication;
 import com.erakk.lnreader.R;
 import com.erakk.lnreader.UIHelper;
 import com.erakk.lnreader.adapter.PageModelAdapter;
 import com.erakk.lnreader.callback.CallbackEventData;
 import com.erakk.lnreader.callback.ICallbackEventData;
 import com.erakk.lnreader.dao.NovelsDao;
 import com.erakk.lnreader.helper.AsyncTaskResult;
 import com.erakk.lnreader.model.NovelCollectionModel;
 import com.erakk.lnreader.model.PageModel;
 import com.erakk.lnreader.task.AddNovelTask;
 import com.erakk.lnreader.task.DownloadNovelDetailsTask;
 import com.erakk.lnreader.task.IAsyncTaskOwner;
 import com.erakk.lnreader.task.LoadNovelsTask;
 
 /*
  * Author: Nandaka
  * Copy from: NovelsActivity.java
  */
 
 public class DisplayLightNovelListActivity extends ListActivity implements IAsyncTaskOwner{
 	private static final String TAG = DisplayLightNovelListActivity.class.toString();
 	private ArrayList<PageModel> listItems = new ArrayList<PageModel>();
 	private PageModelAdapter adapter;
 	private LoadNovelsTask task = null;
 	private DownloadNovelDetailsTask downloadTask = null;
 	private AddNovelTask addTask = null;
 	private ProgressDialog dialog;
 	private boolean isInverted;
 	private boolean onlyWatched = false;
 	
 	@Override
 	public void onCreate(Bundle savedInstanceState) {
 		super.onCreate(savedInstanceState);
 		UIHelper.SetTheme(this, R.layout.activity_display_light_novel_list);
 		UIHelper.SetActionBarDisplayHomeAsUp(this, true);
 		
 		registerForContextMenu(getListView());
 		onlyWatched = getIntent().getBooleanExtra(Constants.EXTRA_ONLY_WATCHED, false);
 
 		//Encapsulated in updateContent
 		updateContent(false, onlyWatched);
 		
 		if(onlyWatched){
 			setTitle("Watched Light Novels");
 		}
 		else {
 			setTitle("Light Novels");
 		}
 		registerForContextMenu(getListView());
 		isInverted = getColorPreferences();
 	}
 
 	@Override
 	protected void onListItemClick(ListView l, View v, int position, long id) {
 		super.onListItemClick(l, v, position, id);
 		// Get the item that was clicked
 		PageModel o = adapter.getItem(position);
 		String novel = o.toString();
 		//Create new intent
 		Intent intent = new Intent(this, DisplayLightNovelDetailsActivity.class);
 		intent.putExtra(Constants.EXTRA_NOVEL, novel);
 		intent.putExtra(Constants.EXTRA_PAGE, o.getPage());
 		intent.putExtra(Constants.EXTRA_TITLE, o.getTitle());
 		intent.putExtra(Constants.EXTRA_ONLY_WATCHED, getIntent().getBooleanExtra(Constants.EXTRA_ONLY_WATCHED, false));
 		startActivity(intent);
 		Log.d("DisplayLightNovelsActivity", o.getPage() + " (" + o.getTitle() + ")");
 	}
 
 	@Override
 	public boolean onCreateOptionsMenu(Menu menu) {
 		getMenuInflater().inflate(R.menu.activity_display_light_novel_list, menu);
 		return true;
 	}
 	
 	@Override
 	protected void onStop() {
 		// cancel running task
 		// disable cancel so the task can run in background
 //		if(task != null) {
 //			if(!(task.getStatus() == Status.FINISHED)) {
 //				task.cancel(true);
 //				Log.d(TAG, "Stopping running task.");
 //			}
 //		}
 //		if(downloadTask != null) {
 //			if(!(downloadTask.getStatus() == Status.FINISHED)) {
 //				downloadTask.cancel(true);
 //				Log.d(TAG, "Stopping running download task.");
 //			}
 //		}
 		super.onStop();
 	}
 	
 	@Override
     protected void onRestart() {
         super.onRestart();
         if(isInverted != getColorPreferences()) {
         	UIHelper.Recreate(this);
         }
         if(adapter != null) adapter.notifyDataSetChanged();
     }
 
 	@Override
 	public boolean onOptionsItemSelected(MenuItem item) {
 		switch (item.getItemId()) {
 		case R.id.menu_settings:
 			Intent launchNewIntent = new Intent(this, DisplaySettingsActivity.class);
 			startActivity(launchNewIntent);
 			return true;
 		case R.id.menu_refresh_novel_list:			
 			/*
 			 * Implement code to refresh novel list
 			 */
 			boolean onlyWatched = getIntent().getBooleanExtra(Constants.EXTRA_ONLY_WATCHED, false);
 			updateContent(true, onlyWatched);			
 			Toast.makeText(getApplicationContext(), "Refreshing", Toast.LENGTH_SHORT).show();
 			return true;
 		case R.id.invert_colors:			
 			UIHelper.ToggleColorPref(this);
 			UIHelper.Recreate(this);
 			return true;
 		case R.id.menu_manual_add:			
 			ManualAdd();
 			return true;
 		case android.R.id.home:
 			super.onBackPressed();
 			return true;
 		}
 		return super.onOptionsItemSelected(item);
 	}
 
 	private void ManualAdd() {
 		AlertDialog.Builder alert = new AlertDialog.Builder(this);
 		alert.setTitle("Add Novel");
 		//alert.setMessage("Message");
 		LayoutInflater factory = LayoutInflater.from(this);
 		View inputView = factory.inflate(R.layout.layout_add_new_novel, null);
 		final EditText inputName = (EditText) inputView.findViewById(R.id.page);
 		final EditText inputTitle = (EditText) inputView.findViewById(R.id.title);
 		alert.setView(inputView);
 		alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
 			public void onClick(DialogInterface dialog, int whichButton) {
 				if(whichButton == DialogInterface.BUTTON_POSITIVE) {
 					HandleOK(inputName, inputTitle);
 				}
 			}
 		});
 		alert.setNegativeButton("Cancel", null);
 		alert.show();
 	}
 	
 	private void HandleOK(EditText input, EditText inputTitle) {
 		String novel = input.getText().toString();
 		String title = inputTitle.getText().toString();
 		if(novel != null && novel.length() > 0 && inputTitle != null && inputTitle.length() > 0) {
 			PageModel temp = new PageModel();
 			temp.setPage(novel);
 			temp.setTitle(title);
 			temp.setType(PageModel.TYPE_NOVEL);
 			temp.setParent("Main_Page");
 			executeAddTask(temp);
 		}
 		else {
 			Toast.makeText(this, "Empty Input", Toast.LENGTH_LONG).show();
 		}
 	}  
 
 	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
 		super.onCreateContextMenu(menu, v, menuInfo);
 		MenuInflater inflater = getMenuInflater();
 		inflater.inflate(R.menu.novel_context_menu, menu);
 	}
 
 	@Override
 	public boolean onContextItemSelected(MenuItem item) {
 		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
 		switch(item.getItemId()) {
 		case R.id.add_to_watch:			
 			/*
 			 * Implement code to toggle watch of this novel
 			 */
			if(info.position > -1) {
				PageModel novel = listItems.get(info.position);
		        if (novel.isWatched()) {
		        	novel.setWatched(false);
		        	Toast.makeText(this, "Removed from watch list: " + novel.getTitle(),	Toast.LENGTH_SHORT).show();
		        }
		        else {
		        	novel.setWatched(true);
		        	Toast.makeText(this, "Added to watch list: " + novel.getTitle(),	Toast.LENGTH_SHORT).show();
		        }
		        NovelsDao.getInstance(this).updatePageModel(novel);
		        adapter.notifyDataSetChanged();
			}
 			return true;
 		case R.id.download_novel:			
 			/*
 			 * Implement code to download novel synopsis
 			 */
 			if(info.position > -1) {
 				toggleProgressBar(true);
 				PageModel novel = listItems.get(info.position);
 				executeDownloadTask(novel);
 			}
 			return true;
 		case R.id.delete_novel:
 			if(info.position > -1) {
 				toggleProgressBar(true);
 				PageModel novel = listItems.get(info.position);
 				boolean result = NovelsDao.getInstance(this).deleteNovel(novel);
 				if(result) {
 					listItems.remove(novel);
 					adapter.notifyDataSetChanged();
 				}				
 				toggleProgressBar(false);
 			}
 			return true;
 		default:
 			return super.onContextItemSelected(item);
 		}
 	}
 	
 	private void updateContent (boolean isRefresh, boolean onlyWatched) {
 		try {
 			// Check size
 			int resourceId = R.layout.novel_list_item;
 			if(UIHelper.IsSmallScreen(this)) {
 				resourceId = R.layout.novel_list_item_small; 
 			}
 			if (adapter != null) {
 				adapter.setResourceId(resourceId);
 			} else {
 				adapter = new PageModelAdapter(this, resourceId, listItems);
 			}
 			boolean alphOrder = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_ALPH_ORDER, false);
 			executeTask(isRefresh, onlyWatched, alphOrder);
 			setListAdapter(adapter);
 		} catch (Exception e) {
 			Log.e(TAG, e.getMessage(), e);
 			Toast.makeText(this, "Error when updating: " + e.getMessage(), Toast.LENGTH_LONG).show();
 		}
 	}
 	
 	@SuppressLint("NewApi")
 	private void executeTask(boolean isRefresh, boolean onlyWatched, boolean alphOrder) {
 		task = new LoadNovelsTask(this, isRefresh, onlyWatched, alphOrder);
 		String key = TAG + ":Main+Page";
 		boolean isAdded = LNReaderApplication.getInstance().addTask(key, task);
 		if(isAdded) {
 			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
 				task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
 			else
 				task.execute();
 		}
 		else {
 			Log.i(TAG, "Continue execute task: " + key);
 			LoadNovelsTask tempTask = (LoadNovelsTask) LNReaderApplication.getInstance().getTask(key);
 			if(tempTask != null) {
 				task = tempTask;
 				task.owner = this;
 			}
 			toggleProgressBar(true);
 		}
 	}
 	
 	@SuppressLint("NewApi")
 	private void executeDownloadTask(PageModel novel) {
 		downloadTask = new DownloadNovelDetailsTask(this);
 		String key = DisplayLightNovelDetailsActivity.TAG + ":" + novel.getPage();
 		boolean isAdded = LNReaderApplication.getInstance().addTask(key, task);
 		if(isAdded) {
 			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
 				downloadTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new PageModel[] {novel});
 			else
 				downloadTask.execute(new PageModel[] {novel});
 		}
 		else {
 			Log.i(TAG, "Continue download task: " + key);
 			DownloadNovelDetailsTask tempTask = (DownloadNovelDetailsTask) LNReaderApplication.getInstance().getTask(key);
 			if(tempTask != null) {
 				downloadTask = tempTask;
 				downloadTask.owner = this;
 			}
 			toggleProgressBar(true);
 		}
 	}
 	
 	@SuppressLint("NewApi")
 	private void executeAddTask(PageModel novel) {
 		addTask = new AddNovelTask(this);
 		String key = DisplayLightNovelDetailsActivity.TAG + ":Add:" + novel.getPage();
 		boolean isAdded = LNReaderApplication.getInstance().addTask(key, task);
 		if(isAdded) {
 			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
 				addTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new PageModel[] {novel});
 			else
 				addTask.execute(new PageModel[] {novel});
 		}
 		else {
 			Log.i(TAG, "Continue Add task: " + key);
 			AddNovelTask tempTask = (AddNovelTask) LNReaderApplication.getInstance().getTask(key);
 			if(tempTask != null) {
 				addTask = tempTask;
 				addTask.owner = this;
 			}
 			toggleProgressBar(true);
 		}
 	}
 	
 	public void toggleProgressBar(boolean show) {
 		if(show) {
 			dialog = ProgressDialog.show(this, "Novel List", "Loading. Please wait...", true);
 			dialog.getWindow().setGravity(Gravity.CENTER);
 			dialog.setCanceledOnTouchOutside(true);
 		}
 		else {
 			dialog.dismiss();
 		}
 	}
 
 	public void setMessageDialog(ICallbackEventData message) {
 		if(dialog.isShowing())
 			dialog.setMessage(message.getMessage());		
 	}
 
 	public void getResult(AsyncTaskResult<?> result) {
 		Exception e = result.getError();
 		if(e == null) {
 			// from LoadNovelsTask
 			if(result.getResult() instanceof ArrayList<?>) {
 				@SuppressWarnings("unchecked")
 				ArrayList<PageModel> list = (ArrayList<PageModel>) result.getResult();
 				if(list != null) {
 					//if (refreshOnly) {
 						adapter.clear();
 					//	refreshOnly = false;
 					//}
 					
 					adapter.addAll(list);
 					toggleProgressBar(false);
 
 					// Show message if watch list is empty
 					if (list.size() == 0 && onlyWatched) {
 						TextView tv = (TextView) findViewById(R.id.emptyList);
 						tv.setVisibility(TextView.VISIBLE);
 						tv.setText("Watch List is empty.");
 					}
 				}
 			}
 			// from DownloadNovelDetailsTask
 			else if(result.getResult() instanceof NovelCollectionModel) {
 				setMessageDialog(new CallbackEventData("Download complete."));
 				NovelCollectionModel novelCol = (NovelCollectionModel) result.getResult();
 				try {
 					PageModel page = novelCol.getPageModel();
 					boolean found = false;
 					for (PageModel temp : adapter.data) {
 						if(temp.getPage().equalsIgnoreCase(page.getPage())) {
 							found = true;
 							break;
 						}
 					}
 					if(!found) {
 						adapter.data.add(page);
 					}
 				} catch (Exception e1) {
 					Log.e(TAG, e1.getClass().toString() + ": " + e1.getMessage(), e1);
 				}
 				
 				adapter.notifyDataSetChanged();
 				toggleProgressBar(false);
 			}
 		}
 		else {
 			Log.e(TAG, e.getClass().toString() + ": " + e.getMessage(), e);
 			Toast.makeText(getApplicationContext(), e.getClass().toString() + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
 		}		
 	}
 	
 	private boolean getColorPreferences(){
     	return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_INVERT_COLOR, true);
 	}
 }
 
