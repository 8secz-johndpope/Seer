 /*
  *  This file is part of SWADroid.
  *
  *  Copyright (C) 2012 Helena Rodriguez Gijon <hrgijon@gmail.com>
  *
  *  SWADroid is free software: you can redistribute it and/or modify
  *  it under the terms of the GNU General Public License as published by
  *  the Free Software Foundation, either version 3 of the License, or
  *  (at your option) any later version.
  *
  *  SWADroid is distributed in the hope that it will be useful,
  *  but WITHOUT ANY WARRANTY; without even the implied warranty of
  *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  *  GNU General Public License for more details.
  *
  *  You should have received a copy of the GNU General Public License
  *  along with SWADroid.  If not, see <http://www.gnu.org/licenses/>.
  */
 package es.ugr.swad.swadroid.modules.downloads;
 
 import java.io.File;
 import java.io.FileNotFoundException;
 import java.io.IOException;
 import java.net.MalformedURLException;
 import java.util.ArrayList;
 import java.util.List;
 
 import com.android.dataframework.DataFramework;
 
 import android.app.Activity;
 import android.app.AlertDialog;
 import android.content.DialogInterface;
 import android.content.Intent;
 import android.net.Uri;
 import android.os.Bundle;
 import android.os.Environment;
 import android.util.Log;
 import android.view.View;
 import android.view.View.OnClickListener;
 import android.webkit.MimeTypeMap;
 import android.widget.AdapterView;
 import android.widget.AdapterView.OnItemClickListener;
 import android.widget.AdapterView.OnItemSelectedListener;
 import android.widget.GridView;
 import android.widget.ImageButton;
 import android.widget.ImageView;
 import android.widget.Spinner;
 import android.widget.TextView;
 import android.widget.ArrayAdapter;
 import android.widget.Toast;
 import android.webkit.MimeTypeMap;
 import es.ugr.swad.swadroid.Global;
 import es.ugr.swad.swadroid.MenuActivity;
 import es.ugr.swad.swadroid.R;
 import es.ugr.swad.swadroid.model.Course;
 import es.ugr.swad.swadroid.model.DataBaseHelper;
 import es.ugr.swad.swadroid.model.Group;
 import es.ugr.swad.swadroid.model.GroupType;
 import es.ugr.swad.swadroid.model.Model;
 import es.ugr.swad.swadroid.modules.GroupTypes;
 import es.ugr.swad.swadroid.modules.Groups;
 
 /**
  * Activity to navigate through the directory tree of documents and to manage
  * the downloads of documents
  * 
  * @author Helena Rodriguez Gijon <hrgijon@gmail.com>
  * */
 public class DownloadsManager extends MenuActivity {
 	/**
 	 * Class that contains the directory tree and gives information of each
 	 * level
 	 * */
 	private DirectoryNavigator navigator;
 
 	/**
 	 * Specifies whether to display the documents or the shared area of the
 	 * subject 1 specifies documents area 2 specifies shared area
 	 * */
 	private int downloadsAreaCode = 0;
 	/**
 	 * Specifies chosen group to show its documents
 	 * 0 - 
 	 * */
 	private long chosenGroupCode = 0;
 	/**
 	 * String that contains the xml files recevied from the web service
 	 * */
 	private String tree;
 
 	/**
 	 * Downloads tag name for Logcat
 	 */
 	public static final String TAG = Global.APP_TAG + " Downloads";
 	
 	/**
 	 * List of group of the selected course to which the user belongs
 	 * */
 	private List<Group> groups;
 	
 	/**
 	 * Indicates if the groups has been requested
 	 * */
 	private boolean groupsRequested = false;
 	
 	/**
 	 * Indicates whether the refresh button was pressed
 	 * */
 	private boolean refresh = false;
 	
 	/**
 	 * Path to the directory where files will be located
 	 * */
 	private String directoryPath = null; 
 	
 	private GridView grid;
 
 	private ImageView moduleIcon = null;
 	private TextView moduleText = null;
 	private TextView currentPathText;
 	private TextView moduleCourseName = null;
 	
 	private AlertDialog fileOptions = null;
 	
 	String chosenNodeName = null;
 	String fileName = null;
 	
 
 	@Override
 	protected void onStart() {
 		super.onStart();
 		if(groupsRequested){
 			if(navigator == null)
 				requestDirectoryTree();
 		}else{
 			List<Model> rows = dbHelper.getAllRows(Global.DB_TABLE_GROUP_TYPES, "courseCode = " + Global.getSelectedCourseCode() , null);
 			if(rows.size() != 0){
 				Intent activity = new Intent(getBaseContext(),Groups.class);
 				activity.putExtra("courseCode", Global.getSelectedCourseCode());
 				startActivityForResult(activity,Global.GROUPS_REQUEST_CODE);
 			}else{
 				Intent activity = new Intent(getBaseContext(),GroupTypes.class);
 				activity.putExtra("courseCode",  Global.getSelectedCourseCode());
 				startActivityForResult(activity,Global.GROUPTYPES_REQUEST_CODE);
 			}
 		}
 		
 	}
 
 	@Override
 	protected void onCreate(Bundle savedInstanceState) {
 		super.onCreate(savedInstanceState);
 		setContentView(R.layout.navigation);
 		
 		checkMediaAvailability();
 		
 		downloadsAreaCode = getIntent().getIntExtra("downloadsAreaCode",
 				Global.DOCUMENTS_AREA_CODE);
 		
 		final CharSequence[] items = {getString(R.string.openFile) , getString(R.string.downloadFile) , getString(R.string.deleteFile) };
 
 		AlertDialog.Builder builder = new AlertDialog.Builder(this);
 		builder.setTitle(getString(R.string.fileOptions));
 		builder.setItems(items, new DialogInterface.OnClickListener() {
 		    public void onClick(DialogInterface dialog, int item) {
 		    	switch(item){
 			    	case 0:
 			    		openFileDefaultApp(directoryPath+File.separator+fileName);
 			    		break;
 			    	case 1:
 			    		callGetFile(chosenNodeName);
 			    		break;
 			    	case 2:
 			    		File f =  new File(directoryPath, fileName);
 			    		if(f.exists())
 			    			f.delete();
 			    		//TODO change icon file to show the file is not downloaded	
 			    		break;
 		    		
 		    	}
 		    }
 		});
 		final AlertDialog fileOptions = builder.create();
 		
 		
 		grid = (GridView) this.findViewById(R.id.gridview);
 		grid.setOnItemClickListener((new OnItemClickListener() {
 			public void onItemClick(AdapterView<?> parent, View v,
 					int position, long id) {
 				TextView text = (TextView) v.findViewById(R.id.icon_text);
 				chosenNodeName = text.getText().toString();
 
 				Long fileCode = navigator.getFileCode(chosenNodeName);
 				if(fileCode == -1) //it is a directory therefore navigates into it
 					updateView(navigator.goToSubDirectory(chosenNodeName));
 				else{ //it is a files therefore gets its information throught web service GETFILE
 
 					//TODO
 					callGetFile(chosenNodeName);
 				}
 			}
 		}));
 
 		ImageButton homeButton = (ImageButton) this
 				.findViewById(R.id.home_button);
 		homeButton.setOnClickListener((new OnClickListener() {
 
 			@Override
 			public void onClick(View v) {
 				updateView(navigator.goToRoot());
 				directoryPath = getDirectoryPath();
 			}
 
 		}));
 
 		ImageButton parentButton = (ImageButton) this
 				.findViewById(R.id.parent_button);
 		parentButton.setOnClickListener((new OnClickListener() {
 
 			@Override
 			public void onClick(View v) {
 				updateView(navigator.goToParentDirectory());
 				directoryPath = getDirectoryPath();
 			}
 
 		}));
 
 		ImageButton refreshButton = (ImageButton) this
 				.findViewById(R.id.refresh_button);
 		refreshButton.setOnClickListener((new OnClickListener() {
 
 			@Override
 			public void onClick(View v) {
 
 				refresh = true;
 				requestDirectoryTree();
 			}
 
 		}));
 
 	}
 
 	@Override
 	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
 		super.onActivityResult(requestCode, resultCode, data);
 		if (resultCode == Activity.RESULT_OK) {
 			switch (requestCode) {
 			// After get the list of courses, a dialog is launched to choice the
 			// course
 			case Global.DIRECTORY_TREE_REQUEST_CODE:
 				 tree = data.getStringExtra("tree");// this is the right call, disable until getDirectoryTreeDownload is fixed
				//tree = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><tree><dir name=\"2012-04-16 4Hackathon\"></dir><file name=\"SWADroid-4hackathon.odp\"><size>5366058</size><time>1335209681</time><license>CC Reconocimiento - No comercial - Compartir bajo la misma licencia</license><publisher>Antonio Caas Vargas</publisher><photo></photo></file></tree>";
 				if (!refresh)
 					setMainView();
 				else {
 					refresh = false;
 					refresh();
 				}
 				break;
 			case Global.GETFILE_REQUEST_CODE:
 				Log.i(TAG, "Correct get file");
 				String url = data.getExtras().getString("link");
 				String infoFile = data.getExtras().getString("name");
 				if(url != null && infoFile != null){
 					directoryPath =  getDirectoryPath();
 					File f = new File(directoryPath, infoFile);
 					if(!f.exists())
 						downloadFile(directoryPath,url);
 				}
 				break;
 			case Global.GROUPS_REQUEST_CODE:
 				groupsRequested = true;
 				this.loadGroupsSpinner();
 				requestDirectoryTree();
 				break;	
 			case Global.GROUPTYPES_REQUEST_CODE:	
 				Intent activity = new Intent(getBaseContext(),Groups.class);
 				activity.putExtra("courseCode", Global.getSelectedCourseCode());
 				startActivityForResult(activity,Global.GROUPS_REQUEST_CODE);
 				break;
 			}
 			
 		}
 	}
 
 	private void setMainView() {
 		if (moduleIcon == null) {
 			if (downloadsAreaCode == Global.DOCUMENTS_AREA_CODE) {
 				moduleIcon = (ImageView) this.findViewById(R.id.moduleIcon);
 				moduleIcon.setBackgroundResource(R.drawable.folder);
 
 				moduleText = (TextView) this.findViewById(R.id.moduleName);
 				moduleText.setText(R.string.documentsDownloadModuleLabel);
 			} else { // SHARE_AREA_CODE
 				moduleIcon = (ImageView) this.findViewById(R.id.moduleIcon);
 				moduleIcon.setBackgroundResource(R.drawable.folderusers);
 
 				moduleText = (TextView) this.findViewById(R.id.moduleName);
 				moduleText.setText(R.string.sharedsDownloadModuleLabel);
 			}
 		}
 
 		currentPathText = (TextView) this.findViewById(R.id.path);
 
 		navigator = new DirectoryNavigator(tree);
 		// GridView
 		ArrayList<DirectoryItem> items = (ArrayList<DirectoryItem>) navigator
 				.goToRoot();
 		currentPathText.setText(navigator.getPath());
 		grid.setAdapter(new NodeAdapter(this, items));
 	}
 
 	private void refresh() {
 		navigator.refresh(tree);
 
 	}
 
 	private void updateView(ArrayList<DirectoryItem> items) {
 		currentPathText.setText(navigator.getPath());
 		((NodeAdapter) grid.getAdapter()).change(items);
 
 	}
 	/**
 	 * If there are not groups to which the user belong in the database, it makes the request
 	 * */
 	private void loadGroupsSpinner(){
 		groups = dbHelper.getGroups(Global.getSelectedCourseCode());
 		//remove groups that do not have a file zone assigned 
 		int j = 0;
 		while(j < groups.size()){
 			if(groups.get(j).getDocumentsArea() != 0 && groups.get(j).isMember())
 				++j;
 			else
 				groups.remove(j);
 		}
 		
 		if(!groups.isEmpty() ){ //there are groups in the selected course, therefore the groups spinner should be loaded
 			this.findViewById(R.id.courseSelectedText).setVisibility(View.GONE);
 			Spinner groupsSpinner = (Spinner)this.findViewById(R.id.groupSpinner);
 			groupsSpinner.setVisibility(View.VISIBLE);
 			
 			ArrayList<String> spinnerNames = new ArrayList<String>(groups.size()+1);
 			spinnerNames.add(getString(R.string.course)+"-" + Global.getSelectedCourseShortName());
 			for(int i=0;i<groups.size();++i){
 				Group g = groups.get(i);
 				GroupType gType = dbHelper.getGroupTypeFromGroup(g.getId());
 				spinnerNames.add(getString(R.string.group)+"-" + gType.getGroupTypeName() + " "+ g.getGroupName() );
 			}
 			
 			ArrayAdapter<String> adapter = new ArrayAdapter<String> (this,android.R.layout.simple_spinner_item,spinnerNames);
 			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
 			groupsSpinner.setAdapter(adapter);
 			groupsSpinner.setOnItemSelectedListener(new onItemSelectedListener());
 			
 		}else{
 			if(groupsRequested){ //there are not groups in the selected course, therefore only the course name should be loaded
 				this.findViewById(R.id.courseSelectedText).setVisibility(View.VISIBLE);
 				this.findViewById(R.id.groupSpinner).setVisibility(View.GONE);
 				
 				TextView courseNameText = (TextView) this.findViewById(R.id.courseSelectedText);
 				courseNameText.setText(Global.getSelectedCourseShortName());
 			}
 		}
 	}
 	
 	private class onItemSelectedListener implements OnItemSelectedListener{
 
 		@Override
 		public void onItemSelected(AdapterView<?> parent, View view, int position,
 				long id) {
 			//if the position is 0, it is chosen the whole course. Otherwise a group has been chosen
 			long newGroupCode = position==0? 0 : groups.get(position).getId();
 			if(chosenGroupCode != newGroupCode){
 				chosenGroupCode = newGroupCode;
 				requestDirectoryTree();
 			}	
 
 		}
 
 		@Override
 		public void onNothingSelected(AdapterView<?> arg0) {
 		}
 
 	}
 	
 	private void requestDirectoryTree(){
 		Intent activity;
 		activity = new Intent(getBaseContext(), DirectoryTreeDownload.class);
 		activity.putExtra("treeCode", downloadsAreaCode);
 		activity.putExtra("groupCode", (int)chosenGroupCode);
 		startActivityForResult(activity, Global.DIRECTORY_TREE_REQUEST_CODE);
 	}
 	
 	/**
 	 * It checks if the external storage is available 
 	 * @return 0 - if external storage can not be read either wrote
 	 * 			1 - if external storage can only be read
 	 * 			2 - if external storage can be read and wrote
 	 * */
 
 	private int checkMediaAvailability(){
 		boolean mExternalStorageAvailable = false;
 		boolean mExternalStorageWriteable = false;
 		String state = Environment.getExternalStorageState();
 		int returnValue = 0;
 		if (Environment.MEDIA_MOUNTED.equals(state)) {
 		    // We can read and write the media
 		    mExternalStorageAvailable = mExternalStorageWriteable = true;
 		    Toast.makeText(this, "External Storage can be read and wrote", Toast.LENGTH_LONG).show();
 		    returnValue = 2;
 		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
 		    // We can only read the media
 		    mExternalStorageAvailable = true;
 		    mExternalStorageWriteable = false;
 		    Toast.makeText(this,"External Storage can only be read", Toast.LENGTH_LONG).show();
 		    returnValue = 1;
 		} else {
 		    // Something else is wrong. It may be one of many other states, but all we need
 		    //  to know is we can neither read nor write
 		    mExternalStorageAvailable = mExternalStorageWriteable = false;
 		    Toast.makeText(this, "External Storage can not be read either wrote", Toast.LENGTH_LONG).show();
 		    returnValue = 0;
 		}
 		return returnValue;
 	}
 	
 	/**
 	 * it gets the directory path where the files will be located.This will be /$EXTERNAL_STORAGE/SWADroid/courseCode shortName Course. This directory is created in case it does not exist
 	 * */
 	private String getDirectoryPath(){
 		String path = null;
 		String swadroidDirName = Environment.getExternalStorageDirectory()+File.separator+getString(R.string.app_name);
 		String courseDirName = swadroidDirName +File.separator + Global.getSelectedCourseCode()+ File.separator +String.valueOf(chosenGroupCode)+navigator.getPath();
 		if(checkMediaAvailability() == 2){
 			File courseDir = new File(courseDirName);
 			if(courseDir.exists()){
 				path = courseDirName;
 			}else if(courseDir.mkdirs())
 					path = courseDirName;
 				
 			
 			/*if(courseDir.exists()){
 				path = courseDirName;
 			}else {
 				File mainDir = new File(swadroidDirName);
 				if(!mainDir.exists()){
 					
 				}
 			}
 			
 			
 			File mainDir = new File(swadroidDirName);
 			boolean mainDirB = mainDir.exists();
 			
 			if(!mainDirB){
 				boolean mainDirCreated = mainDir.mkdir();
 			//if(!mainDir.exists()){
 				if(mainDirCreated){
 				//if(mainDir.mkdir()){
 					String courseDirName = swadroidDirName +
 					
 					if(!courseDir.exists())
 						if(courseDir.mkdir())
 							path = new String(courseDirName);
 				}
 			}*/
 		}
 		return path;
 	}
 	
 	private void downloadFile(String directory, String url){
 		if(downloadsAreaCode == Global.DOCUMENTS_AREA_CODE)
 			new FileDownloaderAsyncTask(getApplicationContext(),false).execute(directory,url);
 		if(downloadsAreaCode == Global.SHARE_AREA_CODE)
 			new FileDownloaderAsyncTask(getApplicationContext(),true).execute(directory,url);
 	}
 	private void openFileDefaultApp(String absolutePath){
 		File file = new File(absolutePath);
 		if(file.exists()){
 			Intent intent = new Intent();
 			intent.setAction(android.content.Intent.ACTION_VIEW);
 			int lastDotIndex = absolutePath.lastIndexOf(".");
 			String extension = absolutePath.substring(lastDotIndex+1);
 			String MIME = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
 			intent.setDataAndType(Uri.fromFile(file), MIME);
 			startActivity(intent);
 		}
 	}
 	private void callGetFile(String fileName){
 	    Intent activity;
 	    activity = new Intent(getBaseContext(), GetFile.class);
 	    activity.putExtra("courseCode", Global.getSelectedCourseCode());
 	    activity.putExtra("groupCode", chosenGroupCode);
 	    activity.putExtra("treeCode", downloadsAreaCode);
 	    activity.putExtra("path", navigator.getPath() + fileName);
 	    startActivityForResult(activity, Global.GETFILE_REQUEST_CODE);
 	  }
 }
