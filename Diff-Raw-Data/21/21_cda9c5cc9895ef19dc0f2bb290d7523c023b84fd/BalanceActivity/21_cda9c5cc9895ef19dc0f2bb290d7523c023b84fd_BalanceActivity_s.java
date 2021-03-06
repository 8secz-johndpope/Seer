 package com.nasageek.UTilities;
 
 
 import java.util.ArrayList;
 import java.util.List;
 import java.util.Vector;
 
 import android.content.Intent;
 import android.os.Build;
 import android.os.Bundle;
 import android.support.v4.app.Fragment;
 import android.support.v4.app.FragmentTransaction;
 
 import android.support.v4.view.ViewPager;
 import android.util.Log;
 import com.actionbarsherlock.app.ActionBar;
 import com.actionbarsherlock.app.ActionBar.Tab;
 import com.actionbarsherlock.app.SherlockFragment;
 import com.actionbarsherlock.app.SherlockFragmentActivity;
 import com.actionbarsherlock.view.MenuItem;
 import com.viewpagerindicator.TabPageIndicator;
 
 import android.widget.TextView;
 
 
 
 public class BalanceActivity extends SherlockFragmentActivity
 {	
 	private ConnectionHelper ch;
 	
 	ArrayList<String> dtransactionlist, btransactionlist, balancelist;
 	String[] dtransactionarray, btransactionarray;
 	int count;
 	TextView tv1, tv2,tv3,tv4;
 	ActionBar actionbar;
 	String bevobalance="", dineinbalance="No Dine In Dollars? What kind of animal are you?";
 	
 	@Override
 	public void onCreate(Bundle savedInstanceState)
 	{
 		super.onCreate(savedInstanceState);
 		setContentView(R.layout.balance_layout);
 		this.initialisePaging();
 		
 		
 		ch = new ConnectionHelper(this);
 	
 		actionbar = getSupportActionBar();
 		actionbar.setTitle("Transactions");
 		actionbar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
 		actionbar.setHomeButtonEnabled(true);
 		actionbar.setDisplayHomeAsUpEnabled(true);
 		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)	
     		actionbar.setBackgroundDrawable(this.getResources().getDrawable(R.drawable.actionbar_bg));
 		
 	/*	 actionbar.addTab(actionbar.newTab()
 		            .setText("Dinein")
 		            .setTabListener(new TabListener<DineinFragment>(
 		                    this, "dinein", DineinFragment.class, null)));
 
 		    actionbar.addTab(actionbar.newTab()
 		            .setText("Bevo Bucks")
 		            .setTabListener(new TabListener<BevoFragment>(
 		                    this, "bevo", BevoFragment.class, null)));*/
 		
 		Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler(){
 			public void uncaughtException(Thread thread, Throwable ex)
 			{
 				// TODO Auto-generated method stub
 				Log.e("UNCAUGHT",ex.getMessage(),ex);
 				finish();
 				return;
 			}});
 	}
 	
 	 /** maintains the pager adapter*/
 	
 	    private PagerAdapter mPagerAdapter;
 	
 	    /* (non-Javadoc)
 	
 	     * @see android.support.v4.app.FragmentActivity#onCreate(android.os.Bundle)
 	
 	     */
 	    /**
 	
 	     * Initialise the fragments to be paged
 	
 	     */
 	
 	    private void initialisePaging() {
 	
 	 
 	
 	        List<SherlockFragment> fragments = new Vector<SherlockFragment>();
 	        Bundle args = new Bundle(1);
 	        args.putString("title", "Dinein");
 	        fragments.add((SherlockFragment)SherlockFragment.instantiate(this, DineinFragment.class.getName(), args));
 	        args = new Bundle(1);
 	        args.putString("title", "Bevo Bucks");
 	        fragments.add((SherlockFragment)SherlockFragment.instantiate(this, BevoFragment.class.getName(), args));
 	
 	      
 	        this.mPagerAdapter  = new PagerAdapter(getSupportFragmentManager(), fragments);	
 	        ViewPager pager = (ViewPager)findViewById(R.id.viewpager);
 	        pager.setPageMargin(2);
 	        pager.setAdapter(this.mPagerAdapter);
 	        TabPageIndicator tabIndicator = (TabPageIndicator)findViewById(R.id.titles);
 			tabIndicator.setViewPager(pager);
 			
 	    }
 	
 	
 
 	
 	
 	
 	@Override
 	public boolean onOptionsItemSelected(MenuItem item)
 	{
 	    	int id = item.getItemId();
 	    	switch(id)
 	    	{
 		    	case android.R.id.home:
 		            // app icon in action bar clicked; go home
 		            Intent home = new Intent(this, UTilitiesActivity.class);
 		            home.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
 		            startActivity(home);break;
 	    	}
	    	return true;
 	}
 
 	public static class TabListener<T extends Fragment> implements ActionBar.TabListener 
 	{
         private final SherlockFragmentActivity mActivity;
         private final String mTag;
         private final Class<T> mClass;
         private final Bundle mArgs;
         private Fragment mFragment;
 
         public TabListener(SherlockFragmentActivity activity, String tag, Class<T> clz) {
             this(activity, tag, clz, null);
         }
 
         public TabListener(SherlockFragmentActivity activity, String tag, Class<T> clz, Bundle args) {
             mActivity = activity;
             mTag = tag;
             mClass = clz;
             mArgs = args;
 
             // Check to see if we already have a fragment for this tab, probably
             // from a previously saved state.  If so, deactivate it, because our
             // initial state is that a tab isn't shown.
             mFragment = mActivity.getSupportFragmentManager().findFragmentByTag(mTag);
             if (mFragment != null && !mFragment.isDetached()) {
                 FragmentTransaction ft = mActivity.getSupportFragmentManager().beginTransaction();
                 ft.detach(mFragment);
                 ft.commit();
             }
         }
 
         public void onTabSelected(Tab tab, FragmentTransaction fta) {
         	FragmentTransaction ft = mActivity.getSupportFragmentManager().beginTransaction();
             if (mFragment == null) {
                 mFragment = Fragment.instantiate(mActivity, mClass.getName(), mArgs);
                 ft.add(android.R.id.content, mFragment, mTag);
                 ft.commit();
             } else {
                 ft.attach(mFragment);
                 ft.commit();
             }
         }
 
         public void onTabUnselected(Tab tab, FragmentTransaction fta) {
         	FragmentTransaction ft = mActivity.getSupportFragmentManager().beginTransaction();
         	if (mFragment != null) {
                 ft.detach(mFragment);
                 ft.commit();
             }
         }
 
         public void onTabReselected(Tab tab, FragmentTransaction fta) {
         	
         }
     
 	}
 }
