 package com.namelessdev.mpdroid.views;
 
 import java.util.List;
 
 import org.a0z.mpd.Album;
 import org.a0z.mpd.Artist;
 import org.a0z.mpd.Item;
 import org.a0z.mpd.Music;
 import org.a0z.mpd.exception.MPDServerException;
 
 import android.content.Context;
 import android.content.SharedPreferences;
 import android.os.Handler;
 import android.os.HandlerThread;
 import android.preference.PreferenceManager;
 import android.view.View;
 import android.widget.ImageView;
 import android.widget.TextView;
 
 import com.namelessdev.mpdroid.MPDApplication;
 import com.namelessdev.mpdroid.R;
 import com.namelessdev.mpdroid.adapters.ArrayIndexerDataBinder;
 import com.namelessdev.mpdroid.cover.CachedCover;
 import com.namelessdev.mpdroid.helpers.AlbumCoverDownloadListener;
 import com.namelessdev.mpdroid.helpers.CoverAsyncHelper;
 import com.namelessdev.mpdroid.views.holders.AbstractViewHolder;
 import com.namelessdev.mpdroid.views.holders.AlbumViewHolder;
 
 public class AlbumDataBinder implements ArrayIndexerDataBinder {
 	protected CoverAsyncHelper coverHelper = null;
 	String artist = null;
 	MPDApplication app = null;
 	boolean lightTheme = false;
 	Handler asyncWorker = null;
 	boolean enableCache = true;
 	boolean onlyDownloadOnWifi = true;
 
 	public AlbumDataBinder(MPDApplication app, String artist, boolean isLightTheme) {
 		this.app = app;
 		this.artist = artist;
 		this.lightTheme = isLightTheme;
 		HandlerThread thread = new HandlerThread("CoverAsyncWorker");
 		thread.start();
 		asyncWorker = new Handler(thread.getLooper());
 
 		final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(app);
 
 		enableCache = settings.getBoolean(CoverAsyncHelper.PREFERENCE_CACHE, true);
 		onlyDownloadOnWifi = settings.getBoolean(CoverAsyncHelper.PREFERENCE_ONLY_WIFI, false);
 	}
 
 	public void onDataBind(final Context context, final View targetView, final AbstractViewHolder viewHolder, List<? extends Item> items, Object item, int position) {
 		AlbumViewHolder holder = (AlbumViewHolder) viewHolder;
 
 		final Album album = (Album) item;
 		String info = "";
 		final long songCount = album.getSongCount();
 		if(album.getYear() > 0)
 			info = Long.toString(album.getYear());
 		if(songCount > 0) {
 			if(info != null && info.length() > 0)
 				info += " - ";
 			info += String.format(context.getString(songCount > 1 ? R.string.tracksInfoHeaderPlural : R.string.tracksInfoHeader), songCount, Music.timeToString(album.getDuration()));
 		}
 		holder.albumName.setText(album.getName());
 		if(info != null && info.length() > 0) {
 			holder.albumInfo.setVisibility(View.VISIBLE);
 			holder.albumInfo.setText(info);
 		} else {
 			holder.albumInfo.setVisibility(View.GONE);
 		}
 
 		final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(app);
 
 		coverHelper = new CoverAsyncHelper(app, settings);
 		final int height = holder.albumCover.getHeight();
 		// If the list is not displayed yet, the height is 0. This is a problem, so set a fallback one.
 		coverHelper.setCoverMaxSize(height == 0 ? 128 : height);
 
 		loadPlaceholder();
 		
 		// display cover art in album listing if caching is on
 		if (this.artist != null && settings.getBoolean(CoverAsyncHelper.PREFERENCE_CACHE, true)) {
 			// listen for new artwork to be loaded
 			final AlbumCoverDownloadListener acd = new AlbumCoverDownloadListener(context, holder.albumCover);
 			final AlbumCoverDownloadListener oldAcd = (AlbumCoverDownloadListener) holder.albumCover
 					.getTag(R.id.AlbumCoverDownloadListener);
 			if (oldAcd != null)
 				oldAcd.detach();
 			holder.albumCover.setTag(R.id.AlbumCoverDownloadListener, acd);
 			coverHelper.addCoverDownloadListener(acd);
 
 			loadArtwork(artist, album.getName());
 		}
 	}
 
 	protected void loadPlaceholder() {
 		coverHelper.obtainMessage(CoverAsyncHelper.EVENT_COVERNOTFOUND).sendToTarget();
 	}
 
 	protected void loadArtwork(String artist, String album) {
 		boolean haveCachedArtwork = false;
 
 		if (enableCache) {
 			try {
 				CachedCover cachedCover = new CachedCover(app);
 				final String[] urls = cachedCover.getCoverUrl(artist, album, null, null);
 				if ((urls != null) && (urls.length > 0)) {
 					haveCachedArtwork = true;
 				}
 			} catch (Exception e) {
 				// no cached artwork available
 			}
 		}
 
 		// Did we find a cached cover ? If yes, skip the download
 		// Only continue if we are not on WiFi and Cellular download is enabled
 		if (!haveCachedArtwork) {
 			// If we are not on WiFi and Cellular download is enabled
 			if (!coverHelper.isWifi() && !onlyDownloadOnWifi) {
 				asyncWorker.post(new DownloaderRunnable(album, artist));
 			}
 		} else {
 			coverHelper.downloadCover(artist, album, null, null);
 		}
 	}
 
 	public boolean isEnabled(int position, List<? extends Item> items, Object item) {
 		return true;
 	}
 
 	@Override
 	public int getLayoutId() {
 		return R.layout.album_list_item;
 	}
 
 	@Override
 	public View onLayoutInflation(Context context, View targetView, List<? extends Item> items) {
 		targetView.findViewById(R.id.albumCover).setVisibility(coverHelper == null ? View.GONE : View.VISIBLE);
 		return targetView;
 	}
 
 	@Override
 	public AbstractViewHolder findInnerViews(View targetView) {
 		// look up all references to inner views
 		AlbumViewHolder viewHolder = new AlbumViewHolder();
 		viewHolder.albumName = (TextView) targetView.findViewById(R.id.album_name);
 		viewHolder.albumInfo = (TextView) targetView.findViewById(R.id.album_info);
 		viewHolder.albumCover = (ImageView) targetView.findViewById(R.id.albumCover);
 		return viewHolder;
 	}
 
 	public class DownloaderRunnable implements Runnable {
 		String album;
 		String artist;
 		
 		public DownloaderRunnable(String album, String artist) {
 			this.album = album;
 			this.artist = artist;
 		}
 		
 		@Override
 		public void run() {
 			String filename = null;
 			String path = null;
 			List<? extends Item> songs = null;
 
 			try {
 				// load songs for this album
 				songs = app.oMPDAsyncHelper.oMPD.getSongs(((artist != null) ? new Artist(artist, 0) : null), new Album(album));
 
 				if (songs.size() > 0) {
 					Music song = (Music) songs.get(0);
 					filename = song.getFilename();
 					path = song.getPath();
 					coverHelper.downloadCover(artist, album, path, filename);
 				}
 			} catch (MPDServerException e) {
 				// MPD error, bail on loading artwork
 				return;
 			}
 		}
 	}
 }
