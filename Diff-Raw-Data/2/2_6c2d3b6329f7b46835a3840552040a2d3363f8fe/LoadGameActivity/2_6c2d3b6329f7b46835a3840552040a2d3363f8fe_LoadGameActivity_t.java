 package com.nolanlawson.keepscore;
 
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Collections;
 import java.util.Date;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.Iterator;
 import java.util.List;
 import java.util.Map.Entry;
 import java.util.Set;
 import java.util.SortedMap;
 import java.util.TreeMap;
 
 import android.app.AlertDialog;
 import android.app.ListActivity;
 import android.content.DialogInterface;
 import android.content.Intent;
 import android.os.AsyncTask;
 import android.os.Bundle;
 import android.os.Handler;
 import android.os.Looper;
 import android.text.InputType;
 import android.text.TextUtils;
 import android.view.View;
 import android.view.View.OnClickListener;
 import android.view.animation.Animation;
 import android.view.animation.Animation.AnimationListener;
 import android.view.animation.AnimationUtils;
 import android.widget.AdapterView;
 import android.widget.AdapterView.OnItemLongClickListener;
 import android.widget.Button;
 import android.widget.EditText;
 import android.widget.LinearLayout;
 import android.widget.ListView;
 import android.widget.Toast;
 
 import com.nolanlawson.keepscore.data.SavedGameAdapter;
 import com.nolanlawson.keepscore.data.SeparatedListAdapter;
 import com.nolanlawson.keepscore.data.TimePeriod;
 import com.nolanlawson.keepscore.db.Game;
 import com.nolanlawson.keepscore.db.GameDBHelper;
 import com.nolanlawson.keepscore.util.CollectionUtil;
 import com.nolanlawson.keepscore.util.CollectionUtil.Predicate;
 import com.nolanlawson.keepscore.util.StringUtil;
 import com.nolanlawson.keepscore.util.UtilLogger;
 import com.nolanlawson.keepscore.widget.CustomFastScrollView;
 
 public class LoadGameActivity extends ListActivity implements OnItemLongClickListener, OnClickListener {
 
 	private static UtilLogger log = new UtilLogger(LoadGameActivity.class);
 	
 	private SeparatedListAdapter<SavedGameAdapter> adapter;
 	private CustomFastScrollView fastScrollView;
 	private LinearLayout buttonRow;
 	private Button selectAllButton, deselectAllButton, deleteButton;
 	private View spacerView;
 	
 	private Integer lastPosition;
 	private Set<Game> lastChecked;
 	
 	private Handler handler = new Handler(Looper.getMainLooper());
 	
 	@Override
     public void onCreate(Bundle savedInstanceState) {
         super.onCreate(savedInstanceState);
         
         setListAdapter(adapter);
         
         setContentView(R.layout.load_game);
         
         setUpWidgets();
 	}
 	
 	private void setUpWidgets() {
 		getListView().setOnItemLongClickListener(this);
         fastScrollView = (CustomFastScrollView) findViewById(R.id.fast_scroll_view);
         
         buttonRow = (LinearLayout) findViewById(R.id.layout_button_row);
         selectAllButton = (Button) findViewById(android.R.id.button1);
         deselectAllButton = (Button) findViewById(android.R.id.button2);
         deleteButton = (Button) findViewById(android.R.id.button3);
         
         for (Button button : new Button[]{selectAllButton, deselectAllButton, deleteButton}) {
         	button.setOnClickListener(this);
         }
         
         spacerView = findViewById(R.id.view_spacer);
 	}
 
 	@Override
 	protected void onPause() {
 		super.onPause();
 		
 		// save which items were checked and where we are in the list
 		lastChecked = new HashSet<Game>();
 		for (SavedGameAdapter subAdapter : adapter.getSubAdapters()) {
 			lastChecked.addAll(subAdapter.getChecked());
 		}
 		lastPosition = getListView().getFirstVisiblePosition();
 	}
 
 	@Override
 	protected void onResume() {
 		super.onResume();
 		
         List<Game> games = getAllGames();
         Collections.sort(games, Game.byRecentlySaved());
         log.d("loaded games %s", games);
         
         SortedMap<TimePeriod, List<Game>> organizedGames = organizeGamesByTimePeriod(games);
         
         adapter = new SeparatedListAdapter<SavedGameAdapter>(this);
         for (Entry<TimePeriod, List<Game>> entry : organizedGames.entrySet()) {
         	TimePeriod timePeriod = entry.getKey();
         	List<Game> gamesSection = entry.getValue();
         	SavedGameAdapter subAdapter = new SavedGameAdapter(this, gamesSection);
         	if (lastChecked != null) {
         		// reload the checked items from when the user last quit
         		subAdapter.setChecked(lastChecked);
         	}
         	subAdapter.setOnCheckChangedRunnable(new Runnable() {
 				
 				@Override
 				public void run() {
 					showOrHideButtonRow();
 				}
 			});
         	adapter.addSection(getString(timePeriod.getTitleResId()), subAdapter);
         }
         setListAdapter(adapter);
         
         if (lastPosition != null) {
         	// scroll to the user's last position when they quit
         	getListView().setSelection(lastPosition);
         }
         lastPosition = null;
         lastChecked = null;
 	}
 
 
 
 	private List<Game> getAllGames() {
 		GameDBHelper dbHelper = null;
 		try {
 			dbHelper = new GameDBHelper(this);
 			return dbHelper.findAllGames();
 		} finally {
 			if (dbHelper != null) {
 				dbHelper.close();
 			}
 		}
 	}
 
 	@Override
 	public void onListItemClick(ListView l, View v, int position, long id) {
 		super.onListItemClick(l, v, position, id);
 		
 		Game game = (Game) adapter.getItem(position);
 		
 		Intent intent = new Intent(this, GameActivity.class);
 		intent.putExtra(GameActivity.EXTRA_GAME_ID, game.getId());
 		
 		startActivity(intent);
 		
 	}
 	
 	@Override
 	public boolean onItemLongClick(AdapterView<?> adapter, View view, int position, long id) {
 		
 		showOptionsMenu((Game)(this.adapter.getItem(position)));
 		
 		return true;
 	}
 
 	@Override
 	public void onClick(View v) {
 		switch (v.getId()) {
 		case android.R.id.button1: // select all
 			selectAll();
 			break;
 		case android.R.id.button2: // deselect all
 			deselectAll();
 			break;
 		case android.R.id.button3: //delete
 			showDeleteSelectedDialog();
 			break;
 		}
 		
 	}
 
 	private void selectAll() {
 		for (SavedGameAdapter subAdapter : adapter.getSectionsMap().values()) {
 			for (int i = 0; i < subAdapter.getCount(); i++) {
 				Game game = subAdapter.getItem(i);
 				subAdapter.getChecked().add(game);
 			}
 		}
 		adapter.notifyDataSetChanged();
 	}
 	
 	private void deselectAll() {
 		for (SavedGameAdapter subAdapter : adapter.getSectionsMap().values()) {
 			subAdapter.getChecked().clear();
 		}
 		
 		adapter.notifyDataSetChanged();
 		hideButtonRow();
 	}
 
 	private void showDeleteSelectedDialog() {
 		final Set<Game> games = new HashSet<Game>();
 		for (SavedGameAdapter subAdapter : adapter.getSectionsMap().values()) {
 			games.addAll(subAdapter.getChecked());
 		}
 		String message = games.size() == 1
 				?	getString(R.string.text_game_will_be_deleted)
 				:	String.format(getString(R.string.text_games_will_be_deleted), games.size());
 				
 		new AlertDialog.Builder(this)
 			.setCancelable(true)
 			.setTitle(R.string.title_confirm_delete)
 			.setMessage(message)
 			.setNegativeButton(android.R.string.cancel, null)
 			.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
 				
 				@Override
 				public void onClick(DialogInterface dialog, int which) {
 					deleteGames(games);
 				}
 			})
 			.show();
 	}
 
 	private void deleteGames(final Set<Game> games) {
 		
 		
 		// do in background to avoid jankiness
 		new AsyncTask<Void, Void, Void>() {
 			
 			@Override
 			protected Void doInBackground(Void... params) {
 				
 				
 				GameDBHelper dbHelper = null;
 				try {
 					dbHelper = new GameDBHelper(LoadGameActivity.this);
 					dbHelper.deleteGames(games);
 					
 				} finally {
 					if (dbHelper != null) {
 						dbHelper.close();
 					}
 				}
 				return null;
 			}
 
 			@Override
 			protected void onPostExecute(Void result) {
 				super.onPostExecute(result);
 				int toast = games.size() == 1 ? R.string.toast_deleted : R.string.toast_multiple_deleted;
 				Toast.makeText(LoadGameActivity.this, toast, Toast.LENGTH_SHORT).show();
 				for (Game game : games) {
 					onGameDeleted(game);
 				}
 				// clear from the selected sets
 			}
 			
 		}.execute((Void)null);
 	}
 
 	private void showOptionsMenu(final Game game) {
 		
 		String editTitle = getString(TextUtils.isEmpty(game.getName()) 
 				? R.string.title_name_game 
 				: R.string.title_edit_game_name);
 		
 		CharSequence[] options = new CharSequence[]{
 				getString(R.string.text_delete), 
 				getString(R.string.text_copy),
 				getString(R.string.menu_history),
 				editTitle
 				};
 		
 		new AlertDialog.Builder(this)
 			.setCancelable(true)
 			.setItems(options, new DialogInterface.OnClickListener() {
 				
 				@Override
 				public void onClick(DialogInterface dialog, int which) {
 					dialog.dismiss();
 					
 					switch (which) {
 					case 0: // delete
 						showDeleteDialog(game);
 						break;
 					case 1: // copy
 						copyGame(game);
 						break;
 					case 2: // history
 						showHistory(game);
 						break;
 					case 3: // edit name
 						showEditGameNameDialog(game);
 						break;	
 					}
 				}
 			})
 			.show();
 		
 	}
 
 	private void copyGame(Game game) {
 
 		final Game newGame = game.makeCleanCopy();
 		
 		new AsyncTask<Void, Void, Void>() {
 
 			@Override
 			protected Void doInBackground(Void... params) {
 				GameDBHelper dbHelper = null;
 				try {
 					dbHelper = new GameDBHelper(LoadGameActivity.this);
 					dbHelper.saveGame(newGame);
 				} finally {
 					if (dbHelper != null) {
 						dbHelper.close();
 					}
 				}
 				return null;
 			}
 
 			@Override
 			protected void onPostExecute(Void result) {
 				super.onPostExecute(result);
 				onGameCopied(newGame);
 			}
 			
 			
 			
 		}.execute((Void)null);
 	}
 
 	private void onGameCopied(Game newGame) {
 		Toast.makeText(LoadGameActivity.this, R.string.toast_game_copied, Toast.LENGTH_SHORT).show();
 		
 		// if the "recent" group doesn't exist, need to create it
 		String mostRecentSection = getString(TimePeriod.values()[0].getTitleResId());
 		if (!adapter.getSectionName(0).equals(mostRecentSection)) {
 			SavedGameAdapter subAdapter = new SavedGameAdapter(LoadGameActivity.this, 
 					new ArrayList<Game>(Collections.singleton(newGame)));
 			subAdapter.setOnCheckChangedRunnable(new Runnable() {
 				
 				@Override
 				public void run() {
 					showOrHideButtonRow();
 				}
 			});
 			adapter.addSectionToFront(mostRecentSection, subAdapter);
 		} else { // just insert it into the first section
 			((SavedGameAdapter)adapter.getSection(0)).insert(newGame, 0);
 		}
 		adapter.notifyDataSetChanged();
 		adapter.refreshSections();
 		fastScrollView.listItemsChanged();
 		
 	}
 
 	private void showOrHideButtonRow() {
 		
 		boolean shouldShow = CollectionUtil.any(adapter.getSectionsMap().values(), new Predicate<SavedGameAdapter>(){
 
 			@Override
 			public boolean apply(SavedGameAdapter obj) {
 				return !obj.getChecked().isEmpty();
 			}
 		});
 		
 		if (shouldShow) {
 			showButtonRow();
 		} else {
 			hideButtonRow();
 		}
 	}
 	
 	private void showButtonRow() {
 		if (buttonRow.getVisibility() == View.VISIBLE) {
 			// already visible; don't show slide animation
 			return;
 		}
 		
 		handler.post(new Runnable() {
 			
 			@Override
 			public void run() {
 				startShowButtonRowAnimation();
 			}
 		});
 		
 	}
 
 	private void hideButtonRow() {
 		if (buttonRow.getVisibility() == View.GONE) {
 			return; // nothing to do
 		}
 		
 		handler.post(new Runnable() {
 			
 			@Override
 			public void run() {
 				startHideButtonRowAnimation();
 			}
 		});
 		
 	}
 
 	private void startHideButtonRowAnimation() {
 		Animation animation = AnimationUtils.loadAnimation(this, R.anim.slide_top_to_bottom);
 		animation.setAnimationListener(new AnimationListener() {
 			
 			@Override
 			public void onAnimationStart(Animation animation) {
 			}
 			
 			@Override
 			public void onAnimationRepeat(Animation animation) {
 			}
 			
 			@Override
 			public void onAnimationEnd(Animation animation) {
 				buttonRow.setVisibility(View.GONE);
 			}
 		});
 		spacerView.setVisibility(View.GONE);
 		buttonRow.setAnimation(animation);
 		buttonRow.startAnimation(animation);
 	}
 
 	private void startShowButtonRowAnimation() {
 		Animation animation = AnimationUtils.loadAnimation(this, R.anim.slide_bottom_to_top);
 		animation.setAnimationListener(new AnimationListener() {
 			
 			@Override
 			public void onAnimationStart(Animation animation) {
 			}
 			
 			@Override
 			public void onAnimationRepeat(Animation animation) {
 			}
 			
 			@Override
 			public void onAnimationEnd(Animation animation) {
 				// propertly shrink the scroll view
				spacerView.setVisibility(View.VISIBLE);
 			}
 		});
 		buttonRow.setAnimation(animation);
 		buttonRow.setVisibility(View.VISIBLE);
 		
 		buttonRow.startAnimation(animation);
 	}
 
 	private void showHistory(Game game) {
 		
 		Intent intent = new Intent(this, HistoryActivity.class);
 		intent.putExtra(HistoryActivity.EXTRA_GAME, game);
 		
 		startActivity(intent);
 		
 	}
 
 	private void showEditGameNameDialog(final Game game) {
 
 		final EditText editText = new EditText(this);
 		editText.setHint(R.string.hint_game_name);
 		editText.setText(StringUtil.nullToEmpty(game.getName()));
 		editText.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
 		editText.setSingleLine();
 		new AlertDialog.Builder(this)
 			.setTitle(R.string.title_edit_game_name)
 			.setView(editText)
 			.setCancelable(true)
 			.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
 				
 				@Override
 				public void onClick(final DialogInterface dialog, int which) {
 
 					final String newName = StringUtil.nullToEmpty(editText.getText().toString());
 					
 					// update database in the background to avoid jankiness
 					new AsyncTask<Void, Void, Void>(){
 
 						@Override
 						protected Void doInBackground(Void... params) {
 							GameDBHelper dbHelper = null;
 							try {
 								dbHelper = new GameDBHelper(LoadGameActivity.this);
 								dbHelper.updateGameName(game, newName);
 							} finally {
 								if (dbHelper != null) {
 									dbHelper.close();
 								}
 							}
 							return null;
 						}
 
 						@Override
 						protected void onPostExecute(Void result) {
 							super.onPostExecute(result);
 							
 							
 							game.setName(newName.trim());
 							adapter.notifyDataSetChanged();
 							
 							dialog.dismiss();
 						}
 						
 						
 						
 					}.execute((Void)null);
 					
 					
 				}
 			})
 			.setNegativeButton(android.R.string.cancel, null)
 			.show();
 
 		
 	}
 	
 	
 	private void showDeleteDialog(final Game game) {
 		new AlertDialog.Builder(this)
 			.setCancelable(true)
 			.setTitle(R.string.title_confirm_delete)
 			.setMessage(R.string.text_game_will_be_deleted)
 			.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
 				
 				@Override
 				public void onClick(DialogInterface dialog, int which) {
 					dialog.dismiss();
 					deleteGames(Collections.singleton(game));
 				}
 			})
 			.setNegativeButton(android.R.string.cancel, null)
 			.show();
 	}
 	
 	private void onGameDeleted(Game game) {
 		// delete the game from the adapter
 
 		for (Entry<String, SavedGameAdapter> entry : new HashMap<String,SavedGameAdapter>(adapter.getSectionsMap()).entrySet()) {
 			SavedGameAdapter subAdapter = (SavedGameAdapter) entry.getValue();
 			if (subAdapter.getCount() == 1 && subAdapter.getItem(0).equals(game)) {
 				// special case where there's only one item left - don't want the adapter to be left empty
 				// So delete the entire section
 				adapter.removeSection(entry.getKey());
 			} else {
 				subAdapter.remove(game);
 			}
 			subAdapter.getChecked().remove(game); // remove from the checked list
 		}
 		
 		adapter.notifyDataSetChanged();
 		adapter.refreshSections();
 		fastScrollView.listItemsChanged();
 		
 		showOrHideButtonRow();
 	}
 
 
 	private SortedMap<TimePeriod, List<Game>> organizeGamesByTimePeriod(List<Game> games) {
 		SortedMap<TimePeriod, List<Game>> result = new TreeMap<TimePeriod, List<Game>>();
 
 		Iterator<TimePeriod> timePeriodIterator = Arrays.asList(TimePeriod.values()).iterator();
 		TimePeriod timePeriod = timePeriodIterator.next();
 		Date date = new Date();
 		for (Game game : games) {
 			// time periods are sorted from newest to oldest, just like the games.  So we can just walk through
 			// them in order
 			while (!timePeriodMatches(date, timePeriod, game)) {
 				timePeriod = timePeriodIterator.next();
 			}
 			List<Game> existing = result.get(timePeriod);
 			if (existing == null) {
 				result.put(timePeriod, new ArrayList<Game>(Collections.singleton(game)));
 			} else {
 				existing.add(game);
 			}
 		}
 		return result;
 	}
 
 
 	/**
 	 * Return true if the game occurred within this time period.
 	 * @param date
 	 * @param timePeriod
 	 * @param currentGame
 	 * @return
 	 */
 	private boolean timePeriodMatches(Date date, TimePeriod timePeriod, Game currentGame) {
 		Date start = timePeriod.getStartDateFunction().apply(date);
 		Date end = timePeriod.getEndDateFunction().apply(date);
 		
 		return currentGame.getDateSaved() < end.getTime() && currentGame.getDateSaved() >= start.getTime();
 	}
 }
