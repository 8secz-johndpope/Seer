 package com.axelby.podax;
 
 import java.io.IOException;
 import java.text.ParseException;
 import java.text.SimpleDateFormat;
 import java.util.Date;
 
 import org.apache.http.HttpResponse;
 import org.apache.http.client.HttpClient;
 import org.apache.http.client.methods.HttpGet;
 import org.apache.http.impl.client.DefaultHttpClient;
 import org.joda.time.DateTime;
 import org.joda.time.Weeks;
 import org.xml.sax.Attributes;
 import org.xml.sax.SAXException;
 
 import android.app.Activity;
 import android.app.ProgressDialog;
 import android.content.ContentValues;
 import android.os.AsyncTask;
 import android.os.Bundle;
 import android.sax.Element;
 import android.sax.EndTextElementListener;
 import android.sax.RootElement;
 import android.sax.StartElementListener;
 import android.util.Xml;
 import android.view.View;
 import android.view.View.OnClickListener;
 import android.widget.Button;
 import android.widget.TextView;
 
 public class PopularSubscriptionActivity extends Activity {
 
 	private class FeedDetails {
 		private String title;
 		private String description;
 		private int podcastCount;
 		private Date lastBuildDate;
 		private Date oldestPodcastDate;
 
 		private FeedDetails() {
 			podcastCount = 0;
 			lastBuildDate = new Date();
 		}
 	}
 
 	ProgressDialog _dialog;
 
 	@Override
 	protected void onCreate(Bundle savedInstanceState) {
 		super.onCreate(savedInstanceState);
 
 		setContentView(R.layout.popularsubscription);
 
 		String title = getIntent().getExtras().getString(Constants.EXTRA_TITLE);
 		TextView titleView = (TextView) findViewById(R.id.title);
 		titleView.setText(title);
 
 		Button add_subscription = (Button) findViewById(R.id.add_subscription);
 		add_subscription.setOnClickListener(new OnClickListener() {
 			public void onClick(View v) {
 				ContentValues values = new ContentValues();
 				values.put(SubscriptionProvider.COLUMN_URL, getIntent().getExtras().getString(Constants.EXTRA_URL));
 				getContentResolver().insert(SubscriptionProvider.URI, values);
 				finish();
 			}			
 		});
 
 		_dialog = ProgressDialog.show(this, "", "Loading Subscription...", true, true);
 
 		String url = getIntent().getExtras().getString(Constants.EXTRA_URL);
 		new AsyncTask<String, Void, FeedDetails>() {
 			@Override
 			protected FeedDetails doInBackground(String... urls) {
 				if (urls.length != 1)
 					return null;
 
 				final FeedDetails details = new FeedDetails();
 
 				RootElement root = new RootElement("rss");
 				Element channel = root.getChild("channel");
 				channel.getChild("title").setEndTextElementListener(new EndTextElementListener() {
 					public void end(String body) {
 						details.title = body;
 					}
 				});
 				channel.getChild("description").setEndTextElementListener(new EndTextElementListener() {
 					public void end(String body) {
 						details.description = body;
 					}
 				});
 				channel.getChild("lastBuildDate").setEndTextElementListener(new EndTextElementListener() {
 					public void end(String body) {
 						details.lastBuildDate = parseRFC822Date(body);
 					}
 				});
 				channel.getChild("item").setStartElementListener(new StartElementListener() {
 					public void start(Attributes attributes) {
 						details.podcastCount++;
 					}
 				});
 				channel.getChild("item").getChild("pubDate").setEndTextElementListener(new EndTextElementListener() {
 					public void end(String body) {
 						Date pubDate = parseRFC822Date(body);
 						if (pubDate == null)
 							return;
 						if (details.oldestPodcastDate == null || pubDate.before(details.oldestPodcastDate))
 							details.oldestPodcastDate = pubDate;
 					}
 				});
 
 				HttpGet get = new HttpGet(urls[0]);
 				HttpClient client = new DefaultHttpClient();
 				try {
 					HttpResponse response = client.execute(get);
 					Xml.parse(response.getEntity().getContent(), Xml.Encoding.UTF_8, root.getContentHandler());
 				} catch (IOException e) {
 					e.printStackTrace();
 				} catch (IllegalStateException e) {
 					e.printStackTrace();
 				} catch (SAXException e) {
 					e.printStackTrace();
 				}
 
 				return details;
 			}
 
 			@Override
 			protected void onPostExecute(FeedDetails result) {
 				TextView title = (TextView) findViewById(R.id.title);
 				title.setText(result.title);
 
 				if (result.lastBuildDate != null && result.oldestPodcastDate != null) {
 					DateTime lastBuildDate = new DateTime(result.lastBuildDate);
 					DateTime oldestPodcastDate = new DateTime(result.oldestPodcastDate);
 					Weeks weeks = Weeks.weeksBetween(oldestPodcastDate, lastBuildDate);
 					if (weeks.getWeeks() > 0) {
 						float podcastsPerWeek = (float)result.podcastCount / weeks.getWeeks();
 						podcastsPerWeek = Math.round(podcastsPerWeek * 10) / 10.0f;
 						result.description = result.description + "\n\nAverages " + podcastsPerWeek + " podcasts per week";
 					}
 				}
 
 				TextView description = (TextView) findViewById(R.id.description);
 				description.setText(result.description);
 
				_dialog.hide();
 			}
 
 		}.execute(url);
 	}
 
 
 	public static Date parseRFC822Date(String date) {
 		for (SimpleDateFormat format : SubscriptionUpdater.rfc822DateFormats) {
 			try {
 				return format.parse(date);
 			} catch (ParseException e) {
 			}
 		}
 		return null;
 	}
 }
