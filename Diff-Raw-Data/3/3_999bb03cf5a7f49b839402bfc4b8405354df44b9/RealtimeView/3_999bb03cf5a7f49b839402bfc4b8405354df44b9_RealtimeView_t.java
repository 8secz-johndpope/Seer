 /**
  *     Copyright (C) 2009 Anders Aagaard <aagaande@gmail.com>
  *
  *   This program is free software: you can redistribute it and/or modify
  *   it under the terms of the GNU General Public License as published by
  *   the Free Software Foundation, either version 3 of the License, or
  *   (at your option) any later version.
  *
  *   This program is distributed in the hope that it will be useful,
  *   
  *   but WITHOUT ANY WARRANTY; without even the implied warranty of
  *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  *   GNU General Public License for more details.
  *
  *   You should have received a copy of the GNU General Public License
  *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
  */
 
 package com.neuron.trafikanten.views.realtime;
 
 import java.util.ArrayList;
 
 import android.app.Dialog;
 import android.app.ListActivity;
 import android.content.Context;
 import android.graphics.Color;
 import android.os.Bundle;
 import android.util.Log;
 import android.view.ContextMenu;
 import android.view.LayoutInflater;
 import android.view.Menu;
 import android.view.MenuItem;
 import android.view.View;
 import android.view.ViewGroup;
 import android.view.Window;
 import android.view.ContextMenu.ContextMenuInfo;
 import android.widget.BaseAdapter;
 import android.widget.ListView;
 import android.widget.TextView;
 import android.widget.Toast;
 import android.widget.AdapterView.AdapterContextMenuInfo;
 
 import com.neuron.trafikanten.HelperFunctions;
 import com.neuron.trafikanten.R;
 import com.neuron.trafikanten.dataProviders.DataProviderFactory;
 import com.neuron.trafikanten.dataProviders.IRealtimeProvider;
 import com.neuron.trafikanten.dataProviders.IRealtimeProvider.RealtimeProviderHandler;
 import com.neuron.trafikanten.dataSets.RealtimeData;
 import com.neuron.trafikanten.dataSets.SearchStationData;
 import com.neuron.trafikanten.notification.NotificationDialog;
 
 public class RealtimeView extends ListActivity {
 	private static final String TAG = "Trafikanten-RealtimeView";
 	
 	/*
 	 * Options menu:
 	 */
 	private static final int REFRESH_ID = Menu.FIRST;
 	
 	/*
 	 * Context menu:
 	 */
 	private static final int NOTIFY_ID = Menu.FIRST;
 	
 	/*
 	 * Dialogs
 	 */
 	private static final int DIALOG_NOTIFICATION = 1;
 	private int selectedId = 0;
 	
 	/*
 	 * Saved instance data
 	 */
 	private SearchStationData station;
 	private RealtimeAdapter realtimeList;
 	
 	/*
 	 * Data provider
 	 */
 	private IRealtimeProvider realtimeProvider;
 	
     /** Called when the activity is first created. */
     @Override
     public void onCreate(Bundle savedInstanceState) {
         super.onCreate(savedInstanceState);
         requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
         
         /*
          * Setup view and adapter.
          */
         setContentView(R.layout.realtime);
         realtimeList = new RealtimeAdapter(this);
         
         /*
          * Load instance state
          */
         if (savedInstanceState == null) {
         	station = getIntent().getParcelableExtra(SearchStationData.PARCELABLE);
             load();
         } else {
         	station = savedInstanceState.getParcelable(SearchStationData.PARCELABLE);
         	final ArrayList<RealtimeData> list = savedInstanceState.getParcelableArrayList(RealtimeAdapter.KEY_REALTIMELIST);
         	realtimeList.setList(list);
         }
         setTitle("Trafikanten - " + station.stopName);
         registerForContextMenu(getListView());
        setListAdapter(realtimeList);
     }
     
     private void load() {
     	setProgressBarIndeterminateVisibility(true);
     	if (realtimeProvider != null)
     		realtimeProvider.Stop();
     	realtimeProvider = DataProviderFactory.getRealtimeProvider(new RealtimeProviderHandler() {
 			@Override
 			public void onData(RealtimeData realtimeData) {
 				realtimeList.addItem(realtimeData);
 				realtimeList.notifyDataSetChanged();
 			}
 
 			@Override
 			public void onError(Exception exception) {
 				Log.w(TAG,"onException " + exception);
 				Toast.makeText(RealtimeView.this, "" + getText(R.string.exception) + "\n" + exception, Toast.LENGTH_LONG).show();
 				setProgressBarIndeterminateVisibility(false);
 			}
 
 			@Override
 			public void onFinished() {
 				setProgressBarIndeterminateVisibility(false);
 				/*
 				 * Show info text if view is empty
 				 */
 				final TextView infoText = (TextView) findViewById(R.id.emptyText);
 				infoText.setVisibility(realtimeList.getCount() > 0 ? View.GONE : View.VISIBLE);
 			}
     	});
     	realtimeProvider.Fetch(station.stationId);
     }
     
 	/*
 	 * Options menu, visible on menu button.
 	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
 	 */
 	@Override
 	public boolean onCreateOptionsMenu(Menu menu) {
 		final MenuItem myLocation = menu.add(0, REFRESH_ID, 0, R.string.refresh);
 		myLocation.setIcon(R.drawable.ic_menu_refresh);
 		return true;
 	}
 
 	/*
 	 * Options menu item selected, options menu visible on menu button.
 	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
 	 */
 	@Override
 	public boolean onOptionsItemSelected(MenuItem item) {
         switch(item.getItemId()) {
         case REFRESH_ID:
         	load();
         	break;
         }
 		return super.onOptionsItemSelected(item);
 	}
 
 	/*
 	 * Dialog creation
 	 * @see android.app.Activity#onCreateDialog(int)
 	 */
 	@Override
 	protected Dialog onCreateDialog(int id) {
 		switch(id) {
 		case DIALOG_NOTIFICATION:
 			/*
 			 * notify dialog
 			 */
 			return NotificationDialog.getDialog(this);
 		}
 		return super.onCreateDialog(id);
 	}
 
 	/*
 	 * Load data into dialog
 	 * @see android.app.Activity#onPrepareDialog(int, android.app.Dialog)
 	 */
 	@Override
 	protected void onPrepareDialog(int id, Dialog dialog) {
 		final RealtimeData realtimeData = (RealtimeData) realtimeList.getItem(selectedId);
 		final String notifyWith = realtimeData.line.equals(realtimeData.destination) 
 			? realtimeData.line 
 			: realtimeData.line + " " + realtimeData.destination;
 		NotificationDialog.setRealtimeData(realtimeData, station, notifyWith);
 		super.onPrepareDialog(id, dialog);
 	}
 
 	/*
 	 * onCreate - Context menu is a popup from a longpress on a list item.
 	 * @see android.app.Activity#onCreateContextMenu(android.view.ContextMenu, android.view.View, android.view.ContextMenu.ContextMenuInfo)
 	 */
 	@Override
 	public void onCreateContextMenu(ContextMenu menu, View v,
 			ContextMenuInfo menuInfo) {
 		super.onCreateContextMenu(menu, v, menuInfo);
 		menu.add(0, NOTIFY_ID, 0, R.string.alarm);
 	}
 	
 	/*
 	 * onSelected - Context menu is a popup from a longpress on a list item.
 	 * @see android.app.Activity#onContextItemSelected(android.view.MenuItem)
 	 */
 	@Override
 	public boolean onContextItemSelected(MenuItem item) {
         final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
         selectedId = info.position;
 		
 		switch(item.getItemId()) {
 		case NOTIFY_ID:
 			showDialog(DIALOG_NOTIFICATION);
 			return true;
 		}
 		return super.onContextItemSelected(item);
 	}
 	
 	
 	
 	/*
 	 * Click on a list item
 	 * @see android.app.ListActivity#onListItemClick(android.widget.ListView, android.view.View, int, long)
 	 */
 	@Override
 	protected void onListItemClick(ListView l, View v, int position, long id) {
 		RealtimeData data = (RealtimeData) realtimeList.getItem(position);
 		final long minutesDelayed = (data.expectedDeparture - data.aimedDeparture) / (60 * 1000);
 		
 
 		// Not including departure platform, as all stations have them, and they currently dont make a lot of sense.
 		//    - Currently used internally for sorting? (seem to also indicate directional reference).
 		//    - Might be able to in the future sort by direction with this variable?
 		//				getText(R.string.platform) + " " + data.departurePlatform + "\n" +
 		String info = data.destination + "\n" +
 			minutesDelayed + "m " + getText(R.string.late);
 		if (data.extra != null) {
 			info = info + "\n" + data.extra;
 		}
 		Toast.makeText(this, info, Toast.LENGTH_SHORT).show();
 	}
 
 	/*
 	 * onPause - stop anything needed.
 	 * @see android.app.Activity#onPause()
 	 */
 	@Override
 	protected void onPause() {
 		super.onPause();
 	}
 
 	/*
 	 * Resume state, restart search.
 	 * @see android.app.Activity#onResume()
 	 */
 	@Override
 	protected void onResume() {
 		super.onResume();
 		realtimeList.notifyDataSetInvalidated();
 	}
 
 	@Override
 	protected void onSaveInstanceState(Bundle outState) {
 		super.onSaveInstanceState(outState);
 		outState.putParcelable(SearchStationData.PARCELABLE, station);
 		outState.putParcelableArrayList(RealtimeAdapter.KEY_REALTIMELIST, realtimeList.getList());
 	}
 }
 
 class RealtimeAdapter extends BaseAdapter {
 	public static final String KEY_REALTIMELIST = "realtimelist";
 	private LayoutInflater inflater;
 	private ArrayList<RealtimeData> items = new ArrayList<RealtimeData>();
 	private Context context;
 	
 	public RealtimeAdapter(Context context) {
 		inflater = LayoutInflater.from(context);
 		this.context = context;
 	}
 	
 	/*
 	 * Simple functions dealing with adding/setting items. 
 	 */
 	public ArrayList<RealtimeData> getList() { return items; }
 	public void setList(ArrayList<RealtimeData> items) { this.items = items; }
 	public void addItem(RealtimeData item) {
 		/*
 		 * First search through and check if any previous match has the same line and destination.
 		 * If it does, add us to that instead of making a huge list of duplicates.
 		 */
 		for (RealtimeData d : items) {
 			if (d.destination.equals(item.destination) && d.line.equals(item.line)) {
 				d.arrivalList.add(item);
 				return;
 			}
 		}
 		
 		items.add(item);
 	}
 	
 	/*
 	 * Standard android.widget.Adapter items, self explanatory.
 	 */
 	@Override
 	public int getCount() {	return items.size(); }
 	@Override
 	public Object getItem(int pos) { return items.get(pos); }
 	@Override
 	public long getItemId(int pos) { return pos; }
 	
 	/*
 	 * Function to render time to a text view, this includes rendering color coding.
 	 */
 	public void renderTimeToTextView(RealtimeData data, TextView out) {
 		if (!data.realtime || data.expectedArrival == 0) {
 			if (!data.realtime) {
 				out.setText("" + context.getText(R.string.ca) + " " + HelperFunctions.renderTime(context, data.aimedArrival));
 			} else {
 				out.setText(HelperFunctions.renderTime(context, data.aimedArrival));
 			}
 		
 			final int color = Color.rgb(255, 255, 255);
 			out.setTextColor(color);
 		} else {
 			out.setText(HelperFunctions.renderTime(context, data.expectedArrival));
 		
 			// Color the time text
 			final long diffMinutes = (data.expectedArrival - data.aimedArrival) / (60 * 1000);
 			final int colorValue = (int)(255 * diffMinutes / 9); // Compute a rgb value from diffMinutes, "max" color range is 9min, so >9 is completely red.
 			final int color = Color.rgb(colorValue, 255 - colorValue, 0);
 			out.setTextColor(color);
 		}
 	}
 	
 	/*
 	 * Setup the view
 	 * @see android.widget.Adapter#getView(int, android.view.View, android.view.ViewGroup)
 	 */
 	@Override
 	public View getView(int pos, View convertView, ViewGroup arg2) {
 		/*
 		 * Setup holder, for performance and readability.
 		 */
 		ViewHolder holder;
 		if (convertView == null) {
 			/*
 			 * New view, inflate and setup holder.
 			 */
 			convertView = inflater.inflate(R.layout.realtime_list, null);
 			
 			holder = new ViewHolder();
 			holder.destination = (TextView) convertView.findViewById(R.id.destination);
 			holder.line = (TextView) convertView.findViewById(R.id.line);
 			holder.time = (TextView) convertView.findViewById(R.id.time);
 			holder.nextDepartures = (TextView) convertView.findViewById(R.id.nextDepartures);
 			
 			convertView.setTag(holder);
 		} else {
 			/*
 			 * Old view found, we can reuse that instead of inflating.
 			 */
 			holder = (ViewHolder) convertView.getTag();
 		}
 		
 		/*
 		 * Render data to view.
 		 */
 		final RealtimeData data = items.get(pos);
 		if (data.destination.equals(data.line)) {
 			holder.destination.setText("");
 			holder.line.setText(data.line);			
 		} else {
 			holder.destination.setText(data.destination);
 			holder.line.setText(data.line);
 		}
 		
 		renderTimeToTextView(data, holder.time);
 		
 		/*
 		 * Render list of coming departures
 		 */
 		if (data.arrivalList.size() > 0) {
 			int size = data.arrivalList.size() - 1;
 			String nextDepartures = HelperFunctions.renderTime(context, data.arrivalList.get(0).aimedArrival);
 			for (int i = 1; i < size; i++) {
 				final RealtimeData nextData = data.arrivalList.get(i);
 				nextDepartures = nextDepartures + ",  " + HelperFunctions.renderTime(context, nextData.aimedArrival);
 			}
 			holder.nextDepartures.setText(nextDepartures);
 			holder.nextDepartures.setVisibility(View.VISIBLE);
 		} else {
 			holder.nextDepartures.setVisibility(View.GONE);
 		}
 		
 		return convertView;
 	}
 	
 	/*
 	 * Class for caching the view.
 	 */
 	static class ViewHolder {
 		TextView line;
 		TextView destination;
 		TextView time;
 		TextView nextDepartures;
 		
 	}
 };
