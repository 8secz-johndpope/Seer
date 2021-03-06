 package fr.neamar.cinetime;
 
 import android.annotation.SuppressLint;
 import android.annotation.TargetApi;
 import android.content.Intent;
 import android.os.Build;
 import android.os.Bundle;
 import android.support.v4.app.Fragment;
 import android.support.v4.app.FragmentActivity;
 import android.view.Menu;
 import android.view.MenuInflater;
 import android.view.MenuItem;
 import android.view.Window;
 import android.widget.Toast;
 
 import com.google.analytics.tracking.android.EasyTracker;
 
 import fr.neamar.cinetime.fragments.DetailsEmptyFragment;
 import fr.neamar.cinetime.fragments.DetailsFragment;
 import fr.neamar.cinetime.fragments.MoviesFragment;
 
 public class MoviesActivity extends FragmentActivity implements MoviesFragment.Callbacks {
 
 	private boolean mTwoPane;
 	private MoviesFragment moviesFragment;
 	private DetailsFragment detailsFragment;
 	private String theater;
 	private MenuItem shareItem;
 
 	@TargetApi(14)
 	@Override
 	public void onCreate(Bundle savedInstanceState) {
 		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
 		super.onCreate(savedInstanceState);
 		setContentView(R.layout.activity_movies_list);
 		mTwoPane = getResources().getBoolean(R.bool.mTwoPane);
 		theater = getIntent().getStringExtra("theater");
 		setTitle(getIntent().getStringExtra("theater"));
 		if (mTwoPane) {
 			if (detailsFragment == null) {
 				getSupportFragmentManager().beginTransaction()
 						.replace(R.id.file_detail_container, new DetailsEmptyFragment()).commit();
 			} else {
 				getSupportFragmentManager().beginTransaction()
 						.replace(R.id.file_detail_container, detailsFragment).commit();
 			}
 		}
 		// Title in action bar brings back one level
 		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
 			getActionBar().setHomeButtonEnabled(true);
 			getActionBar().setDisplayHomeAsUpEnabled(true);
 		}
 	}
 
 	@Override
 	public void setIsLoading(Boolean isLoading) {
 		setProgressBarIndeterminateVisibility(isLoading);
 	}
 
 	@Override
 	public boolean onCreateOptionsMenu(Menu menu) {
 		MenuInflater inflater = getMenuInflater();
 		if (mTwoPane) {
 			inflater.inflate(R.menu.activity_details, menu);
 			shareItem = menu.findItem(R.id.menu_share);
 			if (detailsFragment == null) {
				shareItem.setEnabled(false);
				shareItem.setVisible(false);
 			} else {
				shareItem.setVisible(true);
				shareItem.setEnabled(true);
 			}
 			return true;
 		}
 		return false;
 	}
 
 	@Override
 	protected void onResume() {
 		super.onResume();
 		if (mTwoPane) {
 			if (detailsFragment == null) {
 				getSupportFragmentManager().beginTransaction()
 						.replace(R.id.file_detail_container, new DetailsEmptyFragment()).commit();
 				desactivateShare();
 			} else {
 				activateShare();
 			}
 		}
 	}
 
 	@Override
 	public boolean onOptionsItemSelected(MenuItem item) {
 		// Click on title in actionbar
 		switch (item.getItemId()) {
 		case android.R.id.home:
 			moviesFragment.clear();
 			finish();
 			return true;
 		case R.id.menu_share:
 			detailsFragment.shareMovie();
 			return true;
 		default:
 			return super.onOptionsItemSelected(item);
 		}
 	}
 
 	@Override
 	public void setFragment(Fragment fragment) {
 		if (fragment instanceof MoviesFragment) {
 			this.moviesFragment = (MoviesFragment) fragment;
 		} else if (fragment instanceof DetailsFragment) {
 			this.detailsFragment = (DetailsFragment) fragment;
 			activateShare();
 		}
 	}
 
 	@Override
 	public void onBackPressed() {
 		if (mTwoPane && detailsFragment != null) {
 			getSupportFragmentManager().beginTransaction()
 					.replace(R.id.file_detail_container, new DetailsEmptyFragment()).commit();
 			detailsFragment = null;
 			desactivateShare();
 			setTitle("Séances " + getIntent().getStringExtra("theater"));
 		} else {
 			moviesFragment.clear();
 			super.onBackPressed();
 		}
 	}
 
 	@Override
 	public void onItemSelected(int position, Fragment source) {
 		if (source instanceof MoviesFragment) {
 			if (mTwoPane) {
 				Bundle arguments = new Bundle();
 				arguments.putInt(DetailsFragment.ARG_ITEM_ID, position);
 				arguments.putString(DetailsFragment.ARG_THEATER_NAME, theater);
 				detailsFragment = new DetailsFragment();
 				detailsFragment.setArguments(arguments);
 				getSupportFragmentManager().beginTransaction()
 						.replace(R.id.file_detail_container, detailsFragment).commit();
 			} else {
 				Intent details = new Intent(this, DetailsActivity.class);
 				details.putExtra(DetailsFragment.ARG_ITEM_ID, position);
 				details.putExtra(DetailsFragment.ARG_THEATER_NAME, theater);
 				startActivity(details);
 			}
 		} else if (source instanceof DetailsFragment) {
 			startActivity(new Intent(this, PosterViewerActivity.class));
 		}
 	}
 
 	@Override
 	protected void onStart() {
 		super.onStart();
 		EasyTracker.getInstance().activityStart(this);
 	}
 
 	@Override
 	protected void onStop() {
 		super.onStop();
 		EasyTracker.getInstance().activityStop(this);
 	}
 
 	@Override
 	public void finishNoNetwork() {
 		Toast.makeText(
 				this,
 				"Impossible de télécharger les données. Merci de vérifier votre connexion ou de réessayer dans quelques minutes.",
 				Toast.LENGTH_SHORT).show();
 		finish();
 	}
 
 	@SuppressLint("NewApi")
 	private void activateShare() {
 		if (shareItem != null) {
 			shareItem.setVisible(true);
 			shareItem.setEnabled(true);
 			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
 				invalidateOptionsMenu();
 			}
 		}
 	}
 
 	@SuppressLint("NewApi")
 	private void desactivateShare() {
 		if (shareItem != null) {
 			shareItem.setEnabled(false);
 			shareItem.setVisible(false);
 			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
 				invalidateOptionsMenu();
 			}
 		}
 	}
 }
