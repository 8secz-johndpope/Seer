 package com.xargsgrep.portknocker.fragment;
 
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.Comparator;
 import java.util.List;
 
 import android.content.pm.ApplicationInfo;
 import android.content.pm.PackageManager;
 import android.os.AsyncTask;
 import android.os.Bundle;
 import android.support.v4.app.Fragment;
 import android.support.v4.app.FragmentTransaction;
 import android.view.LayoutInflater;
 import android.view.View;
 import android.view.ViewGroup;
 import android.widget.EditText;
 import android.widget.Spinner;
 
 import com.actionbarsherlock.app.SherlockFragment;
 import com.xargsgrep.portknocker.R;
 import com.xargsgrep.portknocker.activity.EditHostActivity;
 import com.xargsgrep.portknocker.adapter.ApplicationArrayAdapter;
 import com.xargsgrep.portknocker.manager.HostDataManager;
 import com.xargsgrep.portknocker.model.Application;
 import com.xargsgrep.portknocker.model.Host;
 
 public class MiscFragment extends SherlockFragment {
 	
 	private static final String DELAY_BUNDLE_KEY = "delay";
 	private static final String LAUNCH_INTENT_BUNDLE_KEY = "launchIntent";
 	
     HostDataManager hostDataManager;
    ProgressDialogFragment dialogFragment;
 	
 	public static MiscFragment newInstance(Long hostId) {
 		MiscFragment fragment = new MiscFragment();
 		if (hostId != null) {
 			Bundle args = new Bundle();
 			args.putLong(EditHostActivity.HOST_ID_BUNDLE_KEY, hostId);
 			fragment.setArguments(args);
 		}
 		return fragment;
 	}
 	
 	@Override
 	public void onCreate(Bundle savedInstanceState) {
 		super.onCreate(savedInstanceState);
 		setRetainInstance(true);
 		hostDataManager = new HostDataManager(getActivity());
 	}
 	
     @Override
     public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
     	super.onCreateView(inflater, container, savedInstanceState);
         return inflater.inflate(R.layout.misc_fragment, container, false);
     }
     
     @Override
     public void onViewCreated(View view, Bundle savedInstanceState) {
     	super.onViewCreated(view, savedInstanceState);
     	
 		EditText delayEditText = getDelayEditText();
     	Bundle args = getArguments();
     	
     	String selectedLaunchIntent = null;
     	if (savedInstanceState != null) {
     		delayEditText.setText(savedInstanceState.getString(DELAY_BUNDLE_KEY));
 			selectedLaunchIntent = savedInstanceState.getString(LAUNCH_INTENT_BUNDLE_KEY);
     	} else if (args != null) {
     		Long hostId = args.getLong(EditHostActivity.HOST_ID_BUNDLE_KEY);
     		Host host = hostDataManager.getHost(hostId);
     		
 			delayEditText.setText(new Integer(host.getDelay()).toString());
 			selectedLaunchIntent = host.getLaunchIntentPackage();
     	}
     	
    	
    	FragmentTransaction ft = getFragmentManager().beginTransaction();
    	Fragment prev = getFragmentManager().findFragmentByTag("dialog");
    	if (prev != null) ft.remove(prev);
    	ft.addToBackStack(null);
    	
		dialogFragment = ProgressDialogFragment.newInstance(getString(R.string.progress_dialog_retrieving_applications));
		dialogFragment.show(ft, "dialog");
		
     	RetrieveInstalledApplicationsTask retrieveAppsTask = new RetrieveInstalledApplicationsTask(selectedLaunchIntent);
     	retrieveAppsTask.execute();
     }
     
     @Override
     public void onSaveInstanceState(Bundle outState) {
     	super.onSaveInstanceState(outState);
     	
 		outState.putString(DELAY_BUNDLE_KEY, getDelayEditText().getText().toString());
 		if (getLaunchIntentSpinner().getSelectedItem() != null)
 			outState.putString(LAUNCH_INTENT_BUNDLE_KEY, ((Application) getLaunchIntentSpinner().getSelectedItem()).getIntent());
     }
     
     public EditText getDelayEditText() {
 		return (EditText) getView().findViewById(R.id.delay_edit);
     }
     
     public Spinner getLaunchIntentSpinner() {
 		return (Spinner) getView().findViewById(R.id.launch_intent);
     }
     
     private class RetrieveInstalledApplicationsTask extends AsyncTask<Void, Void, List<Application>> {
     	String selectedLaunchIntent;
     	
     	public RetrieveInstalledApplicationsTask(String selectedLaunchIntent) {
     		this.selectedLaunchIntent = selectedLaunchIntent;
 		}
     	
     	@Override
     	protected void onPreExecute() {
     	}
     	
 		@Override
 		protected List<Application> doInBackground(Void... params) {
 			PackageManager packageManager = getActivity().getPackageManager();
 			List<ApplicationInfo> installedApplications = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
 			
 	        List<Application> applications = new ArrayList<Application>();
 	        for (ApplicationInfo applicationInfo : installedApplications) {
 	        	if (isSystemPackage(applicationInfo) || packageManager.getLaunchIntentForPackage(applicationInfo.packageName) == null) continue;
 	        	applications.add(new Application(packageManager.getApplicationLabel(applicationInfo).toString(), applicationInfo.loadIcon(packageManager), applicationInfo.packageName));
 	        }
 	        
 	        Collections.sort(applications, new Comparator<Application>() {
 				@Override
 				public int compare(Application app1, Application app2) {
 					return app1.getLabel().compareTo(app2.getLabel());
 				}
 			});
 	        applications.add(0, new Application("None", null, ""));
         
 			return applications;
 		}
 		
 		@Override
 		protected void onPostExecute(List<Application> applications) {
 	        ApplicationArrayAdapter adapter = new ApplicationArrayAdapter(getActivity(), applications);
 	        Spinner launchAppSpinner = getLaunchIntentSpinner();
 	        launchAppSpinner.setAdapter(adapter);
 	        
 	        if (selectedLaunchIntent != null) {
 		        for (int i=0; i<adapter.getCount(); i++) {
 		        	Application application = adapter.getItem(i);
 		        	if (application.getIntent().equals(selectedLaunchIntent)) {
 		        		launchAppSpinner.setSelection(i);
 		        		break;
 		        	}
 		        }
 	        }
 	        
	        if (dialogFragment.isAdded())
		        dialogFragment.dismiss();
 		}
 
 	    private boolean isSystemPackage(ApplicationInfo applicationInfo) {
 	        return ((applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0);
 	    }
     }
 
 }
