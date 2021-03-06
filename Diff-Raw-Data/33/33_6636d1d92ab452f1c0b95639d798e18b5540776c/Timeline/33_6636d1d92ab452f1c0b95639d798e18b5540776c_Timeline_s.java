 package com.tuit.ar.activities;
 
 import java.net.MalformedURLException;
 import java.net.URL;
 import java.util.ArrayList;
 import java.util.HashMap;
 
 import android.app.Activity;
 import android.app.AlertDialog;
 import android.app.ListActivity;
 import android.content.DialogInterface;
 import android.content.Intent;
 import android.content.DialogInterface.OnClickListener;
 import android.net.Uri;
 import android.os.Bundle;
 import android.view.MenuItem;
 import android.view.View;
 import android.view.ViewGroup;
 import android.view.Window;
 import android.widget.ArrayAdapter;
 import android.widget.ImageView;
 import android.widget.TextView;
 import android.widget.Toast;
 
 import com.tuit.ar.R;
 import com.tuit.ar.activities.timeline.DirectMessages;
 import com.tuit.ar.activities.timeline.Favorites;
 import com.tuit.ar.activities.timeline.Friends;
 import com.tuit.ar.activities.timeline.Replies;
 import com.tuit.ar.api.Avatar;
 import com.tuit.ar.api.AvatarObserver;
 import com.tuit.ar.api.Twitter;
 import com.tuit.ar.models.ListElement;
 import com.tuit.ar.models.Settings;
 import com.tuit.ar.models.User;
 import com.tuit.ar.models.timeline.TimelineObserver;
 import com.tuit.ar.services.Updater;
 
 abstract public class Timeline extends ListActivity implements TimelineObserver {
 	protected static final int MENU_NEW_TWEET = 0;   
 	protected static final int MENU_REFRESH = 1;   
 	protected static final int MENU_FRIENDS = 2;
 	protected static final int MENU_REPLIES = 3;
 	protected static final int MENU_DIRECT = 4;
 	protected static final int MENU_PREFERENCES = 5;
 	protected static final int MENU_NEW_DIRECT_MESSAGE = 6;
 	protected static final int MENU_FAVORITES = 7;
 	protected static final int MENU_MY_PROFILE = 8;
 
 	
 	protected TimelineAdapter<? extends ListElement> timelineAdapter;
 	protected boolean isVisible;
 	protected long newestTweet = 0;
 
 	abstract protected com.tuit.ar.models.Timeline getTimeline();
 
 	public void onCreate(Bundle savedInstanceState) {
 		super.onCreate(savedInstanceState);
         requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
 		setContentView(R.layout.timeline);
 
 		getTimeline().addObserver(this);
		if (Settings.getInstance().getSharedPreferences(this).getBoolean(Settings.LAZY_MODE, Settings.LAZY_MODE_DEFAULT) == false) {
 			getTimeline().refresh();
 		}
 		this.startService(new Intent(this, Updater.class));
 	}
 
    protected void onResume() {
     	super.onResume();
     	isVisible = true;
     	timelineAdapter.notifyDataSetChanged();
     }
 
     protected void onPause() {
     	super.onPause();
     	isVisible = false;
     }
 
 	@Override
 	public void onDestroy() {
 		super.onDestroy();
 		getTimeline().removeObserver(this);
 	}
 
 	public boolean onOptionsItemSelected(MenuItem item) {  
 	    switch (item.getItemId()) {  
 	    case MENU_REFRESH:
 	    {
 	        refresh();
 	        return true;
 	    }
 	    case MENU_FRIENDS:
 	    {
 	    	Intent intent = new Intent(this.getApplicationContext(), Friends.class);
 	    	this.startActivity(intent);		
 	    	return true;
 	    }
 	    case MENU_DIRECT:
 	    {
 	    	Intent intent = new Intent(this.getApplicationContext(), DirectMessages.class);
 	    	this.startActivity(intent);		
 	    	return true;
 	    }
 	    case MENU_NEW_TWEET:
 	    {
 	    	Intent intent = new Intent(this.getApplicationContext(), NewTweet.class);
 	    	this.startActivity(intent);		
 	    	return true;
 	    }
 	    case MENU_FAVORITES:
 	    {
 	    	Intent intent = new Intent(this.getApplicationContext(), Favorites.class);
 	    	this.startActivity(intent);		
 	    	return true;
 	    }
 	    case MENU_REPLIES:
 	    {
 	    	Intent intent = new Intent(this.getApplicationContext(), Replies.class);
 	    	this.startActivity(intent);		
 	    	return true;
 	    }
 	    case MENU_PREFERENCES:
 	    {
 			Intent intent = new Intent(this.getApplicationContext(), Preferences.class);
 			this.startActivity(intent);		
 	        return true;
 	    }
 	    case MENU_NEW_DIRECT_MESSAGE:
 	    {
 			Intent intent = new Intent(this.getApplicationContext(), NewDirectMessage.class);
 			this.startActivity(intent);		
 	        return true;
 	    }
 	    case MENU_MY_PROFILE:
 	    {
 			Profile.setUserToDisplay(Twitter.getInstance().getDefaultAccount().getUser());
 			Intent intent = new Intent(this.getApplicationContext(), Profile.class);
 			this.startActivity(intent);
 	        return true;
 	    }
 	    }
 	    return false;
 	}
 
 	protected void showProfile(User user) {
 		Profile.setUserToDisplay(user);
 		startActivity(new Intent(getApplicationContext(), Profile.class));
 	}
 
 	protected void showProfile(String screen_name) {
 		ArrayList<User> users = User.select("screen_name = ?", new String[]{screen_name}, null, null, null, "1");
 		Intent intent = new Intent(getApplicationContext(), Profile.class);
 		if (users.size() > 0) {
 			Profile.setUserToDisplay(users.get(0));
 		} else {
 			Profile.setUserToDisplay(null);
 			intent.putExtra("screen_name", screen_name);
 		}
 		startActivity(intent);
 	}
 
 
 	protected void openLinksInBrowser(ListElement tweet) {
 		final String[] urls = parseUrls(tweet.getText());
 		if (urls.length == 0) {
 			Toast.makeText(this, getString(R.string.noURLFound), Toast.LENGTH_SHORT).show();
 		} else if (urls.length == 1) {
 			this.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(urls[0].toString())));
 		} else { // we have 2+ urls
 			new AlertDialog.Builder(this).
 			setTitle(getString(R.string.selectURL)).
 			setItems(urls,
 					new OnClickListener() {
 						public void onClick(DialogInterface dialog, int which) {
 							startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(urls[which])));
 						}
 			}).show();
 		}
 	}
 
 	static protected String[] parseUrls(String message) {
         String [] parts = message.split("\\s");
 
         ArrayList<String> foundURLs = new ArrayList<String>();
         for( String item : parts ) try {
             foundURLs.add((new URL(item)).toString());
         } catch (MalformedURLException e) {
         }
 
         return (String[])foundURLs.toArray(new String[foundURLs.size()]);
 	}
 
 	protected void refresh() {
 		getTimeline().refresh();
 	}
 
 	public void timelineRequestStarted(com.tuit.ar.models.Timeline timeline) {
 		this.setProgressBarIndeterminateVisibility(true);
 	}
 	public void timelineRequestFinished(com.tuit.ar.models.Timeline timeline) {
 		this.setProgressBarIndeterminateVisibility(false);
 	}
 
 	public void timelineUpdateHasFailed(com.tuit.ar.models.Timeline timeline, String message) {
 		if (isVisible) {
 			if (message == null)
 				Toast.makeText(this, getString(R.string.unableToFetchTimeline), Toast.LENGTH_SHORT).show();
 			else
 				Toast.makeText(this, getString(R.string.unableToFetchTimeline) + " (" + message + ")", Toast.LENGTH_SHORT).show();
 		}
 	}
 
 	protected class TimelineAdapter<T> extends ArrayAdapter<T> 
 	{
 		protected Activity context;
 		protected HashMap<View, TimelineElement> elements = new HashMap<View, TimelineElement>();
 		protected ArrayList<T> tweets;
 
 		public TimelineAdapter(Activity context, ArrayList<T> tweets)
 		{
 			super(context, R.layout.timeline_element, tweets);
 			this.context = context;
 			this.tweets = tweets;
 		}
 
 		public View getView(int position, View convertView, ViewGroup parent)
 		{
 			TimelineElement element = getTimelineElement(convertView);
 
 			ListElement tweet = (ListElement) tweets.get(position);
 			if (element.currentTweet == tweet) return element.getView();
 
 			element.getUsername().setText("@" + tweet.getUsername());
 			element.getMessage().setText(tweet.getText());
 			element.getDate().setText(tweet.getDisplayDate());
 			if (Settings.getInstance().getSharedPreferences(Timeline.this).getBoolean(Settings.SHOW_AVATAR, Settings.SHOW_AVATAR_DEFAULT)) {
 				element.getAvatar().setVisibility(View.INVISIBLE);
 
 				Avatar avatar = Avatar.get(tweet.getAvatarUrl());
 				avatar.addRequestObserver(element);
 				avatar.download();
 			} else {
 				element.getAvatar().setVisibility(View.GONE);
 			}
 
 			element.currentTweet = tweet;
 
 			return element.getView();
 		}
 
 		private TimelineElement getTimelineElement(View convertView) {
 			if (convertView == null)
 			{
 				convertView = View.inflate(this.context, R.layout.timeline_element, null);
 			}
 
 			if (!elements.containsKey(convertView)) {
 				elements.put(convertView, new TimelineElement(convertView));
 			}
 			return elements.get(convertView);
 		}
 	}
 
 	protected class TimelineElement implements AvatarObserver {
 		private View view;
 		private TextView username;
 		private TextView message;
 		private TextView date;
 		private ImageView avatar;
 		public ListElement currentTweet; 
 
 		public TimelineElement(View view) {
 			super();
 			this.view = view;
 		}
 
 		public TextView getUsername() {
 			if (username != null) return username;
 			else return username = (TextView)view.findViewById(R.id.username);
 		}
 
 		public TextView getMessage() {
 			if (message != null) return message;
 			else return message = (TextView)view.findViewById(R.id.message);
 		}
 
 		public TextView getDate() {
 			if (date != null) return date;
 			else return date = (TextView)view.findViewById(R.id.date);
 		}
 
 		public ImageView getAvatar() {
 			if (avatar != null) return avatar;
 			else return avatar = (ImageView)view.findViewById(R.id.avatar);
 		}
 
 		public View getView() { return view; }
 
 		public void avatarHasFailed(Avatar avatar) {
 			getAvatar().setVisibility(View.GONE);
 			avatar.removeRequestObserver(this);
 		}
 
 		public void avatarHasFinished(Avatar avatar) {
 			getAvatar().setVisibility(View.VISIBLE);
 			getAvatar().setImageBitmap(avatar.getResponse());
 			avatar.removeRequestObserver(this);
 		}
 	}
 }
