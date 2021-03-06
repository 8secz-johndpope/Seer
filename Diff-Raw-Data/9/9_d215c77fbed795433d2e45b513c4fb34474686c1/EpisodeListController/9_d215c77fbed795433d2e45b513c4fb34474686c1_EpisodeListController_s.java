 /*
  *      Copyright (C) 2005-2009 Team XBMC
  *      http://xbmc.org
  *
  *  This Program is free software; you can redistribute it and/or modify
  *  it under the terms of the GNU General Public License as published by
  *  the Free Software Foundation; either version 2, or (at your option)
  *  any later version.
  *
  *  This Program is distributed in the hope that it will be useful,
  *  but WITHOUT ANY WARRANTY; without even the implied warranty of
  *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  *  GNU General Public License for more details.
  *
  *  You should have received a copy of the GNU General Public License
  *  along with XBMC Remote; see the file license.  If not, write to
  *  the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
  *  http://www.gnu.org/copyleft/gpl.html
  *
  */
 
 package org.xbmc.android.remote.presentation.controller;
 
 import java.util.ArrayList;
 
 import org.xbmc.android.remote.R;
 import org.xbmc.android.remote.business.ManagerFactory;
 import org.xbmc.android.remote.presentation.activity.AbsListActivity;
 import org.xbmc.android.remote.presentation.activity.MovieDetailsActivity;
 import org.xbmc.android.remote.presentation.activity.NowPlayingActivity;
 import org.xbmc.android.remote.presentation.widget.FiveLabelsItemView;
 import org.xbmc.android.remote.presentation.widget.FlexibleItemView;
 import org.xbmc.android.util.ImportUtilities;
 import org.xbmc.api.business.DataResponse;
 import org.xbmc.api.business.IControlManager;
 import org.xbmc.api.business.ITvShowManager;
 import org.xbmc.api.object.Episode;
 import org.xbmc.api.object.Movie;
 import org.xbmc.api.object.Season;
 import org.xbmc.api.object.TvShow;
 
 import android.app.Activity;
 import android.app.AlertDialog;
 import android.content.DialogInterface;
 import android.content.Intent;
 import android.graphics.BitmapFactory;
 import android.view.ContextMenu;
 import android.view.Menu;
 import android.view.MenuItem;
 import android.view.SubMenu;
 import android.view.View;
 import android.view.ViewGroup;
 import android.view.ContextMenu.ContextMenuInfo;
 import android.widget.AbsListView;
 import android.widget.AdapterView;
 import android.widget.ArrayAdapter;
 import android.widget.Toast;
 import android.widget.AdapterView.AdapterContextMenuInfo;
 import android.widget.AdapterView.OnItemClickListener;
 
 public class EpisodeListController extends ListController implements IController {
 	
 	public static final int ITEM_CONTEXT_PLAY = 1;
 	public static final int ITEM_CONTEXT_INFO = 2;
 	
 	public static final int MENU_PLAY_ALL = 1;
 	public static final int MENU_SORT = 2;
 	public static final int MENU_SORT_BY_TITLE_ASC = 21;
 	public static final int MENU_SORT_BY_TITLE_DESC = 22;
 	public static final int MENU_SORT_BY_YEAR_ASC = 23;
 	public static final int MENU_SORT_BY_YEAR_DESC = 24;
 	public static final int MENU_SORT_BY_RATING_ASC = 25;
 	public static final int MENU_SORT_BY_RATING_DESC = 26;
 	
 	private Season mSeason;
 	
 	private ITvShowManager mTvManager;
 	private IControlManager mControlManager;
 	
 	private boolean mLoadCovers = false;
 	
 	public void onCreate(Activity activity, AbsListView list) {
 		
 		mTvManager = ManagerFactory.getTvManager(this);
 		mControlManager = ManagerFactory.getControlManager(this);
 		
 		final String sdError = ImportUtilities.assertSdCard();
 		mLoadCovers = sdError == null;
 		
 		if (!isCreated()) {
 			super.onCreate(activity, list);
 
 			if (!mLoadCovers) {
 				Toast toast = Toast.makeText(activity, sdError + " Displaying place holders only.", Toast.LENGTH_LONG);
 				toast.show();
 			}
 			
 			mSeason = (Season)activity.getIntent().getSerializableExtra(ListController.EXTRA_SEASON);
 			
 			activity.registerForContextMenu(mList);
 			
 			mFallbackBitmap = BitmapFactory.decodeResource(activity.getResources(), R.drawable.poster);
//			setupIdleListener();
 			
 			mList.setOnItemClickListener(new OnItemClickListener() {
 				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
 					final TvShow show = (TvShow)mList.getAdapter().getItem(((FiveLabelsItemView)view).position);
 					Intent nextActivity = new Intent(view.getContext(), AbsListActivity.class);
 					nextActivity.putExtra(ListController.EXTRA_TVSHOW, show);
 //					nextActivity.putExtra(ListController.EXTRA_LIST_CONTROLLER,)
 					mActivity.startActivity(nextActivity);
 				}
 			});
 			mList.setOnKeyListener(new ListControllerOnKeyListener<Movie>());
 			fetch();
 		}
 	}
 	
 	private void fetch() {
 		final Season season = mSeason;
 		showOnLoading();
 		if (season != null) {
 			setTitle(season.show.title + " Season " + season.number + " - Episodes");
 			mTvManager.getEpisodes(new DataResponse<ArrayList<Episode>>() {
 				public void run() {
 					if(value.size() > 0) {
 						setTitle(season.show.title + " Season " + season.number + " - Episodes (" + value.size() + ")");
 						mList.setAdapter(new EpisodeAdapter(mActivity, value));
 					} else {
 						setNoDataMessage("No seasons found.", R.drawable.icon_movie_dark);
 					}
 				}
 			}, season, mActivity.getApplicationContext());
 		}
 	}
 	
 	/**
 	 * Shows a dialog and refreshes the movie library if user confirmed.
 	 * @param activity
 	 */
 	public void refreshMovieLibrary(final Activity activity) {
 		final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
 		builder.setMessage("Are you sure you want XBMC to rescan your movie library?")
 			.setCancelable(false)
 			.setPositiveButton("Yes!", new DialogInterface.OnClickListener() {
 				public void onClick(DialogInterface dialog, int which) {
 					mControlManager.updateLibrary(new DataResponse<Boolean>() {
 						public void run() {
 							final String message;
 							if (value) {
 								message = "Movie library updated has been launched.";
 							} else {
 								message = "Error launching movie library update.";
 							}
 							Toast toast = Toast.makeText(activity, message, Toast.LENGTH_SHORT);
 							toast.show();
 						}
 					}, "video", mActivity.getApplicationContext());
 				}
 			})
 			.setNegativeButton("Uh, no.", new DialogInterface.OnClickListener() {
 				public void onClick(DialogInterface dialog, int which) {
 					dialog.cancel();
 				}
 			});
 		builder.create().show();
 	}
 	
 	@Override
 	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
 		final FiveLabelsItemView view = (FiveLabelsItemView)((AdapterContextMenuInfo)menuInfo).targetView;
 		menu.setHeaderTitle(view.title);
 		menu.add(0, ITEM_CONTEXT_PLAY, 1, "Play Movie");
 		menu.add(0, ITEM_CONTEXT_INFO, 2, "View Details");
 	}
 	
 	public void onContextItemSelected(MenuItem item) {
 		final Movie movie = (Movie)mList.getAdapter().getItem(((FiveLabelsItemView)((AdapterContextMenuInfo)item.getMenuInfo()).targetView).position);
 		switch (item.getItemId()) {
 			case ITEM_CONTEXT_PLAY:
 				mControlManager.playFile(new DataResponse<Boolean>() {
 					public void run() {
 						if (value) {
 							mActivity.startActivity(new Intent(mActivity, NowPlayingActivity.class));
 						}
 					}
 				}, movie.getPath(), mActivity.getApplicationContext());
 				break;
 			case ITEM_CONTEXT_INFO:
 				Intent nextActivity = new Intent(mActivity, MovieDetailsActivity.class);
 				nextActivity.putExtra(ListController.EXTRA_MOVIE, movie);
 				mActivity.startActivity(nextActivity);
 				break;
 			default:
 				return;
 		}
 	}
 	
 	@Override
 	public void onCreateOptionsMenu(Menu menu) {
 //		if (mActor != null || mGenre != null) {
 //			menu.add(0, MENU_PLAY_ALL, 0, "Play all").setIcon(R.drawable.menu_album);
 //		}
 		SubMenu sortMenu = menu.addSubMenu(0, MENU_SORT, 0, "Sort").setIcon(R.drawable.menu_sort);
 		sortMenu.add(2, MENU_SORT_BY_TITLE_ASC, 0, "by Title ascending");
 		sortMenu.add(2, MENU_SORT_BY_TITLE_DESC, 0, "by Title descending");
 		sortMenu.add(2, MENU_SORT_BY_YEAR_ASC, 0, "by Year ascending");
 		sortMenu.add(2, MENU_SORT_BY_YEAR_DESC, 0, "by Year descending");
 		sortMenu.add(2, MENU_SORT_BY_RATING_ASC, 0, "by Rating ascending");
 		sortMenu.add(2, MENU_SORT_BY_RATING_DESC, 0, "by Rating descending");
 //		menu.add(0, MENU_SWITCH_VIEW, 0, "Switch view").setIcon(R.drawable.menu_view);
 	}
 	
 	@Override
 	public void onOptionsItemSelected(MenuItem item) {
 //		final SharedPreferences.Editor ed;
 		switch (item.getItemId()) {
 		case MENU_PLAY_ALL:
 			break;
 		case MENU_SORT_BY_TITLE_ASC:
 		case MENU_SORT_BY_TITLE_DESC:
 		case MENU_SORT_BY_YEAR_ASC:
 		case MENU_SORT_BY_YEAR_DESC:
 		case MENU_SORT_BY_RATING_ASC:
 		case MENU_SORT_BY_RATING_DESC:
 			break;
 		}
 	}
 	
 	private class EpisodeAdapter extends ArrayAdapter<Episode> {
 		EpisodeAdapter(Activity activity, ArrayList<Episode> items) {
 			super(activity, 0, items);
 		}
 		public View getView(int position, View convertView, ViewGroup parent) {
 
 			final FlexibleItemView view;
 			if (convertView == null) {
 				view = new FlexibleItemView(mActivity, mTvManager, parent.getWidth(), mFallbackBitmap, mList.getSelector());
 			} else {
 				view = (FlexibleItemView)convertView;
 			}
 			
 			final Episode episode = getItem(position);
 			view.reset();
 			view.position = position;
 			view.title = episode.episode + ". " + episode.title;
 //			view.subtitle = episode.;
 			view.subtitleRight = episode.firstAired!=null?episode.firstAired:"";
 //			view.bottomtitle = show.numEpisodes + " episodes";
 			view.bottomright = String.valueOf(((float)Math.round(episode.rating *10))/ 10);
 			
 			if (mLoadCovers) {
 //				view.getResponse().load(season);
 			}
 			return view;
 		}
 	}
 	
 	private static final long serialVersionUID = 1088971882661811256L;
 
 	public void onActivityPause() {
 		if (mTvManager != null) {
 			mTvManager.setController(null);
 //			mVideoManager.postActivity();
 		}
 		if (mControlManager != null) {
 			mControlManager.setController(null);
 		}
 		super.onActivityPause();
 	}
 
 	public void onActivityResume(Activity activity) {
 		super.onActivityResume(activity);
 		if (mTvManager != null) {
 			mTvManager.setController(this);
 		}
 		if (mControlManager != null) {
 			mControlManager.setController(this);
 		}
 	}
 
 }
