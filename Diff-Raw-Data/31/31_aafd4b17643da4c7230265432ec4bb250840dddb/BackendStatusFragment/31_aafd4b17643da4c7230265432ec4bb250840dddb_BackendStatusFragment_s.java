 /**
  * This file is part of MythTV Android Frontend
  *
  * MythTV Android Frontend is free software: you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  *
  * MythTV Android Frontend is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with MythTV Android Frontend.  If not, see <http://www.gnu.org/licenses/>.
  *
  * This software can be found at <https://github.com/MythTV-Clients/MythTV-Android-Frontend/>
  */
 package org.mythtv.client.ui;
 
 import java.util.List;
 import org.mythtv.R;
 import org.mythtv.client.ui.preferences.LocationProfile;
 import org.mythtv.client.ui.util.ProgramHelper;
 import org.mythtv.services.api.dvr.Encoder;
 import org.mythtv.services.api.dvr.Program;
 import org.mythtv.services.api.status.Job;
 
 import android.app.Fragment;
 import android.app.FragmentTransaction;
import android.content.Context;
 import android.content.res.Configuration;
 import android.os.Bundle;
 import android.util.Log;
 import android.view.LayoutInflater;
 import android.view.View;
import android.view.View.MeasureSpec;
 import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
 import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
 import android.widget.TextView;
 
 
 public class BackendStatusFragment extends AbstractMythFragment {
 
 	private static final String TAG = BackendStatusFragment.class.getSimpleName();
 	public static final String BACKEND_STATUS_FRAGMENT_NAME = "org.mythtv.client.ui.BackendStatusFragment";
 	
 	private ProgramHelper mProgramHelper = ProgramHelper.getInstance();
 	private View mView;
 	private LocationProfile mLocationProfile;
 	private LinearLayout mLinearLayoutEncoders;
 	private LinearLayout mLinearLayoutUpcomingRecs;
 	private LinearLayout mLinearLayoutJobQueue;
 	private TextView mTextViewEncodersEmpty;
 	private TextView mTextViewJobQueueEmpty;
 	private TextView mTextViewUpcomingRecEmpty;
 
	
 	/* (non-Javadoc)
 	 * @see android.support.v4.app.Fragment#onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)
 	 */
 	@Override
 	public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState ) {
 		Log.d( TAG, "onCreateView : enter" );
 		
 		mView = inflater.inflate( R.layout.fragment_backend_status, null, false );
 		
 		mLinearLayoutEncoders = (LinearLayout)mView.findViewById(R.id.linearlayout_encoders_list);
 		mLinearLayoutUpcomingRecs = (LinearLayout)mView.findViewById(R.id.linearlayout_upcoming_recordings_list);
 		mLinearLayoutJobQueue = (LinearLayout)mView.findViewById(R.id.linearlayout_job_queue);
 		mTextViewEncodersEmpty = (TextView)mView.findViewById(R.id.textview_encoders_list_empty);
 		mTextViewJobQueueEmpty = (TextView)mView.findViewById(R.id.textview_job_queue_empty);
 		mTextViewUpcomingRecEmpty = (TextView)mView.findViewById(R.id.textview_upcoming_rec_empty);
 		
 		Log.d( TAG, "onCreateView : exit" );
 		return mView;
 	}
 	
 	/* (non-Javadoc)
 	 * @see android.support.v4.app.Fragment#onActivityCreated(android.os.Bundle)
 	 */
 	@Override
 	public void onActivityCreated( Bundle savedInstanceState ) {
 		Log.d( TAG, "onActivityCreated : enter" );
 		super.onActivityCreated( savedInstanceState );
 
 		if( null != mView ) {
 			
 			TextView tView = (TextView) mView.findViewById( R.id.textview_status );
 			if( null != tView ) {
 //				tView.setText( this.getStatusText() );
 			}
 			
 		}
 		
 		Log.d( TAG, "onActivityCreated : exit" );
 	}
 
 	/* (non-Javadoc)
 	 * @see android.support.v4.app.Fragment#onResume()
 	 */
 	@Override
 	public void onResume() {
 		Log.d( TAG, "onResume : enter" );
 		super.onResume();
 
 		if( null != mView ) {
 			
 			TextView tView = (TextView) mView.findViewById( R.id.textview_status );
 			if( null != tView ) {
 				tView.setText( this.getStatusText() );
 			}
 			
 		}
 	
 		Log.d( TAG, "onResume : exit" );
 	}
 	
 	@Override
 	public void onConfigurationChanged(Configuration newConfig) {
 		
 		//re- attach the fragment forcing the view to be recreated.
 		Fragment currentFragment = getFragmentManager().findFragmentByTag(BACKEND_STATUS_FRAGMENT_NAME);
 	    FragmentTransaction fragTransaction = getFragmentManager().beginTransaction();
 	    fragTransaction.detach(currentFragment);
 	    fragTransaction.attach(currentFragment);
 	    fragTransaction.commit();
 		
 		super.onConfigurationChanged(newConfig);
 	}
 
 	// internal helpers
 	
 	private String getStatusText() {
 		Log.v( TAG, "getStatusText : enter" );
 
 		mLocationProfile = mLocationProfileDaoHelper.findConnectedProfile( getActivity() );
 		
 		if( null == mLocationProfile ) {
 			Log.v( TAG, "getStatusText : exit, no connected profiles found" );
 
 			return "Backend profile is not selected";
 		}
 		
 		BackendStatusTask backendTask = new BackendStatusTask();
 		backendTask.execute();
 		
 		Log.v( TAG, "getStatusText : exit" );
 		return ( mLocationProfile.isConnected() ? "Connected to " : "NOT Connected to " ) + mLocationProfile.getName();
 	}
 	
 	/*
      *  (non-Javadoc)
      *  
      *  Called at the end of BackendStatusTask.onPostExecute() when result is not null.
      */
 	@Override
     protected void onBackendStatusUpdated(org.mythtv.services.api.status.Status result){
     	
 		LayoutInflater inflater = LayoutInflater.from(this.getActivity());
 		
 		//clear lists
 		mLinearLayoutEncoders.removeAllViews();
 		mLinearLayoutUpcomingRecs.removeAllViews();
 		mLinearLayoutJobQueue.removeAllViews();
 		
 		// Set encoder list
 		List<Encoder> encoders = result.getEncoders().getEncoders();
 		if (null != encoders) {
 			mLinearLayoutEncoders.setVisibility(View.VISIBLE);
 			mTextViewEncodersEmpty.setVisibility(View.GONE);
 			
 			for(int i=0; i<encoders.size(); i++){
 				mLinearLayoutEncoders.addView(this.getEncoderView(inflater, encoders.get(i)));
 			}
 		}else{
 			mLinearLayoutEncoders.setVisibility(View.GONE);
 			mTextViewEncodersEmpty.setVisibility(View.VISIBLE);
 		}
 		
 		// Set Upcoming recordings list
 		List<Program> programs = result.getScheduled().getPrograms();
		if(null != programs){
 			mLinearLayoutUpcomingRecs.setVisibility(View.VISIBLE);
 			mTextViewUpcomingRecEmpty.setVisibility(View.GONE);
 			
 			for(int i=0; i<programs.size(); i++){
 				mLinearLayoutUpcomingRecs.addView(this.getUpcomingRecView(inflater, programs.get(i)));
 			}
 		}else{
 			mLinearLayoutUpcomingRecs.setVisibility(View.GONE);
 			mTextViewUpcomingRecEmpty.setVisibility(View.VISIBLE);
 		}
 		
 		List<Job> jobs = result.getJobQueue().getJobs();
 		if(null != jobs){
 			mLinearLayoutJobQueue.setVisibility(View.VISIBLE);
 			mTextViewJobQueueEmpty.setVisibility(View.GONE);
 			
 			for(int i=0; i<jobs.size(); i++){
 				mLinearLayoutJobQueue.addView(this.getJobView(inflater, jobs.get(i)));
 			}
 		}else{
 			mLinearLayoutJobQueue.setVisibility(View.GONE);
 			mTextViewJobQueueEmpty.setVisibility(View.VISIBLE);
 		}
     }
 	
 	
 	private View getEncoderView(LayoutInflater inflater, Encoder encoder) {
 		
 		View view = (View)inflater.inflate(R.layout.backend_status_encoder_list_item, null, false);
 		
 		//set device label
 		TextView tView = (TextView)view.findViewById(R.id.textView_encoder_devicelabel);
 		if(null != tView) {
 			tView.setText(Integer.toString(encoder.getId()) + " - " + encoder.getDeviceLabel());
 		}
 		
 		//set device host
 		tView = (TextView)view.findViewById(R.id.textView_encoder_host);
 		if(null != tView) {
 			tView.setText(encoder.getHostname());
 		}
 		
 		//set device recording status
 		tView = (TextView)view.findViewById(R.id.textView_encoder_rec_status);
 		if(null != tView) {
 			Program rec = encoder.getRecording();
 			if(null != rec){
 				tView.setText(rec.getTitle() + " on " + rec.getChannelInfo().getChannelName());
 				//+ rec.getEndTime().toString("hh:mm") );
 			}else{
 				tView.setText("Inactive");
 			}
 		}
 		
 		
 		return view;
 		
 	}
 	
 	public View getUpcomingRecView(LayoutInflater inflater, Program program) {
 
 		View view = (View)inflater.inflate(R.layout.backend_status_upcoming_list_item, null, false);
 		
 		//set category color
 		View category = view.findViewById(R.id.upcoming_category);
 		if(null != category) category.setBackgroundColor(mProgramHelper.getCategoryColor( program.getCategory() ));
 		
 		//set upcoming_title
 		TextView tView = (TextView)view.findViewById(R.id.upcoming_title);
 		if(null != tView) {
 			tView.setText(program.getTitle());
 		}
 		
 		//set upcoming_sub_title
 		tView = (TextView)view.findViewById(R.id.upcoming_sub_title);
 		if(null != tView) {
 			tView.setText(program.getSubTitle());
 		}
 		
 		//set upcoming_channel
 		tView = (TextView)view.findViewById(R.id.upcoming_channel);
 		if(null != tView) {
 			tView.setText(program.getChannelInfo().getCallSign());
 		}
 		
 		//set upcoming_start_time
 		tView = (TextView)view.findViewById(R.id.upcoming_start_time);
 		if(null != tView) {
			tView.setText(program.getStartTime().toString("hh:mm"));
 		}
 		
 		//set upcoming_duration
 		tView = (TextView)view.findViewById(R.id.upcoming_duration);
 		if(null != tView) {
 			tView.setText(Long.toString(program.getDurationInMinutes()));
 		}
 		
 		return view;
 		
 	}
 	
 	
 	private View getJobView(LayoutInflater inflater, Job job) {
 
 		View view = (View)inflater.inflate(R.layout.backend_status_job_list_item, null, false);
 
 		//set title
 		TextView tView = (TextView)view.findViewById(R.id.textView_job_program_title);
 		if(null != tView) {
 			tView.setText(job.getProgram() != null ? job.getProgram().getTitle() : "");
 		}
 		
 		//set type
 		tView = (TextView)view.findViewById(R.id.textView_job_type);
 		if(null != tView) {
 			tView.setText(job.getType() != null ? job.getType().name() : "");
 		}
 		
 		//set status
 		tView = (TextView)view.findViewById(R.id.textView_job_status);
 		if(null != tView) {
 			tView.setText(getJobStatusStr(job.getStatus()));
 		}
 		
 		return view;
 		
 	}
 	
 	
 	private String getJobStatusStr(Job.Status status){
 		switch(status){
 		case ABORTED:
 			return getString(R.string.job_queue_status_aborted);
 		case ABORTING:
 			return getString(R.string.job_queue_status_aborting);
 		case CANCELLED:
 			return getString(R.string.job_queue_status_cancelled);
 		case DONE:
 			return getString(R.string.job_queue_status_done);
 		case ERRORED:
 			return getString(R.string.job_queue_status_errored);
 		case ERRORING:
 			return getString(R.string.job_queue_status_erroring);
 		case FINISHED:
 			return getString(R.string.job_queue_status_finished);
 		case NO_FLAGS:
 			return getString(R.string.job_queue_status_no_flags);
 		case PAUSED:
 			return getString(R.string.job_queue_status_paused);
 		case PENDING:
 			return getString(R.string.job_queue_status_pending);
 		case QUEUED:
 			return getString(R.string.job_queue_status_queued);
 		case RETRY:
 			return getString(R.string.job_queue_status_retry);
 		case RUNNING:
 			return getString(R.string.job_queue_status_running);
 		case STARTING:
 			return getString(R.string.job_queue_status_starting);
 		case STOPPING:
 			return getString(R.string.job_queue_status_stopping);
 		default:
 			return getString(R.string.job_queue_status_unknown);
 		}
 	}
 
 }
