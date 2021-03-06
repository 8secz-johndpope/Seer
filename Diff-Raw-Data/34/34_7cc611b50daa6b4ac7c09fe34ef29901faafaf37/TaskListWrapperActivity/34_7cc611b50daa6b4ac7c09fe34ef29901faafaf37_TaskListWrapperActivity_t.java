 package com.todoroo.astrid.activity;
 
 import android.os.Bundle;
 import android.support.v4.app.ActionBar;
 import android.support.v4.app.Fragment;
 import android.widget.ArrayAdapter;
 
 import com.timsu.astrid.R;
 import com.todoroo.andlib.utility.AndroidUtilities;
 import com.todoroo.astrid.api.Filter;
 import com.todoroo.astrid.api.FilterListItem;
 import com.todoroo.astrid.service.ThemeService;
 
 public class TaskListWrapperActivity extends AstridWrapperActivity {
 
    public static final String TOKEN_SELECTED_FILTER = "selectedFilter";
    private int selectionToSet;

     private ArrayAdapter<FilterListItem> listDropdownAdapter;
     /**
 	 * @see android.app.Activity#onCreate(Bundle)
 	 */
 	@Override
 	protected void onCreate(Bundle savedInstanceState) {
 	    ThemeService.applyTheme(this);
 		super.onCreate(savedInstanceState);
 		setContentView(R.layout.task_list_wrapper_activity);
 
 		Filter savedFilter = getIntent().getParcelableExtra(TaskListActivity.TOKEN_FILTER);
 		setupTasklistFragmentWithFilter(savedFilter);
 		setupFilterlistFragment();
 
 		ActionBar actionBar = getSupportActionBar();
 		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
 		actionBar.setDisplayOptions(0, ActionBar.DISPLAY_SHOW_TITLE);

		if (savedInstanceState != null) {
		    selectionToSet = savedInstanceState.getInt(TOKEN_SELECTED_FILTER);
		}

 	}
 
     /* (non-Javadoc)
      * @see android.support.v4.app.FragmentActivity#onResume()
      */
     @Override
     protected void onResume() {
         super.onResume();
 
         Fragment frag = getFilterListFragment();
         if (frag != null) {
             mMultipleFragments = true;
         } else {
             mMultipleFragments = false;
         }
     }
 
     public void updateDropdownNav(ArrayAdapter<FilterListItem> arrayAdapter) {
         listDropdownAdapter = arrayAdapter;
         ActionBar actionBar = getSupportActionBar();
         actionBar.setListNavigationCallbacks(arrayAdapter, new ActionBar.OnNavigationListener() {
             @Override
             public boolean onNavigationItemSelected(int itemPosition, long itemId) {
                 onFilterItemClicked(listDropdownAdapter.getItem(itemPosition));
                 return true;
             }
         });
        if (selectionToSet != -1 && selectionToSet < listDropdownAdapter.getCount()) {
            selectionToSet = -1;
            getSupportActionBar().setSelectedNavigationItem(selectionToSet);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle icicle) {
        icicle.putInt(TOKEN_SELECTED_FILTER, getSupportActionBar().getSelectedNavigationIndex());
        super.onSaveInstanceState(icicle);
     }
 
    public void setSelectedItem(FilterListItem item) {
        int position = listDropdownAdapter.getPosition(item);
        getSupportActionBar().setSelectedNavigationItem(position);
     }
 
     @Override
     public void finish() {
         super.finish();
         AndroidUtilities.callOverridePendingTransition(this, R.anim.slide_right_in, R.anim.slide_right_out);
     }
 }
