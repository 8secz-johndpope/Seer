 package org.fourdnest.androidclient.ui;
 
 import java.io.File;
 import java.util.List;
 
 import org.fourdnest.androidclient.Egg;
 import org.fourdnest.androidclient.EggManager;
 import org.fourdnest.androidclient.R;
 import org.fourdnest.androidclient.Tag;
 import org.fourdnest.androidclient.services.SendQueueService;
 import org.fourdnest.androidclient.services.TagSuggestionService;
 
 import android.app.AlertDialog;
 import android.app.Dialog;
 import android.content.ContentResolver;
 import android.content.ContentValues;
 import android.content.DialogInterface;
 import android.content.Intent;
 import android.database.Cursor;
 import android.graphics.Bitmap;
 import android.graphics.BitmapFactory;
 import android.net.Uri;
 import android.os.Bundle;
 import android.provider.MediaStore;
 import android.support.v4.content.CursorLoader;
 import android.view.Menu;
 import android.view.MenuInflater;
 import android.view.MenuItem;
 import android.view.View;
 import android.view.View.OnClickListener;
 import android.webkit.MimeTypeMap;
 import android.widget.Button;
 import android.widget.EditText;
 import android.widget.ImageButton;
 import android.widget.ImageView;
 import android.widget.LinearLayout;
 import android.widget.RelativeLayout;
 import android.widget.ScrollView;
 
 public class NewEggActivity extends NestSpecificActivity{
 
 	
 	/*
 	 * currentMediaItemType is used to track what media item is selected
 	 * (if any). This decides what UI elements to show.
 	 */
 	
 	private enum mediaItemType{
 		none, image, video, audio, multiple //note that multiple is currently not used
 	}
 	protected mediaItemType currentMediaItem = mediaItemType.none;
 	protected static final int SELECT_PICTURE = 1; //this is needed for selecting picture
 	protected static final int SELECT_AUDIO = 2;
 	protected static final int SELECT_VIDEO = 3;
 	protected static final int CAMERA_PIC_REQUEST = 4;
 	protected static final int CAMERA_VIDEO_REQUEST = 5;
 	protected static final int AUDIO_RECORER_REQUEST = 6;
 	protected String currentEggID = "0"; //0 if new egg
 
 	private static final int RESULT_OK = -1; // apparently its -1... dunno
 	
 	/*
 	 *  ID values for dialogues
 	 */
 	static final int DIALOG_ASK_AUDIO = 0;
 	static final int DIALOG_ASK_IMAGE = 1;
 	static final int DIALOG_ASK_VIDEO = 2;
 	//static final int DIALOG_GAMEOVER_ID = 1;
 	
 
 	private String fileURL = "";
 	private String realFileURL = "";
 	private String selectedFilePath;
 	private String filemanagerstring;
 	private ImageView thumbNailView;
 	private RelativeLayout upperButtons;
 	private Uri capturedImageURI;
 	private TaggingTool taggingTool;
 	private boolean kioskMode; //tells us what ever kiosk mode is on, set up in getContentLayout
 
 	/**
 	 * A method required by the mother class. Populates the view used by nestSpesificActivity according
 	 * to layout and requirements of NewEggActivity. Called in mother classes OnCreate method.
 	 */
 	
 	@Override
 	public View getContentLayout(View view) {
 		//super.onCreate(savedInstanceState);
 		//setContentView(R.layout.new_egg_view);
 		this.getApplicationContext();
 		Bundle extras = getIntent().getExtras(); 
 		if(extras !=null)
 		{
 			/*
 			 * I really don't want NULL pointer exceptions. 
 			 */
 			if(extras.containsKey("eggID")){
 				currentEggID = extras.getString("eggID");
 				this.recoverDataFromExistingEGG(); //recovers the data from the existing egg
 			}
 			if(extras.containsKey("pictureURL")){
 				fileURL = extras.getString("pictureURL"); //not really sure what this is for but lets hope its useful
 			}
 		}
 		if (!currentEggID.equals("0")){
 			
 		}
 		
 		this.kioskMode = super.application.getKioskModeEnabled();
 		this.upperButtons = (RelativeLayout) view.findViewById(R.id.new_egg_upper_buttons);
 		this.thumbNailView = (ImageView) view.findViewById(R.id.new_photo_egg_thumbnail_view);
 		/*
 		 * Adds a onClickListener to the preview image so we know when to open a thumbnail
 		 */
 		
         thumbNailView.setOnClickListener(new OnClickListener() {
 
             public void onClick(View arg0) {
                 // in onCreate or any event where your want the user to
                 // select a file
             	Intent i = new Intent(Intent.ACTION_VIEW);
             	/*
             	 * Creates an intent for previewing media with correct type of media
             	 * selected
             	 * 
             	 * LEET HACKS, needs file:// to the front or will crash !!!!!!!!!
             	 */
             	
             	if(currentMediaItem==mediaItemType.image){
                 	i.setDataAndType(Uri.parse("file://"+realFileURL), "image/*");
             	}
             	else if(currentMediaItem==mediaItemType.audio){
             		i.setDataAndType(Uri.parse("file://"+realFileURL), "audio/*");
             	}
             	else if(currentMediaItem==mediaItemType.video){
                 	i.setDataAndType(Uri.parse("file://"+realFileURL), "video/*");
             	}
             	startActivity(i);
             }
         });
 	
         
         /*
          * Adds on click listener to send button, so we know when to send the egg to 
          * the server
          */
         
         Button sendButton = (Button) view.findViewById(R.id.new_photo_egg_send_egg_button);
         sendButton.setOnClickListener(new OnClickListener() {
 			
 			public void onClick(View v) {		
 				//TODO: Proper implementation
 				Egg egg = new Egg();
 				egg.setAuthor("Saruman_The_White_42");
 				egg.setCaption(((EditText)findViewById(R.id.new_photo_egg_caption_view)).getText().toString());
 				egg.setLocalFileURI(Uri.parse("file://"+realFileURL));
 				List<Tag> tags = NewEggActivity.this.taggingTool.getCheckedTags();
 				egg.setTags(tags);
 				SendQueueService.sendEgg(getApplication(), egg);
 				TagSuggestionService.setLastUsedTags(getApplication(), tags);
 				
 				// Go to ListStreamActivity after finishing
 				v.getContext().startActivity(new Intent(v.getContext(), ListStreamActivity.class));
 				v.getContext();
 				
 			}
 		});
         
 		/*
 		* This onClick listener is used to popup a dialogue that determines what ever to
 		* open the image gallery or the camera
 		 */
         
     	((ImageButton) view.findViewById(R.id.select_image))
 		.setOnClickListener(new OnClickListener() {
 			public void onClick(View arg0) {
 				/*
 				 * If kiosk mode is enabled, open camera directly. Othervice 
 				 */
 				if (kioskMode){
 					startIntent(CAMERA_PIC_REQUEST);
 				}
 				else{
 				showDialog(DIALOG_ASK_IMAGE);
 				}
 			}
 		});
     	
 		/*
 		* This onClick listener pops up a default internal android dialogue that asks what ever 
 		 * to open the audio gallery or the audio recorder.
 		 */
     	
        	((ImageButton) view.findViewById(R.id.select_audio))
     		.setOnClickListener(new OnClickListener() {
     			public void onClick(View arg0) {
     				// in onCreate or any event where your want the user to
     				// select a file
     				if(kioskMode){
     					startIntent(AUDIO_RECORER_REQUEST);
     				}
     				else{
     					startIntent(SELECT_AUDIO);
     				}
     			}
     		});
        	
 		/*
 		* This onClick listener is used to popup a dialogue that determines what ever to
 		* open the video gallery or the video camera
 		 */
         
        	
        	((ImageButton) view.findViewById(R.id.select_video))
     		.setOnClickListener(new OnClickListener() {
     			public void onClick(View arg0) {
     				if(kioskMode){
     					startIntent(CAMERA_VIDEO_REQUEST);
     				}
     				// in onCreate or any event where your want the user to
     				// select a file
     				showDialog(DIALOG_ASK_VIDEO);
     			}
     		});	
       	LinearLayout inputsLinearLayout = (LinearLayout) view.findViewById(R.id.new_egg_inputs_linearlayout);
        	this.taggingTool = new TaggingTool(this.getApplicationContext(), inputsLinearLayout);
        	return view;
 	}
 	
 	
 	/**
 	 * 
 	 * Destroys the activity. Over-riden so we get rid of the tagging tool
 	 */
 	
     @Override
     public void onDestroy() {
     	super.onDestroy();
     	this.taggingTool.onDestroy();
     	this.taggingTool = null;
     }
 
     /**
      * 
      * A method used by the onCreate in NestSpesificActivity to recover the correct layout to use (used to initially
      * create the view which is then populated by getContentLayout). 
      *
      */
     
 	
 	public int getLayoutId() {
 		return R.layout.new_egg_view;
 	}
 	
 	
 	/**
 	 * Used to refresh the elements displayed when an media item is selected / unselected
 	 */
 	
 	private void refreshElements(){
 		
 		/*
 		 *  I used to have this work with a switch, but it didn't work for some strange reason
 		 */
 		
 			if (this.currentMediaItem == mediaItemType.image){ //image has been selected, we hide the selection buttons and show the preview thumbnail
 				upperButtons.setVisibility(View.GONE);
 				thumbNailView.setVisibility(View.VISIBLE);
 				ScrollView scrollView = (ScrollView) this.findViewById(R.id.new_egg_scroll_view);
 				File imgFile = new  File(fileURL);
 				if(imgFile.exists()){
 					realFileURL = imgFile.getAbsolutePath();
 				    Bitmap myBitmap = BitmapFactory.decodeFile(realFileURL);
 				    thumbNailView.setImageBitmap(myBitmap);
 				}
 				scrollView.postInvalidate(); //should cause a redraw.... should!
 			}
 			else if(this.currentMediaItem == mediaItemType.none){ //no media item is currently selected
 				thumbNailView.setVisibility(View.VISIBLE);	
 				upperButtons.setVisibility(View.GONE);
 			}
 			else if (this.currentMediaItem == mediaItemType.audio){ //audio item is selected
 			thumbNailView.setVisibility(View.VISIBLE);
 			upperButtons.setVisibility(View.GONE);
 			thumbNailView.setImageResource(R.drawable.note1);
 			File audioFile = new  File(fileURL);
 			realFileURL = audioFile.getAbsolutePath();
 		}
 			
 			else if (this.currentMediaItem == mediaItemType.video){ //video item is selected
 			thumbNailView.setVisibility(View.VISIBLE);
 			upperButtons.setVisibility(View.GONE);
 			thumbNailView.setImageResource(R.drawable.roll1);
 			File audioFile = new  File(fileURL);
 			realFileURL = audioFile.getAbsolutePath();
 		}
 	
 	}
 	
 	/**
 	 * 
 	 * This method creates the dialogues that the user uses to make selections on what ever 
 	 * to use the capture device or browse existing items.
 	 * 
 	 */
 	
 	protected Dialog onCreateDialog(int id) {
 	    Dialog dialog = null;
 	    switch(id) {
 	    case DIALOG_ASK_IMAGE: //determines that this dialogue is used to determine what ever to open image camera or image gallery
 	    	final CharSequence[] items = {getString(R.string.new_egg_dialogue_open_image_callery), getString(R.string.new_egg_dialogue_open_photo_camera)};// {getString (R.string.new_egg_dialogue_open_image_callery), getString(R.string.new_egg_dialogue_open_photo_camera)};
 	    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
 	    	builder.setTitle("Select Source");
 	    	builder.setItems(items, new DialogInterface.OnClickListener() {
 	    	    public void onClick(DialogInterface dialog, int item) {
 	    	    	if(item==0){ //this one means that user wants to open the image gallery
 	    	    		startIntent(SELECT_PICTURE);
 	    	    	}
 	    	    	else if(item==1){ //this one means  that user wants to open the image camera
 	    	    		//define the file-name to save photo taken by Camera activity
 	    	    		startIntent(CAMERA_PIC_REQUEST);
 	    	    	}
 	    	    }
 	    	});
 	    	dialog = builder.create();
 	    	break;
 	    
 	    case DIALOG_ASK_VIDEO: //this one is used to determine what ever to open a video camera or video gallery
 	    	final CharSequence[] videoItems = {getString(R.string.new_egg_dialogue_open_video_callery), getString(R.string.new_egg_dialogue_open_video_camera)};
 	    	AlertDialog.Builder videoBuilder = new AlertDialog.Builder(this);
 	    	videoBuilder.setTitle("Select Source");
 	    	videoBuilder.setItems(videoItems, new DialogInterface.OnClickListener() {
 	    	    public void onClick(DialogInterface dialog, int item) {
 	    	    	if(item==1){ //video camera requested
 	    	    		startIntent(CAMERA_VIDEO_REQUEST);
 	    	    	}
 	    	    	if(item==0){ //video gallery requested.
 	    	    		startIntent(SELECT_VIDEO);
 	    	    	}
 	    	    }
 	    	});
 	    	dialog = videoBuilder.create();
 	    	break;
 
 	    	    	
 	    	    
 	    
 	    default:
 	        dialog = null;
 	    }
 	    return dialog; //the requested dialoque is returned for displaying
 	}
 	
 
 	/**
 	 * This method is used once media item has been selected or captured. Request code determines
 	 * what ever a picture, audio or video was received. The method sets the fileURL to point at the correct file
 	 * and sets the type of currentMediaItem . Automatically called after intent finishes succesfully.
 	 */
 	
 	
 	public void onActivityResult(int requestCode, int resultCode, Intent data) {
 		if (resultCode == RESULT_OK) {
 			
 			String filePath = this.recoverMediaFileURL(requestCode, data);
 			if(requestCode == SELECT_PICTURE || requestCode == CAMERA_PIC_REQUEST){ //is there a neater way to format that?
 			this.currentMediaItem = mediaItemType.image;
 			}
 			else if(requestCode == SELECT_AUDIO || requestCode == AUDIO_RECORER_REQUEST){ //Audio always comes with SELECT_AUDIO code
 				this.currentMediaItem = mediaItemType.audio;
 			}
 			else if(requestCode == SELECT_VIDEO || requestCode==CAMERA_VIDEO_REQUEST){
 				this.currentMediaItem = mediaItemType.video; 
 			}
 			this.fileURL = filePath;
 			this.refreshElements();
 		}
 	}
 
 	
 
 	
 	/**
 	 * Creates the options menu on the press of the Menu button.
 	 * 
 	 * @param menu The menu to inflate
 	 * @return Boolean indicating success of creating the menu
 	 */
 	@Override
 	public boolean onCreateOptionsMenu(Menu menu) {
 		MenuInflater inflater = getMenuInflater();
 		inflater.inflate(R.menu.create_menu, menu);
 		return true;
 	}
 	
 	/**
 	 * Specifies the action to perform when a menu item is pressed.
 	 * 
 	 * @param item The MenuItem that was pressed
 	 * @return Boolean indicating success of identifying the item
 	 */
 	@Override
 	public boolean onOptionsItemSelected(MenuItem item) {
 		switch (item.getItemId()) {
 		case R.id.menu_create_pref:
 			startActivity(new Intent(this, PrefsActivity.class));
 			return true;
 		case R.id.menu_create_help:
 			//TODO create help for new egg
 			return true;
 		case R.id.menu_create_discard:
 			//TODO discard implementation
 			return true;
         case R.id.menu_create_drafts:
             startActivity(new Intent(this, ListDraftEggsActivity.class));
             return true;
 		}
 		return false;
 	}
 	
 	/**
 	 * Private method for recovering the file url from selected or captured media file.
 	 */
 	
 	private String recoverMediaFileURL(int requestCode, Intent data){
 		Uri selectedImageUri = null;
 		if(requestCode==CAMERA_PIC_REQUEST){
 
 					filemanagerstring = capturedImageURI.getPath();
 
 					// MEDIA GALLERY
 					selectedFilePath = getPath(capturedImageURI);
 		}
 		else{
 		selectedImageUri = data.getData();
 		// OI FILE Manager
 		filemanagerstring = selectedImageUri.getPath();
 		selectedFilePath = getPath(selectedImageUri);
 
 		}
 		// MEDIA GALLERY
 		
 		// NOW WE HAVE OUR WANTED STRING
 		String filePath = "";
 		if (selectedFilePath != null) {
 			filePath = selectedFilePath; //filepath is the right one
 		} else {
 			filePath = filemanagerstring; //filemanagerstring is the right one
 		}
 		return filePath;
 	}
 	
 	/**
 	 * Internal help method for recovering a correct string representation of the URI of a file
 	 */
 	
 	private String getPath(Uri uri) {
 		String[] projection = { MediaStore.Images.Media.DATA };
 
 		CursorLoader loader = new CursorLoader(this.getApplicationContext(), uri,
 				projection, null, null, null);
 		Cursor cursor = loader.loadInBackground();
 		if (cursor != null) {
 			// HERE YOU WILL GET A NULLPOINTER IF CURSOR IS NULL
 			// THIS CAN BE, IF YOU USED OI FILE MANAGER FOR PICKING THE MEDIA
 			int columnIndex = cursor
 					.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
 			cursor.moveToFirst();
 			return cursor.getString(columnIndex);
 		} else {
 			return null;
 		}
 	}
 	
 	/*
 	 * Private method for quick starting intents. Needed so we don't need to duplicate code AND for code
 	 * quality
 	 */
 	
 	private void startIntent(int intentType){
 		Intent intent = new Intent(); //all the cases are gonna need an intent
 		ContentValues values = null; //some cases need constantValues 
 		/*
 		 * I JUST CAN'T GET SWITCHES WORKING 
 		 */
 			if(intentType== SELECT_PICTURE){
 	    		intent.setType("image/*");
 	    		intent.setAction(Intent.ACTION_GET_CONTENT);
 	    		intent.addCategory(Intent.CATEGORY_OPENABLE);
 	    		startActivityForResult(
 					Intent.createChooser(intent, getString(R.string.new_egg_intent_select_picture)),	//the second argument is the title of intent
 					SELECT_PICTURE);
 			}
 			if(intentType== SELECT_AUDIO){ 
 				intent.setType("audio/*");
 				intent.setAction(Intent.ACTION_GET_CONTENT);
 				intent.addCategory(Intent.CATEGORY_OPENABLE);
 				startActivityForResult(
 						Intent.createChooser(intent, "Select Audio"),
 						SELECT_AUDIO);
 			}
 			if(intentType ==  CAMERA_PIC_REQUEST){
 	    		String fileName = "dpic.jpg"; //there is a string res for this but I decided not to use if for now 
 	    		//TODO:generate better filenames
 	    		//create parameters for Intent with filename
 	    		values = new ContentValues();
 	    		values.put(MediaStore.Images.Media.TITLE, fileName);
 	    		values.put(MediaStore.Images.Media.DESCRIPTION, getString(R.string.new_egg_intent_image_description));
 	    		/*
 	    		 * We are going to save the Uri to the image before actually taking the picture.
 	    		 * This was the way used in the example, so far I haven't been able to find a better
 	    		 * way (but there has to be one, this cant be good)
 	    		 */
 	    		capturedImageURI = getContentResolver().insert(
 	    		        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
 	    		//create new Intent
 	    		intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE );
 	    		intent.putExtra(MediaStore.EXTRA_OUTPUT, capturedImageURI); //tell the intent where to store the file
 	    		intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
 	    		startActivityForResult(intent, CAMERA_PIC_REQUEST);
 			}
 			if (intentType == SELECT_VIDEO){
 	    		intent.setType("video/*");
 	    		intent.setAction(Intent.ACTION_GET_CONTENT);
 	    		intent.addCategory(Intent.CATEGORY_OPENABLE);
 	    		startActivityForResult(
 					Intent.createChooser(intent, "Select Video"),
 					SELECT_VIDEO);
 			}
 			if (intentType == CAMERA_VIDEO_REQUEST){
 	    		values = new ContentValues();
 	    		values.put(MediaStore.Images.Media.DESCRIPTION,"Video captured for 4D Nest");
 	    		//create new Intent
 	    		intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE );
 	    		intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
 	    		startActivityForResult(intent, CAMERA_VIDEO_REQUEST);
 			}
 			if (intentType == AUDIO_RECORER_REQUEST){
 				intent = new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);
 				startActivityForResult(intent, AUDIO_RECORER_REQUEST);
 
 			}
 		
 		}
 	
 	/**
 	 * A local method for recovering data from an existing egg. Used when 
 	 * a draft is loaded for editing
 	 */
 	
 	private void recoverDataFromExistingEGG(){
 		int eggIDInt = Integer.valueOf(currentEggID);
 		EggManager draftManager = super.application.getDraftEggManager();
 		Egg existingEgg = draftManager.getEgg(eggIDInt);
 		Uri uri = existingEgg.getLocalFileURI();	
 		if (uri == null){
 			currentMediaItem = mediaItemType.none;
 		}
 		else {
 			ContentResolver cR = this.getContentResolver();
 			MimeTypeMap mime = MimeTypeMap.getSingleton();
 			String type = mime.getExtensionFromMimeType(cR.getType(uri));	
 			if (type.startsWith("image")){
 				this.currentMediaItem = mediaItemType.image;
 			}
 			else if (type.startsWith("audio")){
 				this.currentMediaItem = mediaItemType.audio;
 			}
 			else if (type.startsWith("video")){
 				this.currentMediaItem = mediaItemType.video;
 			}
 			fileURL = uri.toString();
 			this.refreshElements();
 		}
 		
 	}
 	
 	
 	}
