 package com.axelby.podax;
 
 import java.io.File;
 import java.util.Vector;
 
 import android.app.AlertDialog;
 import android.app.ListActivity;
 import android.app.NotificationManager;
 import android.content.BroadcastReceiver;
 import android.content.Context;
 import android.content.DialogInterface;
 import android.content.Intent;
 import android.content.IntentFilter;
 import android.graphics.BitmapFactory;
 import android.os.Bundle;
 import android.text.InputType;
 import android.util.Log;
 import android.view.ContextMenu;
 import android.view.LayoutInflater;
 import android.view.Menu;
 import android.view.MenuItem;
 import android.view.View;
 import android.view.ViewGroup;
 import android.widget.AdapterView;
 import android.widget.BaseAdapter;
 import android.widget.EditText;
 import android.widget.ImageView;
 import android.widget.LinearLayout;
 import android.widget.ListView;
 import android.widget.TextView;
 
 public class SubscriptionListActivity extends ListActivity {
 	private SubscriptionUpdateReceiver _subscriptionUpdateReceiver = new SubscriptionUpdateReceiver();	
 	private final class SubscriptionUpdateReceiver extends BroadcastReceiver {
 		@Override
 		public void onReceive(Context context, Intent intent) {
 			Log.d("Podax", "received a subscription update broadcast");
 			setListAdapter(new SubscriptionAdapter());
 		}
 	}
 
 	@Override
     public void onCreate(Bundle savedInstanceState) {
         super.onCreate(savedInstanceState);
         setContentView(R.layout.subscription_list);
         
         Intent intent = getIntent();
        if (intent.getDataString() != null) {
     		DBAdapter adapter = DBAdapter.getInstance(this);
     		Subscription subscription = adapter.addSubscription(intent.getDataString());
     		UpdateService.updateSubscription(this, subscription);
         }
      
         setListAdapter(new SubscriptionAdapter());
         registerForContextMenu(getListView());
         
         // remove any subscription update errors
 		String ns = Context.NOTIFICATION_SERVICE;
 		NotificationManager notificationManager = (NotificationManager) getSystemService(ns);
 		notificationManager.cancel(Constants.SUBSCRIPTION_UPDATE_ERROR);
 		
 		PlayerActivity.injectPlayerFooter(this);
     }
 	
 	@Override
 	public void onResume() {
 		super.onResume();
 		IntentFilter intentFilter = new IntentFilter(Constants.ACTION_SUBSCRIPTION_UPDATE_BROADCAST);
 		this.registerReceiver(_subscriptionUpdateReceiver, intentFilter);
 	}
 	
 	@Override
 	public void onPause() {
 		super.onPause();
 		this.unregisterReceiver(_subscriptionUpdateReceiver);
 	}
     
     @Override
     public boolean onCreateOptionsMenu(Menu menu) {
     	getMenuInflater().inflate(R.menu.subscription_list_menu, menu);
     	return true;
     }
 
     @Override
     public boolean onOptionsItemSelected(MenuItem item) {
         // Handle item selection
         switch (item.getItemId()) {
         case R.id.clear_subscriptions:
             DBAdapter.getInstance(this).deleteAllSubscriptions();
             setListAdapter(new SubscriptionAdapter());
             return true;
         case R.id.refresh_subscriptions:
         	UpdateService.updateSubscriptions(this);
         default:
             return super.onOptionsItemSelected(item);
         }
     }
     
     @Override
     public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
     	AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
     	if (info.position != 0)
     		menu.add(0, 0, 0, "Delete");
     }
 
     @Override
     public boolean onContextItemSelected(MenuItem item) {
     	switch (item.getItemId()) {
     	case 0:
     		AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
     		Subscription sub = (Subscription)getListView().getAdapter().getItem(menuInfo.position);
     		DBAdapter.getInstance(this).deleteSubscription(sub);
     		setListAdapter(new SubscriptionAdapter());
     		break;
     	default:
     	    return super.onContextItemSelected(item);
     	}
     	return true;
     }
 
     @Override
     protected void onListItemClick(ListView list, View view, int position, long id) {
     	// add subscription item
     	if (position == 0)
     	{
     		AlertDialog.Builder alert = new AlertDialog.Builder(this);
     		alert.setTitle("Podcast URL");
     		alert.setMessage("Type the URL of the podcast RSS");
     		final EditText input = new EditText(this);
     		//input.setText("http://blog.axelby.com/podcast.xml");
     		input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
     		alert.setView(input);
     		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
 				public void onClick(DialogInterface dialog, int which) {
 					String subscriptionUrl = input.getText().toString();
 					if (!subscriptionUrl.contains("://"))
 						subscriptionUrl = "http://" + subscriptionUrl;
 					Subscription subscription = DBAdapter.getInstance(SubscriptionListActivity.this).addSubscription(subscriptionUrl);					
 					getListView().setAdapter(new SubscriptionAdapter());
 					UpdateService.updateSubscription(SubscriptionListActivity.this, subscription);
 				}
 			});
     		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
 				public void onClick(DialogInterface dialog, int which) {
 					// do nothing
 				}
 			});
     		alert.show();
     		return;
     	}
     	
     	Intent intent = new Intent();
     	intent.setClassName("com.axelby.podax", "com.axelby.podax.PodcastListActivity");
     	Subscription sub = (Subscription)list.getItemAtPosition(position);
     	intent.putExtra("subscriptionId", sub.getId());
     	startActivity(intent);
 	}
     
     private class SubscriptionAdapter extends BaseAdapter {
     	private LayoutInflater _layoutInflater;
     	private Vector<Subscription> _subscriptions;
     	
     	public SubscriptionAdapter() {
     		_layoutInflater = LayoutInflater.from(SubscriptionListActivity.this);
     		_subscriptions = DBAdapter.getInstance(SubscriptionListActivity.this).getSubscriptions();
     	}
     	
 		public int getCount() {
 			return _subscriptions.size() + 1;
 		}
 
 		public Object getItem(int position) {
 			return _subscriptions.get(position - 1);
 		}
 
 		public long getItemId(int position) {
 			return position;
 		}
 
 		public View getView(int position, View convertView, ViewGroup parent) {
 			LinearLayout layout;
 			if (convertView == null)
 				layout = (LinearLayout)_layoutInflater.inflate(R.layout.subscription_list_item, null);
 			else
 				layout = (LinearLayout)convertView;
 			
 			TextView text = (TextView)layout.findViewById(R.id.text);
 			ImageView thumbnail = (ImageView)layout.findViewById(R.id.thumbnail);
 
 			if (position == 0) {
 				text.setText("Add subscription");
 				thumbnail.setVisibility(0);
 				return layout;
 			}
 
 			Subscription subscription = _subscriptions.get(position - 1);
 			text.setText(subscription.getDisplayTitle());
 
 			File thumbnailFile = new File(subscription.getThumbnailFilename());
 			if (!thumbnailFile.exists())
 				thumbnail.setImageDrawable(null);
 			else
 			{
 				thumbnail.setImageBitmap(BitmapFactory.decodeFile(subscription.getThumbnailFilename()));
 				thumbnail.setVisibility(1);
 			}
 			
 			return layout;
 		}
     }
 }
