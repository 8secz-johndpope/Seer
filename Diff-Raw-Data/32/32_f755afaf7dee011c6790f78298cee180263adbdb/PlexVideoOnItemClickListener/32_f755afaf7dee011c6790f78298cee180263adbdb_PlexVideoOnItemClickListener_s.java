 /**
  * The MIT License (MIT)
  * Copyright (c) 2013 David Carver
  * Permission is hereby granted, free of charge, to any person obtaining
  * a copy of this software and associated documentation files (the
  * "Software"), to deal in the Software without restriction, including
  * without limitation the rights to use, copy, modify, merge, publish,
  * distribute, sublicense, and/or sell copies of the Software, and to
  * permit persons to whom the Software is furnished to do so, subject to
  * the following conditions:
  * 
  * The above copyright notice and this permission notice shall be included
  * in all copies or substantial portions of the Software.
  * 
  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
  * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
  * OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
  * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF
  * OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  * SOFTWARE.
  */
 
 package us.nineworlds.serenity.ui.listeners;
 
 import us.nineworlds.serenity.MainActivity;
 import us.nineworlds.serenity.R;
 import us.nineworlds.serenity.core.services.WatchedEpisodeAsyncTask;
 import us.nineworlds.serenity.ui.browser.tv.episodes.EpisodePosterOnItemSelectedListener;
 import us.nineworlds.serenity.ui.video.player.SerenitySurfaceViewVideoActivity;
 import us.nineworlds.serenity.ui.views.SerenityPosterImageView;
 import us.nineworlds.serenity.widgets.SerenityAdapterView;
 import us.nineworlds.serenity.widgets.SerenityAdapterView.OnItemClickListener;
 
 import android.app.Activity;
 import android.content.Intent;
 import android.content.SharedPreferences;
 import android.net.Uri;
 import android.preference.PreferenceManager;
 import android.view.View;
 import android.widget.ImageView;
 
 /**
  * @author dcarver
  *
  */
 public class PlexVideoOnItemClickListener  implements OnItemClickListener {
 
 	/* (non-Javadoc)
 	 * @see android.widget.AdapterView.OnItemClickListener#onItemClick(android.widget.AdapterView, android.view.View, int, long)
 	 */
 	public void onItemClick(SerenityAdapterView<?> av, View v, int arg2, long arg3) {
 		SerenityPosterImageView mpiv = (SerenityPosterImageView) v;
 		
 		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(v.getContext());
 		boolean externalPlayer = prefs.getBoolean("external_player", false);
 		
 		if (externalPlayer) {
 			String url = mpiv.getPosterInfo().getDirectPlayUrl();
 			Intent vpIntent = new Intent(Intent.ACTION_VIEW);
 			vpIntent.setDataAndType(Uri.parse(url), "video/*");
 			
 			mxVideoPlayerOptions(mpiv, vpIntent);
 			vimuVideoPlayerOptions(mpiv, vpIntent);
 			
 			Activity activity = (Activity) mpiv.getContext();
 			activity.startActivity(vpIntent);
 			new WatchedEpisodeAsyncTask().execute(mpiv.getPosterInfo().id());
 			updatedWatchedCount(mpiv, activity);
 			return;
 		}
 		
		String url = mpiv.getPosterInfo().getDirectPlayUrl();
 		Intent vpIntent = new Intent(mpiv.getContext(), SerenitySurfaceViewVideoActivity.class);
 		vpIntent.putExtra("videoUrl", url);
		vpIntent.putExtra("title", mpiv.getPosterInfo().getTitle());
		vpIntent.putExtra("summary", mpiv.getPosterInfo().getPlotSummary());
		vpIntent.putExtra("posterUrl", mpiv.getPosterInfo().getPosterURL());
		vpIntent.putExtra("id", mpiv.getPosterInfo().id());
		vpIntent.putExtra("aspectRatio", mpiv.getPosterInfo().getAspectRatio());
		vpIntent.putExtra("videoResolution", mpiv.getPosterInfo().getVideoResolution());
		vpIntent.putExtra("audioFormat", mpiv.getPosterInfo().getAudioCodec());
		vpIntent.putExtra("videoFormat", mpiv.getPosterInfo().getVideoCodec());
		vpIntent.putExtra("audioChannels", mpiv.getPosterInfo().getAudioChannels());
		vpIntent.putExtra("resumeOffset", mpiv.getPosterInfo().getResumeOffset());
 		
 		
 		Activity a = (Activity) mpiv.getContext();
 		a.startActivityForResult(vpIntent, MainActivity.BROWSER_RESULT_CODE);
 		updatedWatchedCount(mpiv, a);
 	}
 	
 	/**
 	 * @param epiv
 	 * @param a
 	 */
 	protected void updatedWatchedCount(SerenityPosterImageView epiv, Activity a) {
 		ImageView watchedView = (ImageView)a.findViewById(EpisodePosterOnItemSelectedListener.WATCHED_VIEW_ID);
 		watchedView.setImageResource(R.drawable.watched_small);
 		int watchedCount = epiv.getPosterInfo().getViewCount();
 		epiv.getPosterInfo().setViewCount(watchedCount + 1);
 	}
 	
 	
 	/**
 	 * @param epiv
 	 * @param vpIntent
 	 */
 	protected void vimuVideoPlayerOptions(SerenityPosterImageView epiv,
 			Intent vpIntent) {
 		vpIntent.putExtra("forcename", epiv.getPosterInfo().getTitle());
 		vpIntent.putExtra("forcedirect", true);
 	}
 
 	/**
 	 * @param epiv
 	 * @param vpIntent
 	 */
 	protected void mxVideoPlayerOptions(SerenityPosterImageView epiv,
 			Intent vpIntent) {
 		// MX Video Player options
 		vpIntent.putExtra("decode_mode", 1);
 		vpIntent.putExtra("title", epiv.getPosterInfo().getTitle());
 		vpIntent.putExtra("return_result", true);
 	}
 
 }
