 package tw.idv.palatis.danboorugallery;
 
 /*
  * This file is part of Danbooru Gallery
  *
  * Copyright 2011
  *   - Victor Tseng <palatis@gmail.com>
  *
  * Danbooru Gallery is free software: you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  *
  * Danbooru Gallery is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with Danbooru Gallery.  If not, see <http://www.gnu.org/licenses/>.
  */
 
 import java.io.File;
 import java.io.IOException;
 import java.util.ArrayList;
 import java.util.List;
 
 import tw.idv.palatis.danboorugallery.defines.D;
 import tw.idv.palatis.danboorugallery.model.Hosts;
 import tw.idv.palatis.danboorugallery.model.Post;
 import tw.idv.palatis.danboorugallery.utils.DanbooruUncaughtExceptionHandler;
 import tw.idv.palatis.danboorugallery.utils.LazyImageAdapter;
 import tw.idv.palatis.danboorugallery.utils.LazyPostFetcher;
 import android.app.Activity;
 import android.app.AlertDialog;
 import android.app.DownloadManager;
 import android.app.SearchManager;
 import android.app.AlertDialog.Builder;
 import android.content.Context;
 import android.content.DialogInterface;
 import android.content.Intent;
 import android.content.SharedPreferences;
 import android.content.DialogInterface.OnClickListener;
 import android.content.SharedPreferences.Editor;
 import android.graphics.Bitmap;
 import android.net.Uri;
 import android.os.Bundle;
 import android.text.InputType;
 import android.util.Log;
 import android.view.Display;
 import android.view.Menu;
 import android.view.MenuInflater;
 import android.view.MenuItem;
 import android.view.View;
 import android.view.WindowManager;
 import android.view.View.OnFocusChangeListener;
 import android.view.animation.Animation;
 import android.view.animation.AnimationUtils;
 import android.widget.AbsListView;
 import android.widget.AdapterView;
 import android.widget.EditText;
 import android.widget.GridView;
 import android.widget.ImageView;
 import android.widget.Toast;
 import android.widget.AbsListView.OnScrollListener;
 import android.widget.AdapterView.OnItemClickListener;
 import android.widget.AdapterView.OnItemLongClickListener;
 import android.widget.ImageView.ScaleType;
 
 public class MainActivity
 	extends Activity
 {
 	static public final int	REQUEST_TAGCLICKED	= 0x01;
 
 	LazyImageAdapter		adapter;
 	LazyPostFetcher			fetcher;
 	SharedPreferences		preferences;
 	DownloadManager			downloader;
 
 	List < Post >			posts;
 	Hosts					hosts;
 
 	@Override
 	public void onCreate(Bundle savedInstanceState)
 	{
 		super.onCreate( savedInstanceState );
 		setContentView( R.layout.main );
 
 		// register a global exception handler here so we don't just quit...
 		Thread.setDefaultUncaughtExceptionHandler( new DanbooruUncaughtExceptionHandler( this ) );
 
 		GalleryItemDisplayer.setActivity( this );
 		preferences = getSharedPreferences( D.SHAREDPREFERENCES_NAME, MODE_PRIVATE );
 		loadPreferences();
 
 		// get the download manager
 		downloader = (DownloadManager) getSystemService( DOWNLOAD_SERVICE );
 
 		// calculate screen size
 		Display display = ((WindowManager) getSystemService( Context.WINDOW_SERVICE )).getDefaultDisplay();
 		int gallery_item_size = Math.min( display.getWidth() / 3, display.getHeight() / 3 );
 		int numcols = display.getWidth() / gallery_item_size;
 		if (numcols < ((double) display.getWidth() / gallery_item_size))
 			++numcols;
 		gallery_item_size = display.getWidth() / numcols - 6; // padding
 
 		ConfigurationEnclosure conf = (ConfigurationEnclosure) getLastNonConfigurationInstance();
 		if (conf != null)
 		{
 			posts = conf.posts;
 			fetcher = new LazyPostFetcher( conf.url_enclosure );
 		}
 		else
 		{
 			posts = new ArrayList < Post >();
 			fetcher = new LazyPostFetcher();
 		}
 
 		GridView grid = (GridView) findViewById( R.id.gallery_grid );
 		grid.setOnItemLongClickListener( new OnItemLongClickListener()
 		{
 			@Override
 			public boolean onItemLongClick(AdapterView < ? > parent, View view, final int position, long id)
 			{
 				Builder builder = new AlertDialog.Builder( MainActivity.this );
 				builder.setTitle( R.string.main_download_dialog_title );
 				ImageView image = (ImageView) view;
 				ImageView download_preview = new ImageView( MainActivity.this );
 				download_preview.setImageDrawable( image.getDrawable() );
 				builder.setView( download_preview );
 				builder.setPositiveButton( android.R.string.yes, new OnClickListener()
 				{
 					@Override
 					public void onClick(DialogInterface dialog, int which)
 					{
 						Post post = posts.get( position );
 						String host[] = hosts.get( preferences.getInt( "selected_host", 0 ) );
 						String title = post.tags;
 						if (host != null)
 							title = String.format( "%1$s - %2$s", host[Hosts.HOST_NAME], post.tags );
 
 						Uri uri = Uri.parse( post.file_url );
 						String filename = uri.getLastPathSegment();
 						if (filename == null)
 							filename = Uri.encode( uri.toString() );
 
 						File dest = new File( android.os.Environment.getExternalStorageDirectory(), D.SAVEDIR + "/" + host[Hosts.HOST_NAME] + "/" + filename );
 						if ( !dest.getParentFile().exists())
 							dest.getParentFile().mkdirs();
 
 						DownloadManager.Request request = new DownloadManager.Request( uri );
 						request.setTitle( title );
 						request.setDestinationUri( Uri.fromFile( dest ) );
 						downloader.enqueue( request );
 					}
 				} );
 				builder.setNegativeButton( android.R.string.no, null );
 				builder.create().show();
 				return true;
 			}
 
 		} );
 		grid.setNumColumns( numcols );
 
 		adapter = new LazyImageAdapter( this, posts, gallery_item_size );
 		grid.setAdapter( adapter );
 		grid.setOnScrollListener( new GalleryOnScrollListener( fetcher, adapter, preferences.getInt( "page_limit", 16 ) ) );
 		grid.setOnItemClickListener( new GalleryOnItemClickListener() );
 
 		if (conf == null)
 			handleIntent( getIntent() );
 	}
 
 	@Override
 	protected void onNewIntent(Intent intent)
 	{
 		handleIntent( intent );
 	}
 
 	private void handleIntent(Intent intent)
 	{
 		Log.d( D.LOGTAG, "handleIntent(): action = " + intent.getAction() );
 		boolean dosearch = false;
 		String query = "";
 		if (intent.getAction().equals( Intent.ACTION_SEARCH ))
 		{
			query = intent.getStringExtra( SearchManager.QUERY );
 			dosearch = true;
 		}
 		else if (intent.getAction().equals( Intent.ACTION_VIEW ))
 		{
 			Uri uri = intent.getData();
 			if (uri != null)
 			{
 				query = uri.toString();
 				dosearch = true;
 			}
 		}
 
 		if (dosearch)
 		{
			Log.d( D.LOGTAG, "query = " + query );
			Toast.makeText( this, String.format( getString( R.string.main_query ), Uri.encode( query ) ), Toast.LENGTH_SHORT ).show();
 
 			if (fetcher.setTags( query ))
 			{
 				fetcher.setPage( 1 );
 				fetcher.cancel();
 				posts.clear();
 				adapter.cancelAll();
 				adapter.notifyDataSetChanged();
 				fetcher.fetchNextPage( adapter );
 			}
 		}
 		setIntent( intent );
 	}
 
 	@Override
 	public boolean onSearchRequested()
 	{
 		startSearch( fetcher.getURLEnclosure().tags, false, null, false );
 		return true;
 	}
 
 	public void loadPreferences()
 	{
 		Editor prefeditor = preferences.edit();
 
 		if ( !preferences.contains( "preference_version" ))
 			prefeditor.putInt( "preference_version", D.PREFERENCE_VERSION );
 
 		if ( !preferences.contains( "serialized_hosts" ))
 		{
 			hosts = new Hosts();
 			hosts.add( "Danbooru", "http://danbooru.donmai.us/" );
 			hosts.add( "Danbooru (mirror)", "http://hijiribe.donmai.us/" );
 			hosts.add( "Konachan", "http://konachan.com/" );
 			hosts.add( "Sankaku Complex (Chan)", "http://chan.sankakucomplex.com/" );
 			hosts.add( "Sankaku Complex (Idol)", "http://idol.sankakucomplex.com/" );
 			prefeditor.putString( "serialized_hosts", hosts.toCSV() );
 		}
 
 		if ( !preferences.contains( "selected_host" ))
 			prefeditor.putInt( "selected_host", 0 );
 
 		if ( !preferences.contains( "page_limit" ))
 			prefeditor.putInt( "page_limit", 16 );
 
 		if ( !preferences.contains( "rating" ))
 			prefeditor.putString( "rating", "s" );
 
 		prefeditor.apply();
 	}
 
 	@Override
 	public void onStart()
 	{
 		try
 		{
 			hosts = Hosts.fromCSV( preferences.getString( "serialized_hosts", "" ) );
 		}
 		catch (IOException e)
 		{
 			hosts = new Hosts();
 		}
 
 		String host[] = hosts.get( preferences.getInt( "selected_host", 0 ) );
 		String url = "http://konachan.com/";
 		if (host != null)
 			url = host[Hosts.HOST_URL];
 
 		boolean reset = false;
 		reset |= fetcher.setUrl( url + D.URL_POST );
 		reset |= fetcher.setRating( preferences.getString( "rating", "s" ) );
 		reset |= fetcher.setPageLimit( preferences.getInt( "page_limit", 16 ) );
 
 		if (reset)
 		{
 			fetcher.setPage( 1 );
 			fetcher.cancel();
 			posts.clear();
 			adapter.cancelAll();
 			adapter.notifyDataSetChanged();
 			fetcher.fetchNextPage( adapter );
 		}
 
 		super.onStart();
 	}
 
 	@Override
 	public void onLowMemory()
 	{
 		adapter.onLowMemory();
 	}
 
 	@Override
 	public boolean onCreateOptionsMenu(Menu menu)
 	{
 		MenuInflater inflater = getMenuInflater();
 		inflater.inflate( R.menu.main_menu, menu );
 		return super.onCreateOptionsMenu( menu );
 	}
 
 	@Override
 	public boolean onOptionsItemSelected(MenuItem item)
 	{
 		Builder builder = null;
 		switch (item.getItemId())
 		{
 		case R.id.main_menu_goto_page:
 		{
 			final EditText input = new EditText( this );
 			input.setInputType( InputType.TYPE_CLASS_NUMBER );
 			input.setText( String.valueOf( fetcher.enclosure.page ) );
 
 			builder = new AlertDialog.Builder( this );
 			builder.setView( input );
 			builder.setTitle( R.string.main_goto_page_dialog_title );
 			builder.setPositiveButton( android.R.string.ok, new OnClickListener()
 			{
 				@Override
 				public void onClick(DialogInterface dialog, int which)
 				{
 					if (fetcher.setPage( Integer.parseInt( "0" + input.getText().toString() ) ))
 					{
 						fetcher.cancel();
 						posts.clear();
 						adapter.notifyDataSetChanged();
 					}
 				}
 			} );
 			builder.setNegativeButton( android.R.string.cancel, null );
 
 			final AlertDialog dialog = builder.create();
 			dialog.show();
 			input.setOnFocusChangeListener( new OnFocusChangeListener()
 			{
 				@Override
 				public void onFocusChange(View v, boolean hasFocus)
 				{
 					dialog.getWindow().setSoftInputMode( WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE );
 				}
 			} );
 			input.requestFocus();
 		}
 			break;
 		case R.id.main_menu_goto_tags:
 			return onSearchRequested();
 		case R.id.main_menu_goto_reset_tags:
 			if (fetcher.setTags( "" ))
 			{
 				fetcher.setPage( 1 );
 				fetcher.cancel();
 				posts.clear();
 				adapter.cancelAll();
 				adapter.notifyDataSetChanged();
 				fetcher.fetchNextPage( adapter );
 			}
 			break;
 		case R.id.main_menu_refresh:
 			fetcher.cancel();
 			fetcher.setPage( 1 );
 			posts.clear();
 			adapter.notifyDataSetChanged();
 			fetcher.fetchNextPage( adapter );
 			break;
 		case R.id.main_menu_preferences:
 			startActivity( new Intent( this, DanbooruGalleryPreferenceActivity.class ) );
 			break;
 		default:
 			return super.onOptionsItemSelected( item );
 		}
 		return true;
 	}
 
 	@Override
 	public void onDestroy()
 	{
 		fetcher.cancel();
 		adapter.onDestroy();
 		super.onDestroy();
 	}
 
 	private class ConfigurationEnclosure
 	{
 		public List < Post >				posts;
 		public LazyPostFetcher.URLEnclosure	url_enclosure;
 
 		public ConfigurationEnclosure(List < Post > p, LazyPostFetcher.URLEnclosure e)
 		{
 			posts = p;
 			url_enclosure = e;
 		}
 	}
 
 	@Override
 	public Object onRetainNonConfigurationInstance()
 	{
 		return new ConfigurationEnclosure( posts, fetcher.getURLEnclosure() );
 	}
 
 	private class GalleryOnItemClickListener
 		implements OnItemClickListener
 	{
 		@Override
 		public void onItemClick(AdapterView < ? > parent, View view, int position, long id)
 		{
 			Intent intent = new Intent( MainActivity.this, ViewImageActivity.class );
 			intent.putExtra( "post.preview_url", posts.get( position ).preview_url );
 			intent.putExtra( "post.file_url", posts.get( position ).file_url );
 			intent.putExtra( "post.author", posts.get( position ).author );
 			intent.putExtra( "post.tags", posts.get( position ).tags );
 			intent.putExtra( "post.width", posts.get( position ).width );
 			intent.putExtra( "post.height", posts.get( position ).height );
 
 			if (posts.get( position ).created_at != null)
 				intent.putExtra( "post.created_at", posts.get( position ).created_at.getTime() );
 
 			String host[] = hosts.get( preferences.getInt( "selected_host", 0 ) );
 			intent.putExtra( "host_name", host[Hosts.HOST_NAME] );
 			intent.putExtra( "host_url", host[Hosts.HOST_URL] );
 
 			startActivityForResult( intent, REQUEST_TAGCLICKED );
 			overridePendingTransition( R.anim.zoom_up, R.anim.zoom_exit );
 		}
 	}
 
 	@Override
 	protected void onActivityResult(int requestCode, int resultCode, Intent data)
 	{
 		switch (requestCode)
 		{
 		case REQUEST_TAGCLICKED:
 			if (resultCode == RESULT_OK)
 				handleIntent( data );
 			break;
 		}
 	}
 
 	private class GalleryOnScrollListener
 		implements OnScrollListener
 	{
 		LazyPostFetcher		fetcher;
 		LazyImageAdapter	adapter;
 		Toast				toast_loading;
 		int					fetch_threshold;
 
 		public GalleryOnScrollListener(LazyPostFetcher f, LazyImageAdapter a, int l)
 		{
 			fetcher = f;
 			adapter = a;
 			toast_loading = Toast.makeText( a.getActivity(), R.string.main_loading_next_page, Toast.LENGTH_SHORT );
 			fetch_threshold = l;
 		}
 
 		@Override
 		public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount)
 		{
 			if (fetcher.hasMorePost() && firstVisibleItem + visibleItemCount > totalItemCount - fetch_threshold)
 			{
 				toast_loading.show();
 				fetcher.fetchNextPage( adapter );
 			}
 		}
 
 		@Override
 		public void onScrollStateChanged(AbsListView view, int scrollState)
 		{
 			// do nothing
 		}
 	}
 
 	static public class GalleryItemDisplayer
 		implements Runnable
 	{
 		private static Activity	activity;
 
 		final ImageView			image;
 		final Integer			res_id;
 		final Bitmap			bitmap;
 		final ScaleType			scale_type;
 		final boolean			do_animation;
 
 		static public void setActivity(Activity a)
 		{
 			activity = a;
 		}
 
 		public GalleryItemDisplayer(ImageView v, Bitmap b, ScaleType t, boolean anim)
 		{
 			res_id = null;
 			image = v;
 			bitmap = b;
 			scale_type = t;
 			do_animation = anim;
 		}
 
 		public GalleryItemDisplayer(ImageView v, int rid, ScaleType t, boolean anim)
 		{
 			res_id = rid;
 			image = v;
 			bitmap = null;
 			scale_type = t;
 			do_animation = anim;
 		}
 
 		public void display()
 		{
 			activity.runOnUiThread( this );
 		}
 
 		@Override
 		public void run()
 		{
 			if (image.getTag() == null)
 				return;
 
 			if (bitmap != null || res_id != null)
 			{
 				if (bitmap != null)
 					image.setImageBitmap( bitmap );
 
 				if (res_id != null)
 					image.setImageResource( res_id );
 
 				image.setScaleType( scale_type );
 
 				if (do_animation)
 				{
 					Animation anim = image.getAnimation();
 					if (anim != null)
 					{
 						if ( !anim.hasStarted() || anim.hasEnded())
 						{
 							anim.reset();
 							image.startAnimation( anim );
 						}
 					}
 					else
 						image.startAnimation( AnimationUtils.loadAnimation( image.getContext(), android.R.anim.fade_in ) );
 				}
 
 				image.setTag( null );
 			}
 		}
 	}
 }
