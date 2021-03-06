 package edu.mit.mitmobile2.facilities;
 
 import android.content.Context;
 import android.content.Intent;
 import android.database.Cursor;
 import android.os.AsyncTask;
 import android.os.Bundle;
 import android.os.Handler;
 import android.os.Message;
 import android.util.Log;
 import android.view.KeyEvent;
 import android.view.Menu;
 import android.view.View;
 import android.widget.AdapterView;
 import android.widget.AdapterView.OnItemClickListener;
 import android.widget.ListView;
 import android.widget.TextView;
 import edu.mit.mitmobile2.FullScreenLoader;
 import edu.mit.mitmobile2.Global;
 import edu.mit.mitmobile2.Module;
 import edu.mit.mitmobile2.ModuleActivity;
 import edu.mit.mitmobile2.R;
 import edu.mit.mitmobile2.TwoLineActionRow;
 import edu.mit.mitmobile2.objs.FacilitiesItem.CategoryRecord;
 
 public class FacilitiesProblemLocationActivity extends ModuleActivity {
 
 	public static final String TAG = "FacilitiesProblemLocationActivity";
 
 	Context mContext;
 	final FacilitiesDB db = FacilitiesDB.getInstance(this);
 	FullScreenLoader mLoader;
 	TwoLineActionRow useMyLocationActionRow;
 	
 	Handler mFacilitiesLoadedHandler = new Handler() {
 		@Override
 		public void handleMessage(Message msg) {
 			if(msg.arg1 == FacilitiesDB.STATUS_CATEGORIES_SUCCESSFUL) {
 				Log.d(TAG,"received success message for categories");
 			} 
 			else if(msg.arg1 == FacilitiesDB.STATUS_LOCATIONS_SUCCESSFUL) {
 				Log.d(TAG,"received success message for categories");
 			} 
 
 			else if(msg.arg1 == FacilitiesDB.STATUS_PROBLEM_TYPES_SUCCESSFUL) {
 				Log.d(TAG,"received success message for problem types, launching next activity");
 				
 				CategoryAdapter adapter = new CategoryAdapter(FacilitiesProblemLocationActivity.this, db.getCategoryCursor());
 				ListView listView = (ListView) findViewById(R.id.facilitiesProblemLocationListView);
 				listView.setAdapter(adapter);
 				listView.setVisibility(View.VISIBLE);
 				
 				listView.setOnItemClickListener(new OnItemClickListener() {
 					@Override
 					public void onItemClick(AdapterView<?> parent, View view, int position,
 							long id) {
 						CategoryRecord category = db.getCategory(position);
 						Log.d(TAG,"position = " + position + " id = " + category.id + " name = " + category.name);
 						// save the selected category
 						Global.sharedData.getFacilitiesData().setLocationCategory(category.id);
 						Intent intent = new Intent(mContext, FacilitiesLocationsForCategoryActivity.class);
 						startActivity(intent);          
 					}
 				});
 
 
 				mLoader.setVisibility(View.GONE);
 			}
 			else {
 				mLoader.showError();
 			}
 		}		
 	};
 	
 	/** Called when the activity is first created. */
 	@Override
 	public void onCreate(Bundle savedInstanceState) {
 		super.onCreate(savedInstanceState);
 		Log.d(TAG,"onCreate()");		
 		mContext = this;
 		createViews();		
 	}
 
 	public void createViews() {
 		
 		setContentView(R.layout.facilities_problem_location);
 		mLoader = (FullScreenLoader) findViewById(R.id.facilitiesLoader);
 		mLoader.showLoading();
 		new DatabaseUpdater().execute(""); 
 		
         // Set up location search
 
         // Set up use my location button
 		useMyLocationActionRow = (TwoLineActionRow) findViewById(R.id.facilitiesUseMyLocationActionRow);
 		String title1 = "Use My Location";
 		String title2 = "";
 		useMyLocationActionRow.setTitle(title1 + " " + title2, TextView.BufferType.SPANNABLE);
 		useMyLocationActionRow.setOnClickListener(new View.OnClickListener() {
 			
 			@Override
 			public void onClick(View v) {
 				Intent intent = new Intent(mContext, FacilitiesUseMyLocationActivity.class);
 				startActivity(intent);
 			}
 		});
 		
		facilitiesTextLocation = (AutoCompleteTextView) findViewById(R.id.facilitiesTextLocation);
 		facilitiesTextLocation.setAdapter(new LocationsSearchCursorAdapter(this, db));
 		facilitiesTextLocation.setOnItemClickListener(new AdapterView.OnItemClickListener() {
 
 			@Override
 			public void onItemClick(AdapterView<?> listView, View row, int position,
 					long id) {
 				
 				// have no idea what this method should actually do
 				// this is completey a placeholder
 				Cursor cursor = (Cursor) listView.getItemAtPosition(position);
 				int titleIndex = cursor.getColumnIndex(FacilitiesDB.LocationTable.NAME);
 				String name = cursor.getString(titleIndex);
 				Log.d(TAG, "Location Selected: " + name);
 				
 			}
 		});
         		
 	}
 
 	public boolean onKeyDown(int keyCode, KeyEvent event){
 	    if(keyCode == KeyEvent.KEYCODE_BACK) {
 	            Intent intent = new Intent(mContext, FacilitiesActivity.class);              
 	            startActivity(intent);          
 	            finish();
 	            return true;
 	    }
 	    return false;
 	}
 
 
 	private class DatabaseUpdater extends AsyncTask<String, Void, String> {
 		
 		@Override
 		protected void onPreExecute() {
 		}
 
 		@Override
 		protected String doInBackground(String... msg) {
 			// Executed in worker thread
 			String result = "";
 			try {
 				FacilitiesDB.updateCategories(mContext, mFacilitiesLoadedHandler );
 				FacilitiesDB.updateLocations(mContext, mFacilitiesLoadedHandler );
 				FacilitiesDB.updateProblemTypes(mContext, mFacilitiesLoadedHandler );
 				result = "success";
 			} catch (Exception e) {
 			}
 			return result;
 		}
 
 		@Override
 		protected void onPostExecute(String result) {
 			// Executed in UI thread
 			//dialog.dismiss();
 		}
 	}
 	
 	@Override
 	public void onWindowFocusChanged(boolean hasFocus) {
 		//mBackgroundView.startBackgroundAnimation();
 	}
 		
 	@Override
 	protected void prepareActivityOptionsMenu(Menu menu) {
 	}
 	
 
 	@Override
 	protected Module getModule() {
 		// TODO Auto-generated method stub
 		return null;
 	}
 	@Override
 	public boolean isModuleHomeActivity() {
 		// TODO Auto-generated method stub
 		return false;
 	}
 
 	
 	
 }
 	
