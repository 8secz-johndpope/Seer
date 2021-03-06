 package cmupdaterapp.ui;
 
 import java.io.BufferedReader;
 import java.io.File;
 import java.io.FileNotFoundException;
 import java.io.FileReader;
 import java.io.FilenameFilter;
 import java.io.IOException;
 import java.io.ObjectInputStream;
 import java.io.ObjectOutputStream;
 import java.io.Serializable;
 import java.net.MalformedURLException;
 import java.net.URI;
 import java.net.URISyntaxException;
 import java.text.MessageFormat;
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.Map;
 import java.text.DateFormat;
 
 import android.app.AlertDialog;
 import android.app.Dialog;
 import android.app.NotificationManager;
 import android.app.ProgressDialog;
 import android.content.ComponentName;
 import android.content.Context;
 import android.content.DialogInterface;
 import android.content.Intent;
 import android.content.ServiceConnection;
 import android.content.pm.PackageInfo;
 import android.content.pm.PackageManager;
 import android.content.res.Configuration;
 import android.content.res.Resources;
 import android.graphics.Color;
 import android.graphics.Typeface;
 import android.net.Uri;
 import android.os.Bundle;
 import android.os.Environment;
 import android.os.Handler;
 import android.os.IBinder;
 import android.os.Message;
 import android.util.Log;
 import android.view.Gravity;
 import android.view.Menu;
 import android.view.MenuItem;
 import android.view.View;
 import android.view.ViewGroup;
 import android.view.ViewGroup.LayoutParams;
 import android.view.animation.AnimationUtils;
 import android.widget.AdapterView;
 import android.widget.ArrayAdapter;
 import android.widget.Button;
 import android.widget.ImageView;
 import android.widget.LinearLayout;
 import android.widget.ProgressBar;
 import android.widget.ScrollView;
 import android.widget.Spinner;
 import android.widget.TextView;
 import android.widget.Toast;
 import android.widget.ViewFlipper;
 import cmupdaterapp.service.FullUpdateInfo;
 import cmupdaterapp.service.PlainTextUpdateServer;
 import cmupdaterapp.service.UpdateDownloaderService;
 import cmupdaterapp.service.UpdateInfo;
 import cmupdaterapp.utils.IOUtils;
 import cmupdaterapp.utils.Preferences;
 import cmupdaterapp.utils.SysUtils;
 
 import com.google.zxing.integration.android.IntentIntegrator;
 import com.google.zxing.integration.android.IntentResult;
 
 public class UpdateProcessInfo extends IUpdateProcessInfo
 {
 	private static final String TAG = "<CM-Updater> UpdateProcessInfo";
 	private static final String STORED_STATE_FILENAME = "CMUpdater.ser";
 
 	private static final int MENU_ID_UPDATE_NOW = 1;
 	private static final int MENU_ID_SCAN_QR = 2;
 	private static final int MENU_ID_CONFIG= 3;
 	private static final int MENU_ID_ABOUT= 4;
 	private static final int MENU_ID_CHANGELOG= 5;
 
 	private static final String KEY_AVAILABLE_UPDATES = "cmupdaterapp.availableUpdates";
 	private static final String KEY_MIRROR_NAME = "cmupdaterapp.mirrorName";
 
 	public static final int REQUEST_NEW_UPDATE_LIST = 1;
 	public static final int REQUEST_UPDATE_CHECK_ERROR = 2;
 	public static final int REQUEST_DOWNLOAD_FAILED = 3;
 	public static final int REQUEST_MD5CHECKER_CANCEL = 4;
 
 	public static final String KEY_REQUEST = "cmupdaterapp.keyRequest";
 	public static final String KEY_UPDATE_LIST = "cmupdaterapp.fullUpdateList";
 	
 	public static final int CHANGELOGTYPE_ROM = 1;
 	public static final int CHANGELOGTYPE_APP = 2;
 	public static final int CHANGELOGTYPE_THEME = 3;
 
 	public static final int FLIPPER_AVAILABLE_UPDATES = 0;
 	public static final int FLIPPER_EXISTING_UPDATES = 1;
 	public static final int FLIPPER_AVAILABLE_THEMES = 2;
 
 	private Spinner mUpdatesSpinner;
 	private Spinner mThemesSpinner;
 	private PlainTextUpdateServer mUpdateServer;
 	private ProgressBar mProgressBar;
 	private TextView mDownloadedBytesTextView;
 	private TextView mDownloadMirrorTextView;
 	private TextView mDownloadFilenameTextView;
 	private TextView mDownloadSpeedTextView;
 	private TextView mRemainingTimeTextView;
 	private FullUpdateInfo mAvailableUpdates;
 	private String mMirrorName;
 	private String mFileName;
 	private UpdateDownloaderService mUpdateDownloaderService;
 	private Intent mUpdateDownloaderServiceIntent;
 
 	private File mUpdateFolder;
 	private Spinner mExistingUpdatesSpinner;
 
 	public static ProgressDialog ChangelogProgressDialog;
 	public static Handler ChangelogProgressHandler;
 	public Thread ChangelogThread;
 	public List<Version> ChangelogList = null;
 
 	private List<String> mfilenames;
 
 	private TextView mdownloadedUpdateText;
 	private Spinner mspFoundUpdates;
 	private Button mdeleteOldUpdatesButton;
 	private Button mapplyUpdateButton;
 	private TextView mNoExistingUpdatesFound;
 
 	private ViewFlipper flipper;
 
 
 	private final ServiceConnection mUpdateDownloaderServiceConnection = new ServiceConnection()
 	{
 		public void onServiceConnected(ComponentName className, IBinder service)
 		{
 			mUpdateDownloaderService = ((UpdateDownloaderService.LocalBinder)service).getService();
 			if(mUpdateDownloaderService.isDownloading())
 			{
 				switchToDownloadingLayout(mUpdateDownloaderService.getCurrentUpdate());
 			}
 		}
 
 		public void onServiceDisconnected(ComponentName className)
 		{
 			mUpdateDownloaderService = null;
 		}
 	};
 
 	//static so the reference is kept while the thread is running
 	//private static DownloadUpdateTask mDownloadUpdateTask;
 
 
 	private final View.OnClickListener mDownloadUpdateButtonListener = new View.OnClickListener()
 	{
 		public void onClick(View v)
 		{
 			if(!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()))
 			{
 				new AlertDialog.Builder(UpdateProcessInfo.this)
 				.setTitle(R.string.sdcard_is_not_present_dialog_title)
 				.setMessage(R.string.sdcard_is_not_present_dialog_body)
 				.setPositiveButton(R.string.sdcard_is_not_present_dialog_ok_button, new DialogInterface.OnClickListener()
 				{
 					public void onClick(DialogInterface dialog, int which)
 					{
 						dialog.dismiss();
 					}
 				})
 				.show();
 				return;
 			}
 
 			
 			UpdateInfo ui = (UpdateInfo) mUpdatesSpinner.getSelectedItem();
 			//Check if the File is present, so prompt the User to overwrite it
 			File foo = new File(mUpdateFolder + "/" + ui.fileName);
 			if (foo.isFile() && foo.exists())
 			{
 				new AlertDialog.Builder(UpdateProcessInfo.this)
 				.setTitle(R.string.overwrite_update_title)
 				.setMessage(R.string.overwrite_update_summary)
 				.setNegativeButton(R.string.overwrite_update_negative, new DialogInterface.OnClickListener()
 				{
 					public void onClick(DialogInterface dialog, int which)
 					{
 						dialog.dismiss();
 					}
 				})
 				.setPositiveButton(R.string.overwrite_update_positive, new DialogInterface.OnClickListener()
 				{
 					public void onClick(DialogInterface dialog, int which)
 					{
 						downloadRequestedUpdate((UpdateInfo) mUpdatesSpinner.getSelectedItem());
 					}
 				})
 				.show();
 				return;
 			}
 			//Otherwise download it
 			else
 			{
 				downloadRequestedUpdate((UpdateInfo) mUpdatesSpinner.getSelectedItem());
 			}
 		}
 	};
 	
 	private final View.OnClickListener mDownloadThemeButtonListener = new View.OnClickListener()
 	{
 		public void onClick(View v)
 		{
 			if(!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()))
 			{
 				new AlertDialog.Builder(UpdateProcessInfo.this)
 				.setTitle(R.string.sdcard_is_not_present_dialog_title)
 				.setMessage(R.string.sdcard_is_not_present_dialog_body)
 				.setPositiveButton(R.string.sdcard_is_not_present_dialog_ok_button, new DialogInterface.OnClickListener()
 				{
 					public void onClick(DialogInterface dialog, int which)
 					{
 						dialog.dismiss();
 					}
 				})
 				.show();
 				return;
 			}
 
 			
 			UpdateInfo ui = (UpdateInfo) mThemesSpinner.getSelectedItem();
 			//Check if the File is present, so prompt the User to overwrite it
 			File foo = new File(mUpdateFolder + "/" + ui.fileName);
 			if (foo.isFile() && foo.exists())
 			{
 				new AlertDialog.Builder(UpdateProcessInfo.this)
 				.setTitle(R.string.overwrite_update_title)
 				.setMessage(R.string.overwrite_update_summary)
 				.setNegativeButton(R.string.overwrite_update_negative, new DialogInterface.OnClickListener()
 				{
 					public void onClick(DialogInterface dialog, int which)
 					{
 						dialog.dismiss();
 					}
 				})
 				.setPositiveButton(R.string.overwrite_update_positive, new DialogInterface.OnClickListener()
 				{
 					public void onClick(DialogInterface dialog, int which)
 					{
 						downloadRequestedUpdate((UpdateInfo) mThemesSpinner.getSelectedItem());
 					}
 				})
 				.show();
 				return;
 			}
 			//Otherwise download it
 			else
 			{
 				downloadRequestedUpdate((UpdateInfo) mThemesSpinner.getSelectedItem());
 			}
 		}
 	};
 	
 	private final Spinner.OnItemSelectedListener mUpdateSpinnerChanged = new Spinner.OnItemSelectedListener()
 	{
 		public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3)
 		{
 			Button updateChangelogButton = (Button) findViewById(R.id.show_changelog_button);
 			String changelog = ((UpdateInfo) mUpdatesSpinner.getSelectedItem()).description;
 			if (changelog == null || changelog == "")
 			{
 				updateChangelogButton.setVisibility(View.GONE);
 			}
 			else
 			{
 				updateChangelogButton.setVisibility(View.VISIBLE);
 			}
 		}
 
 		public void onNothingSelected(AdapterView<?> arg0)
 		{
 
 		}
 	};
 	
 	private final Spinner.OnItemSelectedListener mThemeSpinnerChanged = new Spinner.OnItemSelectedListener()
 	{
 		public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3)
 		{
 			Button themeChangelogButton = (Button) findViewById(R.id.show_theme_changelog_button);
 			String changelog = ((UpdateInfo) mThemesSpinner.getSelectedItem()).description;
 			if (changelog == null || changelog == "")
 			{
 				themeChangelogButton.setVisibility(View.GONE);
 			}
 			else
 			{
 				themeChangelogButton.setVisibility(View.VISIBLE);
 			}
 		}
 
 		public void onNothingSelected(AdapterView<?> arg0)
 		{
 
 		}
 	};
 	
 	private final View.OnClickListener mUpdateChangelogButtonListener = new View.OnClickListener()
 	{
 		public void onClick(View v)
 		{
 			getChangelog(CHANGELOGTYPE_ROM);
 		}
 	};
 	
 	private final View.OnClickListener mThemeChangelogButtonListener = new View.OnClickListener()
 	{
 		public void onClick(View v)
 		{
 			getChangelog(CHANGELOGTYPE_THEME);
 		}
 	};
 
 	private final View.OnClickListener mDeleteUpdatesButtonListener = new View.OnClickListener()
 	{
 		public void onClick(View v)
 		{
 			if(!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()))
 			{
 				new AlertDialog.Builder(UpdateProcessInfo.this)
 				.setTitle(R.string.sdcard_is_not_present_dialog_title)
 				.setMessage(R.string.sdcard_is_not_present_dialog_body)
 				.setPositiveButton(R.string.sdcard_is_not_present_dialog_ok_button, new DialogInterface.OnClickListener()
 				{
 					public void onClick(DialogInterface dialog, int which)
 					{
 						dialog.dismiss();
 					}
 				})
 				.show();
 				return;
 			}
 			else
 			{
 				new AlertDialog.Builder(UpdateProcessInfo.this)
 				.setTitle(R.string.delete_updates_text)
 				.setMessage(R.string.confirm_delete_update_folder_dialog_message)
 				//Delete Only Selected Update
 				.setNeutralButton(R.string.confirm_delete_update_folder_dialog_neutral, new DialogInterface.OnClickListener()
 				{
 					public void onClick(DialogInterface dialog, int which)
 					{
 						//Delete Updates here
 						String f = (String) mExistingUpdatesSpinner.getSelectedItem();
 						if(deleteUpdate(f))
 						{
 							mfilenames.remove(f);
 							//mfilenames.trimToSize();
 						}
 						//If Updates are cached or Present, reload the View
 						if(mAvailableUpdates != null)
 						{
 							switchToUpdateChooserLayout(mAvailableUpdates);
 						}
 						//Otherwise switch to Updatechooserlayout. If no Updates are found and no files in Updatefolder, the Functions redirects you to NO ROMS FOUND
 						else
 						{
 							switchToUpdateChooserLayout(null);
 						}
 					}
 				})
 				//Delete All Updates
 				.setPositiveButton(R.string.confirm_delete_update_folder_dialog_yes, new DialogInterface.OnClickListener()
 				{
 					public void onClick(DialogInterface dialog, int which)
 					{
 						//Delete Updates here
 						deleteOldUpdates();
 						//Set the Filenames to null, so the Spinner will be empty
 						mfilenames = null;
 						//If Updates are cached or Present, reload the View
 						if(mAvailableUpdates != null)
 						{
 							switchToUpdateChooserLayout(mAvailableUpdates);
 						}
 						//Otherwise switch to Updatechooserlayout. If no Updates are found and no files in Updatefolder, the Functions redirects you to NO ROMS FOUND
 						else
 						{
 							switchToUpdateChooserLayout(null);
 						}
 					}
 				})
 				//Delete no Update
 				.setNegativeButton(R.string.confirm_delete_update_folder_dialog_no, new DialogInterface.OnClickListener()
 				{
 					public void onClick(DialogInterface dialog, int which)
 					{
 						dialog.dismiss();
 					}
 				})
 				.show();
 			}
 		}
 	};
 
 	//To Apply Existing Update
 	private final class mApplyExistingButtonListener implements View.OnClickListener
 	{
 		private ProgressDialog mDialog;
 		private UserTask<File, Void, Boolean> mBgTask;
 		private String filename;
 		private File Update;
 		
 		public void onClick(View v)
 		{	
 			if(!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()))
 			{
 				new AlertDialog.Builder(UpdateProcessInfo.this)
 				.setTitle(R.string.sdcard_is_not_present_dialog_title)
 				.setMessage(R.string.sdcard_is_not_present_dialog_body)
 				.setPositiveButton(R.string.sdcard_is_not_present_dialog_ok_button, new DialogInterface.OnClickListener()
 				{
 					public void onClick(DialogInterface dialog, int which)
 					{
 						dialog.dismiss();
 					}
 				})
 				.show();
 				return;
 			}
 
 			filename = (String) mExistingUpdatesSpinner.getSelectedItem();
 			Update = new File(mUpdateFolder + "/" +filename);
 			File MD5 = new File(mUpdateFolder + "/" +filename + ".md5sum");
 			//IF no MD5 exists, ask the User what to do
 			if(!MD5.exists() || !MD5.canRead())
 			{
 				new AlertDialog.Builder(UpdateProcessInfo.this)
 				.setTitle(R.string.no_md5_found_title)
 				.setMessage(R.string.no_md5_found_summary)
 				.setPositiveButton(R.string.no_md5_found_positive, new DialogInterface.OnClickListener()
 				{
 					public void onClick(DialogInterface dialog, int which)
 					{
 						Resources r = getResources();
 						mDialog = ProgressDialog.show(
 								UpdateProcessInfo.this,
 								r.getString(R.string.verify_and_apply_dialog_title),
 								r.getString(R.string.verify_and_apply_dialog_message),
 								true,
 								true,
 								new DialogInterface.OnCancelListener()
 								{
 									public void onCancel(DialogInterface arg0)
 									{
 										mBgTask.cancel(true);
 									}
 								}
 						);
 			
 						mBgTask = new MD5CheckerTask(mDialog, filename).execute(Update);
 						dialog.dismiss();
 					}
 				})
 				.setNegativeButton(R.string.no_md5_found_negative, new DialogInterface.OnClickListener()
 				{
 					public void onClick(DialogInterface dialog, int which)
 					{
 						dialog.dismiss();
 					}
 				})
 				.show();
 			}
 			//If MD5 exists, apply the update normally
 			else
 			{
 				Resources r = getResources();
 				mDialog = ProgressDialog.show(
 						UpdateProcessInfo.this,
 						r.getString(R.string.verify_and_apply_dialog_title),
 						r.getString(R.string.verify_and_apply_dialog_message),
 						true,
 						true,
 						new DialogInterface.OnCancelListener()
 						{
 							public void onCancel(DialogInterface arg0)
 							{
 								mBgTask.cancel(true);
 							}
 						}
 				);
 	
 				mBgTask = new MD5CheckerTask(mDialog, filename).execute(Update);
 			}
 		}
 	}
 
 	private final class MD5CheckerTask extends UserTask<File, Void, Boolean>
 	{	
 		private ProgressDialog mDialog;
 		private String mFilename;
 		private boolean mreturnvalue;
 
 		public MD5CheckerTask(ProgressDialog dialog, String filename)
 		{
 			mDialog = dialog;
 			mFilename = filename;
 		}
 
 		@Override
 		public Boolean doInBackground(File... params)
 		{
 			
 			boolean MD5exists = false;
 			try
 			{
 				File MD5 = new File(params[0]+".md5sum");
 				if (MD5.exists() && MD5.canRead())
 					MD5exists = true;
 				if (params[0].exists() && params[0].canRead())
 				{
 					//If MD5 File exists, check it
 					if(MD5exists)
 					{
 						//Calculate MD5 of Existing Update
 						String calculatedMD5 = IOUtils.calculateMD5(params[0], false);
 						//Read the existing MD5SUM
 						FileReader input = new FileReader(MD5);
 						BufferedReader bufRead = new BufferedReader(input);
 						String firstLine = bufRead.readLine();
 						bufRead.close();
 						input.close();
 						//If the content of the File is not empty, compare it
 						if (firstLine != null)
 						{
 							String[] SplittedString = firstLine.split("  ");
 							if(SplittedString[0].equalsIgnoreCase(calculatedMD5))
 								mreturnvalue = true;
 						}
 						else
 							mreturnvalue = false;
 					}
 					else
 					{
 						return true;
 					}
 				}
 			}
 			catch (IOException e)
 			{
 				Log.e(TAG, "IOEx while checking MD5 sum", e);
 				mreturnvalue = false;
 			}
 			return mreturnvalue;
 		}
 
 		@Override
 		public void onPostExecute(Boolean result)
 		{
 			UpdateInfo ui = new UpdateInfo();
 			String[] temp = mFilename.split("\\\\");
 			ui.name = temp[temp.length-1];
 			ui.fileName = mFilename;
 			if(result == true)
 			{
 				Intent i = new Intent(UpdateProcessInfo.this, ApplyUploadActivity.class)
 				.putExtra(ApplyUploadActivity.KEY_UPDATE_INFO, ui);
 				startActivity(i);
 			}
 			else
 			{
 				Toast.makeText(UpdateProcessInfo.this, R.string.apply_existing_update_md5error_message, Toast.LENGTH_LONG).show();
 			}
 
 			mDialog.dismiss();
 		}
 
 		@Override
 		public void onCancelled()
 		{
 			Log.w(TAG, "MD5Checker Task cancelled");
 			Intent i = new Intent(UpdateProcessInfo.this, UpdateProcessInfo.class);
 			i.putExtra(UpdateProcessInfo.KEY_REQUEST, UpdateProcessInfo.REQUEST_MD5CHECKER_CANCEL);
 			i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
 			startActivity(i);
 		}
 	};
 
 	private final View.OnClickListener mCancelDownloadListener = new View.OnClickListener()
 	{
 		public void onClick(View arg0)
 		{
 			new AlertDialog.Builder(UpdateProcessInfo.this)
 			.setMessage(R.string.confirm_download_cancelation_dialog_message)
 			.setPositiveButton(R.string.confirm_download_cancelation_dialog_yes, new DialogInterface.OnClickListener()
 			{
 				public void onClick(DialogInterface dialog, int which)
 				{
 					Log.d(TAG, "Positive Download Cancel Button pressed");
 					if (mUpdateDownloaderService!=null)
 					{
 						mUpdateDownloaderService.cancelDownload();
 						Log.d(TAG, "Cancel onClick Event: cancelDownload finished");
 					}
 					else
 						Log.d(TAG, "Cancel Download: mUpdateDownloaderService was NULL");
 					try
 					{
 						stopService(mUpdateDownloaderServiceIntent);
 						Log.d(TAG, "stopService(mUpdateDownloaderServiceIntent) finished");
 					}
 					catch (Exception ex)
 					{
 						Log.e(TAG, "Cancel Download: mUpdateDownloaderServiceIntent could not be Stopped", ex);
 					}
 					try
 					{
 						unbindService(mUpdateDownloaderServiceConnection);
 						Log.d(TAG, "unbindService(mUpdateDownloaderServiceConnection) finished");
 					}
 					catch (Exception ex)
 					{
 						Log.e(TAG, "Cancel Download: mUpdateDownloaderServiceConnection unbind failed", ex);
 					}
 					//UpdateDownloaderService.setUpdateProcessInfo(null);
 					Log.d(TAG, "Download Cancel Procedure Finished. Switching Layout");
 					switchToUpdateChooserLayout(null);
 				}
 			})
 			.setNegativeButton(R.string.confirm_download_cancelation_dialog_no, new DialogInterface.OnClickListener()
 			{
 				public void onClick(DialogInterface dialog, int which)
 				{
 					Log.d(TAG, "Negative Download Cancel Button pressed");
 					dialog.dismiss();
 				}
 			})
 			.show();
 		}
 
 	};
 
 	@Override
 	public void onCreate(Bundle savedInstanceState)
 	{
 		super.onCreate(savedInstanceState);
 
 		Preferences prefs = Preferences.getPreferences(this);
 		if(prefs.isFirstRun())
 		{
 			prefs.configureModString();
 			prefs.setFirstRun(false);
 		}
 		//If an older Version was installed, the ModVersion is still ADP1. So reset it
 		if(prefs.getConfiguredModString().equals("ADP1"))
 			prefs.configureModString();
 
 		try
 		{
 			loadState();
 		}
 		catch (IOException e)
 		{
 			Log.e(TAG, "Unable to load application state", e);
 		}
 
 		restoreSavedInstanceValues(savedInstanceState);
 
 		mUpdateServer = new PlainTextUpdateServer(this);
 
 		mUpdateFolder = new File(Environment.getExternalStorageDirectory() + "/" + Preferences.getPreferences(this).getUpdateFolder());
 
 		mUpdateDownloaderServiceIntent = new Intent(this, UpdateDownloaderService.class);
 	}
 
 	/* (non-Javadoc)
 	 * @see android.app.Activity#onStart()
 	 */
 	@Override
 	protected void onStart()
 	{
 		super.onStart();
 		
 		//Delete any older Versions, because of the changed Signing Key
 		while (deleteOldVersionsOfUpdater()==false)
 		{
 			//User MUST uninstall old App
 			Log.i(TAG, "Old App not uninstalled, try again");
 		}
 		
 		try
 		{
 			loadState();
 		}
 		catch (FileNotFoundException e)
 		{
 			//Ignored, data was not saved
 		}
 		catch (IOException e)
 		{
 			Log.w(TAG, "Unable to restore activity status", e);
 		}
 		
 		Intent UpdateIntent = getIntent();
 		if (UpdateIntent != null)
 		{
 			int req = UpdateIntent.getIntExtra(KEY_REQUEST, -1);
 			switch(req)
 			{
 				case REQUEST_NEW_UPDATE_LIST:
 					mAvailableUpdates = (FullUpdateInfo) getIntent().getSerializableExtra(KEY_UPDATE_LIST);
 					try
 					{
 						saveState();
 					}
 					catch (IOException e)
 					{
 						Log.e(TAG, "Unable to save application state", e);
 					}
 					break;
 				case REQUEST_UPDATE_CHECK_ERROR:
 					Log.w(TAG, "Update check error");
 					Toast.makeText(this, R.string.not_update_check_error_ticker, Toast.LENGTH_SHORT).show();
 					break;
 		
 				case REQUEST_DOWNLOAD_FAILED:
 					Log.w(TAG, "Download Error");
 					Toast.makeText(this, R.string.exception_while_downloading, Toast.LENGTH_SHORT).show();
 					break;
 				case REQUEST_MD5CHECKER_CANCEL:
 					Log.w(TAG, "MD5Check canceled. Switching Layout");
 					Toast.makeText(this, R.string.md5_check_cancelled, Toast.LENGTH_SHORT).show();
 					break;
 				default:
 					Log.w(TAG, "Uknown KEY_REQUEST in Intent. Maybe its the first start.");
 					break;
 			}
 		}
 		else
 		{
 			Log.w(TAG, "Intent is NULL");
 		}
 		
 		//Outside the if to prevent a empty spinnercontrol
 		FilenameFilter f = new UpdateFilter(".zip");
 		File[] files = mUpdateFolder.listFiles(f);
 		//If Folder Exists and Updates are present(with md5files)
 		if(mUpdateFolder.exists() && mUpdateFolder.isDirectory() && files != null && files.length>0)
 		{
 			//To show only the Filename. Otherwise the whole Path with /sdcard/cm-updates will be shown
 			mfilenames = new ArrayList<String>();
 			for (int i=0;i<files.length;i++)
 			{
 				mfilenames.add(files[i].getName());
 			}
 			//For sorting the Filenames, have to find a way to do natural sorting
 			mfilenames = Collections.synchronizedList(mfilenames); 
             Collections.sort(mfilenames, Collections.reverseOrder()); 
 		}
 		files = null;
 		if(mUpdateDownloaderService != null && mUpdateDownloaderService.isDownloading())
 		{
 			switchToDownloadingLayout(mUpdateDownloaderService.getCurrentUpdate());
 		}
 		else if (mAvailableUpdates != null || (mfilenames != null && mfilenames.size() > 0))
 		{
 			switchToUpdateChooserLayout(mAvailableUpdates);
 		}
 		else
 		{
 			switchToUpdateChooserLayout(null);
 		}
 		UpdateDownloaderService.setUpdateProcessInfo(UpdateProcessInfo.this);
 	}
 
 	@Override
 	public void onConfigurationChanged(Configuration newConfig)
 	{
 		ScrollView s = (ScrollView) findViewById(R.id.mainScroll);
 		LinearLayout l = (LinearLayout) findViewById(R.id.mainLinear);
 		Resources res = getResources();
 		if (s!=null)
 		{
 			if(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE)
 				s.setBackgroundDrawable(res.getDrawable(R.drawable.background_landscape));
 			else if(newConfig.orientation == Configuration.ORIENTATION_PORTRAIT)
 				s.setBackgroundDrawable(res.getDrawable(R.drawable.background));
 		}
 		else if (l!=null)
 		{
 			if(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE)
 				l.setBackgroundDrawable(res.getDrawable(R.drawable.background_landscape));
 			else if(newConfig.orientation == Configuration.ORIENTATION_PORTRAIT)
 				l.setBackgroundDrawable(res.getDrawable(R.drawable.background));
 		}
         super.onConfigurationChanged(newConfig); 
         Log.i(TAG, "Orientation Changed. New Orientation: "+newConfig.orientation);
     }
 	
 	/* (non-Javadoc)
 	 * @see android.app.Activity#onStop()
 	 */
 	@Override
 	protected void onStop()
 	{
 		super.onStop();
 		try
 		{
 			saveState();
 		}
 		catch (IOException e)
 		{
 			Log.w(TAG, "Unable to save state", e);
 		}
 		Log.d(TAG, "App closed");
 
 		if(mUpdateDownloaderService != null && !mUpdateDownloaderService.isDownloading())
 		{
 			try
 			{
 				unbindService(mUpdateDownloaderServiceConnection);
 			}
 			catch (Exception ex)
 			{
 				Log.e(TAG, "Exit App: mUpdateDownloaderServiceConnection unbind failed", ex);
 			}
 			try
 			{
 				stopService(mUpdateDownloaderServiceIntent);
 			}
 			catch (Exception ex)
 			{
 				Log.e(TAG, "Exit App: mUpdateDownloaderServiceIntent could not be Stopped", ex);
 			}
 		}
 		else
 			Log.d(TAG, "DownloadService not Stopped. Not Started or Currently Downloading");
 	}
 
 	private void saveState() throws IOException
 	{
 		ObjectOutputStream oos = new ObjectOutputStream(openFileOutput(STORED_STATE_FILENAME, Context.MODE_PRIVATE));
 		try
 		{
 			Map<String,Serializable> data = new HashMap<String, Serializable>();
 			data.put("mAvailableUpdates", (Serializable)mAvailableUpdates);
 			data.put("mMirrorName", mMirrorName);
 			oos.writeObject(data);
 			oos.flush();
 		}
 		finally
 		{
 			oos.close();
 		}
 	}
 
 	@SuppressWarnings("unchecked")
 	private void loadState() throws IOException
 	{
 		ObjectInputStream ois = new ObjectInputStream(openFileInput(STORED_STATE_FILENAME));
 		try
 		{
 			Map<String,Serializable> data = (Map<String, Serializable>) ois.readObject();
 
 			Object o = data.get("mAvailableUpdates"); 
 			if(o != null) mAvailableUpdates = (FullUpdateInfo) o;
 
 			o = data.get("mMirrorName"); 
 			if(o != null) mMirrorName =  (String) o;
 		}
 		catch (ClassNotFoundException e)
 		{
 			Log.e(TAG, "Unable to load stored class", e);
 		}
 		finally
 		{
 			ois.close();
 		}
 	}
 
 	private void restoreSavedInstanceValues(Bundle b)
 	{
 		if(b == null) return;
 		mAvailableUpdates = (FullUpdateInfo) b.getSerializable(KEY_AVAILABLE_UPDATES);
 		mMirrorName = b.getString(KEY_MIRROR_NAME);
 	}
 
 	@Override
 	protected void onSaveInstanceState(Bundle outState)
 	{
 		outState.putSerializable(KEY_AVAILABLE_UPDATES, (Serializable)mAvailableUpdates);
 		outState.putString(KEY_MIRROR_NAME, mMirrorName);
 	}
 
 	/* (non-Javadoc)
 	 * @see android.app.Activity#onNewIntent(android.content.Intent)
 	 */
 	@Override
 	protected void onNewIntent(Intent intent)
 	{
 		super.onNewIntent(intent);
 
 		int req = intent.getIntExtra(KEY_REQUEST, -1);
 		switch(req)
 		{
 			case REQUEST_NEW_UPDATE_LIST:
 				switchToUpdateChooserLayout((FullUpdateInfo) intent.getSerializableExtra(KEY_UPDATE_LIST));
 				break;
 		}
 	}
 
 	@Override
 	public boolean onCreateOptionsMenu(Menu menu)
 	{
 		super.onCreateOptionsMenu(menu);
 		menu.add(Menu.NONE, MENU_ID_UPDATE_NOW, Menu.NONE, R.string.menu_check_now)
 		.setIcon(R.drawable.check_now);
 		menu.add(Menu.NONE, MENU_ID_SCAN_QR, Menu.NONE, R.string.menu_qr_code)
 		.setIcon(R.drawable.button_scanqr);
 		menu.add(Menu.NONE, MENU_ID_CONFIG, Menu.NONE, R.string.menu_config)
 		.setIcon(R.drawable.button_config);
 		menu.add(Menu.NONE, MENU_ID_ABOUT, Menu.NONE, R.string.menu_about)
 		.setIcon(R.drawable.button_about);
 		menu.add(Menu.NONE, MENU_ID_CHANGELOG, Menu.NONE, R.string.menu_changelog)
 		.setIcon(R.drawable.button_clog);
 		return true;
 	}
 
 	/* (non-Javadoc)
 	 * @see android.app.Activity#onPrepareOptionsMenu(android.view.Menu)
 	 */
 	@Override
 	public boolean onPrepareOptionsMenu(Menu menu) 
 	{
 		boolean superReturn = super.onPrepareOptionsMenu(menu);
 
 		if(mUpdateDownloaderService != null && mUpdateDownloaderService.isDownloading())
 		{
 			//Download in progress
 			menu.findItem(MENU_ID_UPDATE_NOW).setEnabled(false);
 		}
 		else if (mAvailableUpdates != null)
 		{
 			//Available updates
 		}
 		else
 		{
 			//No available updates
 		}
 		return superReturn;
 	}
 
 	/* (non-Javadoc)
 	 * @see android.app.Activity#onMenuItemSelected(int, android.view.MenuItem)
 	 */
 	@Override
 	public boolean onMenuItemSelected(int featureId, MenuItem item)
 	{
 		switch(item.getItemId())
 		{
 			case MENU_ID_UPDATE_NOW:
 				checkForUpdates();
 				return true;
 			case MENU_ID_SCAN_QR:
 				scanQRURL();
 				return true;
 			case MENU_ID_CONFIG:
 				showConfigActivity();
 				return true;
 			case MENU_ID_ABOUT:
 				showAboutDialog();
 				return true;
 			case MENU_ID_CHANGELOG:
 				getChangelog(CHANGELOGTYPE_APP);
 				//Open the Browser for Changelog
 				//Preferences prefs = Preferences.getPreferences(this);
 				//Intent i = new Intent(Intent.ACTION_VIEW);
 				//i.setData(Uri.parse(prefs.getAboutURL()));
 				//startActivity(i);
 				return true;
 			default:
 				Log.w(TAG, "Unknown Menu ID:" + item.getItemId());
 				break;
 		}
 
 		return super.onMenuItemSelected(featureId, item);
 	}
 
 	@Override
 	public void switchToDownloadingLayout(UpdateInfo downloadingUpdate)
 	{
 		bindService(mUpdateDownloaderServiceIntent, mUpdateDownloaderServiceConnection, Context.BIND_AUTO_CREATE);
 		setContentView(R.layout.update_download_info);
 		try
 		{
 			String[] temp = downloadingUpdate.updateFileUris.get(0).toURL().getFile().split("/");
 			mFileName = temp[temp.length-1];
 		}
 		catch (MalformedURLException e)
 		{
 			mFileName = "Unable to get Filename";
 			Log.e(TAG, "Unable to get Filename", e);
 		}
 		
 		Resources res = getResources();
 		
 		mProgressBar = (ProgressBar) findViewById(R.id.download_progress_bar);
 		mDownloadedBytesTextView = (TextView) findViewById(R.id.bytes_downloaded_text_view);
 
 		mDownloadMirrorTextView = (TextView) findViewById(R.id.mirror_text_view);
 
 		mDownloadFilenameTextView = (TextView) findViewById(R.id.filename_text_view);
 
 		mDownloadSpeedTextView = (TextView) findViewById(R.id.speed_text_view);
 		mRemainingTimeTextView = (TextView) findViewById(R.id.remaining_time_text_view);
 
 		if(mMirrorName != null)
 			mDownloadMirrorTextView.setText(mMirrorName);
 		if(mFileName != null)
 			mDownloadFilenameTextView.setText(mFileName);
 		((Button)findViewById(R.id.cancel_download_buton)).setOnClickListener(mCancelDownloadListener);
 		
 		//Set the correct wallpaper
 		LinearLayout l = (LinearLayout) findViewById(R.id.mainLinear);
 		int Orientation = res.getConfiguration().orientation;
 		if(Orientation == Configuration.ORIENTATION_LANDSCAPE)
 			l.setBackgroundDrawable(res.getDrawable(R.drawable.background_landscape));
 		else if(Orientation == Configuration.ORIENTATION_PORTRAIT)
 			l.setBackgroundDrawable(res.getDrawable(R.drawable.background));
 	}
 
 	@Override
 	public void switchToUpdateChooserLayout(FullUpdateInfo availableUpdates)
 	{
 		if(availableUpdates != null)
 		{
 			mAvailableUpdates = availableUpdates;
 		}
 
 		setContentView(R.layout.main);
 		flipper = (ViewFlipper)findViewById(R.id.Flipper);
 		flipper.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.push_left_in));
 		flipper.setOutAnimation(AnimationUtils.loadAnimation(this, R.anim.push_left_out));
 		
 		//Flipper Buttons
 		Button btnAvailableUpdates=(Button)findViewById(R.id.button_available_updates);
 		btnAvailableUpdates.setOnClickListener(new View.OnClickListener()
 		{
 			public void onClick(View view)
 			{
 				if(flipper.getDisplayedChild() != FLIPPER_AVAILABLE_UPDATES)
 					flipper.setDisplayedChild(FLIPPER_AVAILABLE_UPDATES);
 			}
 		});
 		Button btnExistingUpdates=(Button)findViewById(R.id.button_existing_updates);
 		btnExistingUpdates.setOnClickListener(new View.OnClickListener()
 		{
 			public void onClick(View view)
 			{
 				if(flipper.getDisplayedChild() != FLIPPER_EXISTING_UPDATES)
 					flipper.setDisplayedChild(FLIPPER_EXISTING_UPDATES);
 			}
 		});
 		Button btnAvailableThemes=(Button)findViewById(R.id.button_available_themes);
 		btnAvailableThemes.setOnClickListener(new View.OnClickListener()
 		{
 			public void onClick(View view)
 			{
 				if(flipper.getDisplayedChild() != FLIPPER_AVAILABLE_THEMES)
 					flipper.setDisplayedChild(FLIPPER_AVAILABLE_THEMES);
 			}
 		});
 		
 		((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).cancel(R.string.not_new_updates_found_title);
 		
 		Resources res = getResources();
 		TextView currentVersiontv = (TextView) findViewById(R.id.up_chooser_current_version);
 		TextView experimentalBuildstv = (TextView) findViewById(R.id.experimental_updates_textview);
 		TextView showDowngradestv = (TextView) findViewById(R.id.show_downgrades_textview);
 		TextView lastUpdateChecktv = (TextView) findViewById(R.id.last_update_check);
 		String pattern = res.getString(R.string.current_version_text);
 		Preferences prefs = Preferences.getPreferences(this);
 		currentVersiontv.setText(MessageFormat.format(pattern, SysUtils.getReadableModVersion()));
 		String allowExperimental;
 		String showDowngrades;
 		if(prefs.allowExperimental())
 			allowExperimental = res.getString(R.string.true_string);
 		else
 			allowExperimental = res.getString(R.string.false_string);
 		if(prefs.showDowngrades())
 			showDowngrades = res.getString(R.string.true_string);
 		else
 			showDowngrades = res.getString(R.string.false_string);
 		experimentalBuildstv.setText(MessageFormat.format(res.getString(R.string.p_display_allow_experimental_versions_title)+": {0}", allowExperimental));
 		showDowngradestv.setText(MessageFormat.format(res.getString(R.string.p_display_older_mod_versions_title)+": {0}", showDowngrades));
 		lastUpdateChecktv.setText(MessageFormat.format(res.getString(R.string.last_update_check_text)+": {0} {1}", DateFormat.getDateInstance().format(prefs.getLastUpdateCheck()), DateFormat.getTimeInstance().format(prefs.getLastUpdateCheck())));
 		
 		//Existing Updates Layout
 		mdownloadedUpdateText = (TextView) findViewById(R.id.downloaded_update_found);
 		mspFoundUpdates = mExistingUpdatesSpinner = (Spinner) findViewById(R.id.found_updates_list);
 		mdeleteOldUpdatesButton = (Button) findViewById(R.id.delete_updates_button);
 		mapplyUpdateButton = (Button) findViewById(R.id.apply_update_button);
 		mNoExistingUpdatesFound = (TextView) findViewById(R.id.no_existing_updates_found_textview);
 		
 		//Rom Layout
 		Button selectUploadButton = (Button) findViewById(R.id.download_update_button);
 		mUpdatesSpinner = (Spinner) findViewById(R.id.available_updates_list);
 		TextView DownloadText = (TextView) findViewById(R.id.available_updates_text);
 		LinearLayout stableExperimentalInfo = (LinearLayout) findViewById(R.id.stable_experimental_description_container);
 		Button changelogButton = (Button) findViewById(R.id.show_changelog_button);
 		
 		//Theme Layout
 		Button btnDownloadTheme = (Button) findViewById(R.id.download_theme_button);
 		mThemesSpinner = (Spinner) findViewById(R.id.available_themes_list);
 		TextView tvThemeDownloadText = (TextView) findViewById(R.id.available_themes_text);
 		Button btnThemechangelogButton = (Button) findViewById(R.id.show_theme_changelog_button);
 		
 		//No Updates Found Layout
 		Button CheckNowUpdateChooser = (Button) findViewById(R.id.check_now_button_update_chooser);
 		TextView CheckNowUpdateChooserText = (TextView) findViewById(R.id.check_now_update_chooser_text);
 		CheckNowUpdateChooserText.setVisibility(View.GONE);
 		CheckNowUpdateChooser.setVisibility(View.GONE);
 		
 		//Set the correct wallpaper
 		LinearLayout l = (LinearLayout) findViewById(R.id.mainLinear);
 		int Orientation = res.getConfiguration().orientation;
 		if(Orientation == Configuration.ORIENTATION_LANDSCAPE)
 			l.setBackgroundDrawable(res.getDrawable(R.drawable.background_landscape));
 		else if(Orientation == Configuration.ORIENTATION_PORTRAIT)
 			l.setBackgroundDrawable(res.getDrawable(R.drawable.background));
 		
 		//Sets the Theme and Rom Variables
 		List<UpdateInfo> availableRoms = null;
 		List<UpdateInfo> availableThemes = null;
 		if (mAvailableUpdates != null)
 		{
 			if (mAvailableUpdates.roms != null)
 				availableRoms = mAvailableUpdates.roms;
 			if (mAvailableUpdates.themes != null)
 				availableThemes = mAvailableUpdates.themes;
 		}
 		
 		//Rom Layout
 		if(availableRoms != null)
 		{
 			selectUploadButton.setOnClickListener(mDownloadUpdateButtonListener);
 			changelogButton.setOnClickListener(mUpdateChangelogButtonListener);
 			mUpdatesSpinner.setOnItemSelectedListener(mUpdateSpinnerChanged);
 			
 			UpdateListAdapter<UpdateInfo> spAdapterRoms = new UpdateListAdapter<UpdateInfo>(
 					this,
 					android.R.layout.simple_spinner_item,
 					availableRoms);
 			spAdapterRoms.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
 			mUpdatesSpinner.setAdapter(spAdapterRoms);
 
 		}
 		else
 		{
 			selectUploadButton.setVisibility(View.GONE);
 			mUpdatesSpinner.setVisibility(View.GONE);
 			DownloadText.setVisibility(View.GONE);
 			stableExperimentalInfo.setVisibility(View.GONE);
 			changelogButton.setVisibility(View.GONE);
 		}
 
 		//Theme Layout
 		if(availableThemes != null)
 		{
 			btnDownloadTheme.setOnClickListener(mDownloadThemeButtonListener);
 			btnThemechangelogButton.setOnClickListener(mThemeChangelogButtonListener);
 			mThemesSpinner.setOnItemSelectedListener(mThemeSpinnerChanged);
 			
 			UpdateListAdapter<UpdateInfo> spAdapterThemes = new UpdateListAdapter<UpdateInfo>(
 					this,
 					android.R.layout.simple_spinner_item,
 					availableThemes);
 			spAdapterThemes.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
 			mThemesSpinner.setAdapter(spAdapterThemes);
 
 		}
 		else
 		{
 			btnDownloadTheme.setVisibility(View.GONE);
 			mThemesSpinner.setVisibility(View.GONE);
 			tvThemeDownloadText.setVisibility(View.GONE);
 			btnThemechangelogButton.setVisibility(View.GONE);
 		}
 
 		//Existing Updates Layout
 		if (mfilenames != null && mfilenames.size() > 0)
 		{
 			if(availableUpdates == null)
 			{
 				//Display the Check Now Button and add the Event
 				//only Display when there are no Updates available
 				//or this Button and the Apply Button will be there
 				CheckNowUpdateChooserText.setVisibility(View.VISIBLE);
 				CheckNowUpdateChooser.setVisibility(View.VISIBLE);
 				CheckNowUpdateChooser.setOnClickListener(new View.OnClickListener()
 				{
 					public void onClick(View v)
 					{
 						checkForUpdates();
 					}
 				});
 			}
 			
 			ArrayAdapter<String> localUpdates = new ArrayAdapter<String>(
 					this,
 					android.R.layout.simple_spinner_item,
 					mfilenames);
 			localUpdates.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
 			mspFoundUpdates.setAdapter(localUpdates);
 		  	mapplyUpdateButton.setOnClickListener(new mApplyExistingButtonListener());
 			mdeleteOldUpdatesButton.setOnClickListener(mDeleteUpdatesButtonListener);
 		}
 		else
 		{
 			mNoExistingUpdatesFound.setVisibility(View.VISIBLE);
 			mspFoundUpdates.setVisibility(View.GONE);
 			mapplyUpdateButton.setVisibility(View.GONE);
 			mdownloadedUpdateText.setVisibility(View.GONE);
 			mdeleteOldUpdatesButton.setVisibility(View.GONE);
 		}
 		
		if (availableUpdates == null && (mfilenames == null || mfilenames.size() <= 0))
 		{
 			//Display the Check Now Button and add the Event
 			//The same as the SwitchToNoUpdatesAvailable
 			CheckNowUpdateChooserText.setVisibility(View.VISIBLE);
 			CheckNowUpdateChooser.setVisibility(View.VISIBLE);
 			CheckNowUpdateChooser.setOnClickListener(new View.OnClickListener()
 			{
 				public void onClick(View v)
 				{
 					checkForUpdates();
 				}
 			});
 		}
 	}
 
 	private void getChangelog(int changelogType)
 	{
 		Resources res = this.getResources(); 
 		
 		//Handler for the ThreadClass, that downloads the AppChangelog
 		ChangelogProgressHandler = new Handler()
 		{
 			@SuppressWarnings("unchecked")
 			public void handleMessage(Message msg)
 			{
 				if (UpdateProcessInfo.ChangelogProgressDialog != null)
 					UpdateProcessInfo.ChangelogProgressDialog.dismiss();
 				if (msg.obj instanceof String)
 				{
 					Toast.makeText(UpdateProcessInfo.this, (CharSequence) msg.obj, Toast.LENGTH_LONG).show();
 					ChangelogList = null;
 					UpdateProcessInfo.this.ChangelogThread.interrupt();
 					UpdateProcessInfo.ChangelogProgressDialog.dismiss();
 					displayChangelog(CHANGELOGTYPE_APP);
 				}
 				else if (msg.obj instanceof List<?>)
 				{
 					ChangelogList = (List<Version>) msg.obj;
 					UpdateProcessInfo.this.ChangelogThread.interrupt();
 					UpdateProcessInfo.ChangelogProgressDialog.dismiss();
 					displayChangelog(CHANGELOGTYPE_APP);
 				}
 	        }
 	    };
 		
 		switch (changelogType)
 		{
 			case CHANGELOGTYPE_ROM:
 				//Get the ROM Changelog and Display the Changelog
 				ChangelogList = Changelog.getRomChangelog((UpdateInfo) mUpdatesSpinner.getSelectedItem());
 				displayChangelog(CHANGELOGTYPE_ROM);
 				break;
 			case CHANGELOGTYPE_THEME:
 				//Get the ROM Changelog and Display the Changelog
 				ChangelogList = Changelog.getRomChangelog((UpdateInfo) mThemesSpinner.getSelectedItem());
 				displayChangelog(CHANGELOGTYPE_THEME);
 				break;
 			case CHANGELOGTYPE_APP:
 				//Show a ProgressDialog and start the Thread. The Dialog is shown in the Handler Function
 				ChangelogProgressDialog = ProgressDialog.show(this, res.getString(R.string.changelog_progress_title), res.getString(R.string.changelog_progress_body), true);
 				ChangelogThread = new Thread(new Changelog(this));
 				ChangelogThread.start();
 				break;
 			default:
 				return;
 		}
 	}
 	
 	private void displayChangelog(int changelogtype)
 	{
 		if (ChangelogList == null)
 			return;
 		Resources res = this.getResources(); 
 		boolean ChangelogEmpty = true;
 		Dialog dialog = new Dialog(this);
 		String dialogTitle;
 		switch (changelogtype)
 		{
 			case CHANGELOGTYPE_ROM:
 				dialogTitle = res.getString(R.string.changelog_title_rom);
 				break;
 			case CHANGELOGTYPE_THEME:
 				dialogTitle = res.getString(R.string.changelog_title_theme);
 				break;
 			case CHANGELOGTYPE_APP:
 				dialogTitle = res.getString(R.string.changelog_title_app);
 				break;
 			default:
 				return;
 		}
 		dialog.setTitle(dialogTitle);
 		dialog.setContentView(R.layout.changelog);
 		LinearLayout main = (LinearLayout) dialog.findViewById(R.id.ChangelogLinearMain);
 		
 		//Foreach Version
 		for (Version v:ChangelogList)
 		{
 			if (v.ChangeLogText.isEmpty())
 			{
 				continue;
 			}
 			ChangelogEmpty = false;
 			TextView versiontext = new TextView(this);
 			versiontext.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
 			versiontext.setGravity(Gravity.CENTER);
 			versiontext.setTextColor(Color.RED);
 			versiontext.setText("Version " + v.Version);
 			versiontext.setTypeface(null, Typeface.BOLD);
 			versiontext.setTextSize((versiontext.getTextSize() * (float)1.5));
 			main.addView(versiontext);
 			//Foreach Changelogtext
 			for(String Change:v.ChangeLogText)
 			{
 				LinearLayout l = new LinearLayout(this);
 				l.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.FILL_PARENT));
 				l.setGravity(Gravity.CENTER_VERTICAL);
 				ImageView i = new ImageView(this);
 				i.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
 				i.setImageResource(R.drawable.icon);
 				l.addView(i);
 				TextView ChangeText = new TextView(this);
 				ChangeText.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
 				ChangeText.setText(Change);
 				l.addView(ChangeText);
 				main.addView(l);
 				//Horizontal Line
 				View ruler = new View(this);
 				ruler.setBackgroundColor(Color.WHITE);
 				main.addView(ruler, new ViewGroup.LayoutParams( ViewGroup.LayoutParams.FILL_PARENT, 1));
 			}
 		}
 		if(!ChangelogEmpty)
 			dialog.show();
 		else
 			Toast.makeText(this, res.getString(R.string.no_changelog_found), Toast.LENGTH_SHORT).show();
 		System.gc();
 	}
 
 	@Override
 	public void updateDownloadProgress(final int downloaded, final int total, final String downloadedText, final String speedText, final String remainingTimeText)
 	{
 		if(mProgressBar ==null)return;
 
 		mProgressBar.post(new Runnable()
 		{
 			public void run()
 			{
 				if(total < 0)
 				{
 					mProgressBar.setIndeterminate(true);
 				}
 				else
 				{
 					mProgressBar.setIndeterminate(false);
 					mProgressBar.setMax(total);
 				}
 				mProgressBar.setProgress(downloaded);
 
 				mDownloadedBytesTextView.setText(downloadedText);
 				mDownloadSpeedTextView.setText(speedText);
 				mRemainingTimeTextView.setText(remainingTimeText);
 			}
 		});
 	}
 	
 	@Override
 	public void updateDownloadMirror(final String mirror)
 	{
 		if(mDownloadMirrorTextView == null) return;
 
 		mDownloadMirrorTextView.post(new Runnable()
 		{
 			public void run()
 			{
 				mDownloadMirrorTextView.setText(mirror);
 				mMirrorName = mirror;
 			}
 		});
 	}
 
 	private void showConfigActivity()
 	{
 		Intent i = new Intent(this, ConfigActivity.class);
 		startActivity(i);
 	}
 
 	private void checkForUpdates()
 	{
 		ProgressDialog pg = ProgressDialog.show(this, getResources().getString(R.string.checking_for_updates), getResources().getString(R.string.checking_for_updates), true, true);	
 		UpdateCheck u = new UpdateCheck(mUpdateServer, this, pg);
 		Thread t = new Thread(u);
 		t.start();
 	}
 
 	private void showAboutDialog()
 	{
 		Dialog dialog = new Dialog(this);
 		dialog.setTitle("About");
 		dialog.setContentView(R.layout.about);
 		TextView mVersionName = (TextView) dialog.findViewById(R.id.version_name_about_text_view);            
 		try
 		{
 			PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);                
 			mVersionName.setText("v " + pi.versionName);
 		}
 		catch (Exception e)
 		{
 			Log.e(TAG, "Can't find version name", e);
 			mVersionName.setText("v unknown");
 		}
 		dialog.show();			
 	}
 
 	private void scanQRURL()
 	{
 		IntentIntegrator.initiateScan(this);
 	}
 
 	private void downloadRequestedUpdate(UpdateInfo ui)
 	{
 		switchToDownloadingLayout(ui);
 		mUpdateDownloaderServiceIntent.putExtra(UpdateDownloaderService.KEY_REQUEST, UpdateDownloaderService.REQUEST_DOWNLOAD_UPDATE);
 		mUpdateDownloaderServiceIntent.putExtra(UpdateDownloaderService.KEY_UPDATE_INFO, ui);
 		startService(mUpdateDownloaderServiceIntent);
 		Toast.makeText(this, R.string.downloading_update, Toast.LENGTH_SHORT).show();
 	}
 
 	private boolean deleteOldUpdates()
 	{
 		boolean success = false;
 		if (mUpdateFolder.exists() && mUpdateFolder.isDirectory())
 		{
 			deleteDir(mUpdateFolder);
 			mUpdateFolder.mkdir();
 			Log.e(TAG, "Updates deleted and UpdateFolder created again");
 			success=true;
 			Toast.makeText(this, R.string.delete_updates_success_message, Toast.LENGTH_LONG).show();
 		}
 		else if (!mUpdateFolder.exists())
 		{
 			Toast.makeText(this, R.string.delete_updates_noFolder_message, Toast.LENGTH_LONG).show();
 		}
 		else
 		{
 			Toast.makeText(this, R.string.delete_updates_failure_message, Toast.LENGTH_LONG).show();
 		}
 		return success;
 	}
 	
 	private boolean deleteUpdate(String filename)
 	{
 		boolean success = false;
 		if (mUpdateFolder.exists() && mUpdateFolder.isDirectory())
 		{
 			File ZIPfiletodelete = new File(mUpdateFolder + "/" + filename);
 			File MD5filetodelete = new File(mUpdateFolder + "/" + filename + ".md5sum");
 			if (ZIPfiletodelete.exists())
 			{
 				ZIPfiletodelete.delete();
 			}
 			else
 			{
 				Log.e(TAG, "Update to delete not found");
 				Log.e(TAG, "Zip File: "+ZIPfiletodelete.getAbsolutePath());
 				return false;
 			}
 			if (MD5filetodelete.exists())
 			{
 				MD5filetodelete.delete();
 			}
 			else
 			{
 				Log.e(TAG, "MD5 to delete not found. No Problem here.");
 				Log.e(TAG, "MD5 File: "+MD5filetodelete.getAbsolutePath());
 			}
 			ZIPfiletodelete = null;
 			MD5filetodelete = null;
 			
 			success=true;
 			Toast.makeText(this, MessageFormat.format(getResources().getString(R.string.delete_single_update_success_message), filename), Toast.LENGTH_LONG).show();
 		}
 		else if (!mUpdateFolder.exists())
 		{
 			Toast.makeText(this, R.string.delete_updates_noFolder_message, Toast.LENGTH_LONG).show();
 		}
 		else
 		{
 			Toast.makeText(this, R.string.delete_updates_failure_message, Toast.LENGTH_LONG).show();
 		}
 		return success;
 	}
 
 	public static boolean deleteDir(File dir)
 	{
 		if (dir.isDirectory())
 		{ 	
 			String[] children = dir.list();
 			for (int i=0; i<children.length; i++)
 			{
 				boolean success = deleteDir(new File(dir, children[i]));
 				if (!success)
 				{
 					return false;
 				}
 			}
 		}
 		// The directory is now empty so delete it
 		return dir.delete();
 	}
 
 	public void onActivityResult(int requestCode, int resultCode, Intent intent)
 	{
 		IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
 		if (null != scanResult)
 		{
 			String result = scanResult.getContents();
 			if (null != result && !result.equals("") )
 			{
 				if (result.contains("zip"))
 				{
 					UpdateInfo ui = new UpdateInfo();
 					ui.updateFileUris = new LinkedList<URI>();
 					try
 					{
 						ui.updateFileUris.add(new URI(result));
 					}
 					catch (URISyntaxException e)
 					{
 						Log.e(TAG, "Exception while adding URL from QR Scan", e);
 					}
 					String[] tmp = result.split("/");
 					ui.fileName = tmp[tmp.length-1];
 					ui.name = ui.fileName;
 
 					Log.d(TAG, "Scanned QR Code: " + scanResult.getContents());
 					downloadRequestedUpdate(ui);
 				}
 				else
 				{
 					Toast.makeText(getBaseContext(), "Scanned result is not a zip. Please check.", Toast.LENGTH_LONG).show();
 				}
 			}
 			else
 			{
 				Toast.makeText(getBaseContext(), "No result was received. Please try again.", Toast.LENGTH_LONG).show();
 			}
 
 		}
 		else
 		{
 			Toast.makeText(getBaseContext(), "No result was received. Please try again.", Toast.LENGTH_LONG).show();
 		}
 	}
 
 	private boolean deleteOldVersionsOfUpdater()
 	{
 		try
 		{
 			String packageName = "cmupdater.ui";
 			PackageManager p = getPackageManager();
 			//This throws an Exception, when the Package is not found
 			PackageInfo a = p.getPackageInfo(packageName, 0);
 			if (a!=null && a.versionCode < 310)
 			{
 				Log.i(TAG, "Old VersionCode: "+a.versionCode);
 				Intent intent1 = new Intent(Intent.ACTION_DELETE); 
 				Uri data = Uri.fromParts("package", packageName, null); 
 				intent1.setData(data);
 				Toast.makeText(getBaseContext(), R.string.toast_uninstall_old_Version, Toast.LENGTH_LONG).show();
 				startActivity(intent1);
 				Log.i(TAG, "Uninstall Activity started");
 				return true;
 			}
 			else
 			{
 				throw new PackageManager.NameNotFoundException();
 			}
 		}
 		catch (PackageManager.NameNotFoundException e)
 		{
 			//No old Version found, so we return true
 			Log.i(TAG, "No old Version found");
 			return true;
 		}
 		catch (Exception e)
 		{
 			//Other Exception
 			Log.e(TAG, "Exception while trying to uninstall old Versions of the App", e);
 			return false;
 		}
 	}
 }
 
 /**
  * Filename Filter for getting only Files that matches the Given Extensions 
  * Extensions can be split with |
  * Example: .zip|.md5sum  
  *
  * @param  Extensions  String with supported Extensions. Split multiple Extensions with |
  * @return      true when file Matches Extension, otherwise false
  */
 class UpdateFilter implements FilenameFilter
 {
 	private String[] mExtension;
 	
 	public UpdateFilter(String Extensions)
 	{
 		mExtension = Extensions.split("\\|");
 	}
 	
 	public boolean accept(File dir, String name)
 	{
 		for (String Ext : mExtension)
 		{
 			if (name.endsWith(Ext))
 				return true;
 		}
 		return false;
 	}
 }
