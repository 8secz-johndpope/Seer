 /*
  * Geopaparazzi - Digital field mapping on Android based devices
  * Copyright (C) 2010  HydroloGIS (www.hydrologis.com)
  *
  * This program is free software: you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program.  If not, see <http://www.gnu.org/licenses/>.
  */
 package eu.hydrologis.geopaparazzi.maps;
 
 import java.io.File;
 import java.io.IOException;
 import java.text.DecimalFormat;
 import java.text.MessageFormat;
 import java.util.Date;
 import java.util.HashMap;
 import java.util.LinkedHashMap;
 import java.util.List;
 import java.util.Map.Entry;
 import java.util.Set;
 
 import org.mapsforge.android.maps.MapActivity;
 import org.mapsforge.android.maps.MapScaleBar;
 import org.mapsforge.android.maps.MapScaleBar.TextField;
 import org.mapsforge.android.maps.MapView;
 import org.mapsforge.android.maps.MapViewPosition;
 import org.mapsforge.android.maps.Projection;
 import org.mapsforge.android.maps.mapgenerator.MapGenerator;
 import org.mapsforge.android.maps.mapgenerator.MapGeneratorFactory;
 import org.mapsforge.android.maps.mapgenerator.MapGeneratorInternal;
 import org.mapsforge.android.maps.overlay.OverlayItem;
 import org.mapsforge.android.maps.overlay.OverlayWay;
 import org.mapsforge.core.GeoPoint;
 
 import android.app.Activity;
 import android.app.AlertDialog;
 import android.app.AlertDialog.Builder;
 import android.app.ProgressDialog;
 import android.content.DialogInterface;
 import android.content.Intent;
 import android.content.SharedPreferences;
 import android.graphics.Rect;
 import android.graphics.drawable.Drawable;
 import android.os.AsyncTask;
 import android.os.Bundle;
 import android.os.Environment;
 import android.os.Handler;
 import android.os.Message;
 import android.preference.PreferenceManager;
 import android.text.Editable;
 import android.view.KeyEvent;
 import android.view.Menu;
 import android.view.MenuItem;
 import android.view.MotionEvent;
 import android.view.SubMenu;
 import android.view.View;
 import android.view.View.OnTouchListener;
 import android.view.ViewGroup;
 import android.widget.ArrayAdapter;
 import android.widget.Button;
 import android.widget.EditText;
 import android.widget.GridView;
 import android.widget.ImageButton;
 import android.widget.ImageView;
 import android.widget.RelativeLayout;
 import android.widget.RelativeLayout.LayoutParams;
 import android.widget.SlidingDrawer;
 import android.widget.Toast;
 import eu.geopaparazzi.library.gps.GpsLocation;
 import eu.geopaparazzi.library.gps.GpsManager;
 import eu.geopaparazzi.library.gps.GpsManagerListener;
 import eu.geopaparazzi.library.mixare.MixareHandler;
 import eu.geopaparazzi.library.network.NetworkUtilities;
 import eu.geopaparazzi.library.util.LibraryConstants;
 import eu.geopaparazzi.library.util.PositionUtilities;
 import eu.geopaparazzi.library.util.activities.InsertCoordActivity;
 import eu.geopaparazzi.library.util.debug.Debug;
 import eu.geopaparazzi.library.util.debug.Logger;
 import eu.hydrologis.geopaparazzi.R;
 import eu.hydrologis.geopaparazzi.database.DaoBookmarks;
 import eu.hydrologis.geopaparazzi.database.DaoGpsLog;
 import eu.hydrologis.geopaparazzi.database.DaoImages;
 import eu.hydrologis.geopaparazzi.database.DaoMaps;
 import eu.hydrologis.geopaparazzi.database.DaoNotes;
 import eu.hydrologis.geopaparazzi.database.NoteType;
 import eu.hydrologis.geopaparazzi.maps.overlays.ArrayGenericOverlay;
 import eu.hydrologis.geopaparazzi.osm.OsmCategoryActivity;
 import eu.hydrologis.geopaparazzi.osm.OsmTagsManager;
 import eu.hydrologis.geopaparazzi.osm.OsmUtilities;
 import eu.hydrologis.geopaparazzi.util.Bookmark;
 import eu.hydrologis.geopaparazzi.util.Constants;
 import eu.hydrologis.geopaparazzi.util.MixareUtilities;
 import eu.hydrologis.geopaparazzi.util.Note;
 import eu.hydrologis.geopaparazzi.util.VerticalSeekBar;
 
 /**
  * @author Andrea Antonello (www.hydrologis.com)
  */
 public class MapsActivity extends MapActivity implements GpsManagerListener, OnTouchListener {
     private static final int INSERTCOORD_RETURN_CODE = 666;
 
     private static final int MENU_GPSDATA = 1;
     private static final int MENU_MAPDATA = 2;
     private static final int MENU_TILE_SOURCE_ID = 3;
     private static final int MENU_MINIMAP_ID = 4;
     private static final int MENU_SCALE_ID = 5;
     private static final int MENU_COMPASS_ID = 6;
     private static final int MENU_MIXARE_ID = 7;
     private static final int GO_TO = 8;
 
     private HashMap<Integer, String> tileSourcesMap = null;
 
     private DecimalFormat formatter = new DecimalFormat("00"); //$NON-NLS-1$
     private Button zoomInButton;
     private Button zoomOutButton;
     private VerticalSeekBar zoomBar;
     private SlidingDrawer slidingDrawer;
     private boolean sliderIsOpen;
     private boolean osmSliderIsOpen;
     private MapView mapView;
     // private MinimapOverlayWithCross mMiniMapOverlay;
     // private LogsOverlay mLogsOverlay;
     // private NotesOverlay mNotesOverlay;
     // private BookmarksOverlay mBookmarksOverlay;
     // private GpsPositionOverlay mGpsOverlay;
     // private CrossOverlay mCrossOverlay;
     // private MapsOverlay mMapsOverlay;
     // private MeasureToolOverlay mMeasureOverlay;
     // private ImagesOverlay mImagesOverlay;
     private int maxZoomLevel;
     private int minZoomLevel;
     private SlidingDrawer osmSlidingDrawer;
     private SharedPreferences preferences;
     private boolean doOsm;
 
     public void onCreate( Bundle icicle ) {
         super.onCreate(icicle);
         setContentView(R.layout.mapsview);
 
         tileSourcesMap = new LinkedHashMap<Integer, String>();
         tileSourcesMap.put(1001, MapGeneratorInternal.DATABASE_RENDERER.name());
         tileSourcesMap.put(1002, MapGeneratorInternal.MAPNIK.name());
         tileSourcesMap.put(1003, MapGeneratorInternal.OPENCYCLEMAP.name());
 
         preferences = PreferenceManager.getDefaultSharedPreferences(this);
 
         mapView = new MapView(this);
         mapView.setClickable(true);
         mapView.setBuiltInZoomControls(false);
         File mapFile = new File(Environment.getExternalStorageDirectory().getPath(), "download/italy.map");
         if (!mapFile.exists()) {
             mapFile = new File(Environment.getExternalStorageDirectory().getPath(), "download/berlin.map");
             if (!mapFile.exists()) {
                 mapFile = new File(Environment.getExternalStorageDirectory().getPath(), "berlin.map");
                 if (!mapFile.exists()) {
                     throw new RuntimeException();
                 }
             }
         }
         mapView.setMapFile(mapFile);
         mapView.setOnTouchListener(this);
 
         MapScaleBar mapScaleBar = this.mapView.getMapScaleBar();
         mapScaleBar.setImperialUnits(false);
         mapScaleBar.setText(TextField.KILOMETER, " km");
         mapScaleBar.setText(TextField.METER, " m");
 
         // boolean drawTileFrames = preferences.getBoolean("drawTileFrames", false);
         // boolean drawTileCoordinates = preferences.getBoolean("drawTileCoordinates", false);
         // boolean highlightWaterTiles = preferences.getBoolean("highlightWaterTiles", false);
         // DebugSettings debugSettings = new DebugSettings(true, true, highlightWaterTiles);
         // this.mapView.setDebugSettings(debugSettings);
 
         final RelativeLayout rl = (RelativeLayout) findViewById(R.id.innerlayout);
         rl.addView(mapView, new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
 
         ViewportManager.INSTANCE.setMapActivity(this);
 
         GpsManager.getInstance(this).addListener(this);
 
         /* imported maps */
         // {
         // mMapsOverlay = new MapsOverlay(this, mResourceProxy);
         // this.mapsView.getOverlays().add(mMapsOverlay);
         // }
 
         dataOverlay = new ArrayGenericOverlay(this);
         mapView.getOverlays().add(dataOverlay);
         readData();
 
         // /* measure tool */
         // {
         // mMeasureOverlay = new MeasureToolOverlay(this, mResourceProxy);
         // this.mapsView.getOverlays().add(mMeasureOverlay);
         // }
 
         final double[] mapCenterLocation = PositionUtilities.getMapCenterFromPreferences(preferences, true, true);
         GeoPoint geoPoint = new GeoPoint(mapCenterLocation[1], mapCenterLocation[0]);
         mapView.getController().setZoom((int) mapCenterLocation[2]);
         mapView.getController().setCenter(geoPoint);
 
         maxZoomLevel = mapView.getMapZoomControls().getZoomLevelMax();
         minZoomLevel = mapView.getMapZoomControls().getZoomLevelMin();
 
         // zoom bar
         zoomBar = (VerticalSeekBar) findViewById(R.id.ZoomBar);
         zoomBar.setMax(maxZoomLevel);
         zoomBar.setProgress((int) mapCenterLocation[2]);
         zoomBar.setOnSeekBarChangeListener(new VerticalSeekBar.OnSeekBarChangeListener(){
             private int progress = (int) mapCenterLocation[2];
             public void onStopTrackingTouch( VerticalSeekBar seekBar ) {
                 setZoomGuiText(progress);
                 mapView.getController().setZoom(progress);
                 inalidateMap();
                 //                Logger.d(this, "Zoomed to: " + progress); //$NON-NLS-1$
             }
 
             public void onStartTrackingTouch( VerticalSeekBar seekBar ) {
 
             }
 
             public void onProgressChanged( VerticalSeekBar seekBar, int progress, boolean fromUser ) {
                 this.progress = progress;
                 setZoomGuiText(progress);
             }
         });
 
         int zoomInLevel = (int) mapCenterLocation[2] + 1;
         if (zoomInLevel > maxZoomLevel) {
             zoomInLevel = maxZoomLevel;
         }
         int zoomOutLevel = (int) mapCenterLocation[2] - 1;
         if (zoomOutLevel < minZoomLevel) {
             zoomOutLevel = minZoomLevel;
         }
         zoomInButton = (Button) findViewById(R.id.zoomin);
         zoomInButton.setText(formatter.format(zoomInLevel));
         zoomInButton.setOnClickListener(new Button.OnClickListener(){
             public void onClick( View v ) {
                 String text = zoomInButton.getText().toString();
                 int newZoom = Integer.parseInt(text);
                 setZoomGuiText(newZoom);
                 mapView.getController().setZoom(newZoom);
                 inalidateMap();
             }
         });
         zoomOutButton = (Button) findViewById(R.id.zoomout);
         zoomOutButton.setText(formatter.format(zoomOutLevel));
         zoomOutButton.setOnClickListener(new Button.OnClickListener(){
             public void onClick( View v ) {
                 String text = zoomOutButton.getText().toString();
                 int newZoom = Integer.parseInt(text);
                 setZoomGuiText(newZoom);
                 mapView.getController().setZoom(newZoom);
                 inalidateMap();
             }
         });
 
         // center on gps button
         ImageButton centerOnGps = (ImageButton) findViewById(R.id.center_on_gps_btn);
         centerOnGps.setOnClickListener(new Button.OnClickListener(){
             public void onClick( View v ) {
                 GpsLocation location = GpsManager.getInstance(MapsActivity.this).getLocation();
                 if (location != null) {
                     setNewCenter(location.getLongitude(), location.getLatitude(), false);
                 }
             }
         });
 
         // slidingdrawer
         final int slidingId = R.id.mapslide;
         slidingDrawer = (SlidingDrawer) findViewById(slidingId);
         final ImageView slideHandleButton = (ImageView) findViewById(R.id.mapslidehandle);
 
         slidingDrawer.setOnDrawerOpenListener(new SlidingDrawer.OnDrawerOpenListener(){
             public void onDrawerOpened() {
                 // Logger.d(this, "Enable drawing");
                 sliderIsOpen = true;
                 slideHandleButton.setBackgroundResource(R.drawable.min);
                 enableDrawingWithDelay();
             }
         });
         slidingDrawer.setOnDrawerCloseListener(new SlidingDrawer.OnDrawerCloseListener(){
             public void onDrawerClosed() {
                 // Logger.d(this, "Enable drawing");
                 slideHandleButton.setBackgroundResource(R.drawable.max);
                 sliderIsOpen = false;
                 enableDrawingWithDelay();
             }
 
         });
 
         slidingDrawer.setOnDrawerScrollListener(new SlidingDrawer.OnDrawerScrollListener(){
             public void onScrollEnded() {
                 // Logger.d(this, "Scroll End Disable drawing");
                 disableDrawing();
             }
 
             public void onScrollStarted() {
                 // Logger.d(this, "Scroll Start Disable drawing");
                 disableDrawing();
             }
         });
 
         /*
         * tool buttons
         */
         ImageButton addnotebytagButton = (ImageButton) findViewById(R.id.addnotebytagbutton);
         addnotebytagButton.setOnClickListener(new Button.OnClickListener(){
             public void onClick( View v ) {
                 MapViewPosition mapPosition = mapView.getMapPosition();
                 GeoPoint mapCenter = mapPosition.getMapCenter();
                 Intent osmTagsIntent = new Intent(MapsActivity.this, MapTagsActivity.class);
                 osmTagsIntent.putExtra(LibraryConstants.LATITUDE, (double) (mapCenter.latitudeE6 / LibraryConstants.E6));
                 osmTagsIntent.putExtra(LibraryConstants.LONGITUDE, (double) (mapCenter.longitudeE6 / LibraryConstants.E6));
                 osmTagsIntent.putExtra(LibraryConstants.ELEVATION, 0.0);
                 startActivity(osmTagsIntent);
             }
         });
 
         ImageButton addBookmarkButton = (ImageButton) findViewById(R.id.addbookmarkbutton);
         addBookmarkButton.setOnClickListener(new Button.OnClickListener(){
             public void onClick( View v ) {
                 addBookmark();
             }
         });
 
         ImageButton removeNotesButton = (ImageButton) findViewById(R.id.removenotesbutton);
         removeNotesButton.setOnClickListener(new Button.OnClickListener(){
             public void onClick( View v ) {
                 deleteVisibleNotes();
             }
         });
 
         ImageButton removeBookmarksButton = (ImageButton) findViewById(R.id.removebookmarkbutton);
         removeBookmarksButton.setOnClickListener(new Button.OnClickListener(){
             public void onClick( View v ) {
                 deleteVisibleBookmarks();
             }
         });
 
         ImageButton listBookmarksButton = (ImageButton) findViewById(R.id.bookmarkslistbutton);
         listBookmarksButton.setOnClickListener(new Button.OnClickListener(){
             public void onClick( View v ) {
                 Intent intent = new Intent(MapsActivity.this, BookmarksListActivity.class);
                 startActivity(intent);
             }
         });
 
         final ImageButton toggleMeasuremodeButton = (ImageButton) findViewById(R.id.togglemeasuremodebutton);
         toggleMeasuremodeButton.setOnClickListener(new Button.OnClickListener(){
             public void onClick( View v ) {
                 // boolean isInMeasureMode = mMeasureOverlay.isInMeasureMode();
                 // mMeasureOverlay.setMeasureMode(!isInMeasureMode);
                 // if (!isInMeasureMode) {
                 // toggleMeasuremodeButton.setBackgroundResource(R.drawable.measuremode_on);
                 // } else {
                 // toggleMeasuremodeButton.setBackgroundResource(R.drawable.measuremode);
                 // }
                 // if (!isInMeasureMode) {
                 // disableDrawing();
                 // } else {
                 // enableDrawingWithDelay();
                 // }
             }
         });
 
         try {
             handleOsmSliderView();
         } catch (Exception e) {
             e.printStackTrace();
         }
 
     }
 
     private void readData() {
         try {
             dataOverlay.clearItems();
             dataOverlay.clearWays();
 
             List<OverlayWay> logOverlaysList = DaoGpsLog.getGpslogOverlays(this);
             dataOverlay.addWays(logOverlaysList);
 
             /* images */
             if (DataManager.getInstance().areImagesVisible()) {
                 Drawable imageMarker = getResources().getDrawable(R.drawable.image);
                 Drawable newImageMarker = ArrayGenericOverlay.boundCenter(imageMarker);
                 List<OverlayItem> imagesOverlaysList = DaoImages.getImagesOverlayList(this, newImageMarker);
                 dataOverlay.addItems(imagesOverlaysList);
             }
 
             /* gps notes */
             if (DataManager.getInstance().areNotesVisible()) {
                 Drawable notesMarker = getResources().getDrawable(R.drawable.goto_position);
                 Drawable newNotesMarker = ArrayGenericOverlay.boundCenter(notesMarker);
                 List<OverlayItem> noteOverlaysList = DaoNotes.getNoteOverlaysList(this, newNotesMarker);
                 dataOverlay.addItems(noteOverlaysList);
             }
 
             /* bookmarks */
             Drawable bookmarkMarker = getResources().getDrawable(R.drawable.bookmark);
             Drawable newBookmarkMarker = ArrayGenericOverlay.boundCenter(bookmarkMarker);
             List<OverlayItem> bookmarksOverlays = DaoBookmarks.getBookmarksOverlays(this, newBookmarkMarker);
             dataOverlay.addItems(bookmarksOverlays);
         } catch (IOException e1) {
             e1.printStackTrace();
         }
     }
 
     public boolean onTouch( View v, MotionEvent event ) {
         if (event.getAction() == MotionEvent.ACTION_UP) {
             saveCenterPref();
         }
         return false;
     }
 
     private void handleOsmSliderView() throws Exception {
         OsmTagsManager osmTagsManager = OsmTagsManager.getInstance();
         String[] categoriesNamesArray = osmTagsManager.getTagCategories(this);
 
         int visibility = 0;
         if (categoriesNamesArray == null) {
             categoriesNamesArray = new String[]{""}; //$NON-NLS-1$
             visibility = 4; // invisible
         }
         doOsm = visibility != 4;
         boolean doOsmPref = preferences.getBoolean(Constants.PREFS_KEY_DOOSM, false);
         doOsm = doOsm && doOsmPref;
         if (!doOsm) {
             visibility = 4; // invisible
         }
 
         final String[] categoriesNamesArrayFinal = categoriesNamesArray;
 
         // slidingdrawer
         final int slidingId = R.id.osmslide;
         osmSlidingDrawer = (SlidingDrawer) findViewById(slidingId);
         osmSlidingDrawer.setVisibility(visibility);
 
         if (doOsm) {
             osmSlidingDrawer.setOnDrawerOpenListener(new SlidingDrawer.OnDrawerOpenListener(){
 
                 public void onDrawerOpened() {
                     osmSliderIsOpen = true;
                     enableDrawingWithDelay();
                 }
             });
             osmSlidingDrawer.setOnDrawerCloseListener(new SlidingDrawer.OnDrawerCloseListener(){
                 public void onDrawerClosed() {
                     osmSliderIsOpen = false;
                     enableDrawingWithDelay();
                 }
 
             });
 
             osmSlidingDrawer.setOnDrawerScrollListener(new SlidingDrawer.OnDrawerScrollListener(){
                 public void onScrollEnded() {
                     disableDrawing();
                 }
                 public void onScrollStarted() {
                     disableDrawing();
                 }
             });
 
             GridView buttonGridView = (GridView) findViewById(R.id.osmcategoriesview);
 
             ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, R.layout.gpslog_row, categoriesNamesArrayFinal){
                 public View getView( final int position, View cView, ViewGroup parent ) {
 
                     final Button osmButton = new Button(MapsActivity.this);
                     osmButton.setText(categoriesNamesArrayFinal[position]);
                     osmButton.setBackgroundResource(R.drawable.osmcategory_button_drawable);
                     osmButton.setOnClickListener(new Button.OnClickListener(){
                         public void onClick( View v ) {
                             String categoryName = osmButton.getText().toString();
                             Intent osmCategoryIntent = new Intent(MapsActivity.this, OsmCategoryActivity.class);
                             osmCategoryIntent.putExtra(Constants.OSM_CATEGORY_KEY, categoryName);
                             startActivity(osmCategoryIntent);
 
                             // osmSlidingDrawer.close();
                         }
                     });
                     return osmButton;
                 }
             };
             buttonGridView.setAdapter(arrayAdapter);
 
             Button syncOsmButton = (Button) findViewById(R.id.syncosmbutton);
             syncOsmButton.setOnClickListener(new Button.OnClickListener(){
                 public void onClick( View v ) {
 
                     if (!NetworkUtilities.isNetworkAvailable(getApplicationContext())) {
                         AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
                         builder.setMessage(R.string.available_only_with_network).setCancelable(false)
                                 .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener(){
                                     public void onClick( DialogInterface dialog, int id ) {
                                     }
                                 });
                         AlertDialog alertDialog = builder.create();
                         alertDialog.show();
                         return;
                     }
 
                     final EditText input = new EditText(MapsActivity.this);
                     input.setText(""); //$NON-NLS-1$
                     Builder builder = new AlertDialog.Builder(MapsActivity.this);
                     builder.setTitle(R.string.set_description);
                     builder.setMessage(R.string.osm_insert_a_changeset_description);
                     builder.setView(input);
                     builder.setIcon(android.R.drawable.ic_dialog_alert)
                             .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener(){
                                 public void onClick( DialogInterface dialog, int whichButton ) {
                                     sync(""); //$NON-NLS-1$
                                 }
                             }).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener(){
                                 public void onClick( DialogInterface dialog, int whichButton ) {
                                     Editable value = input.getText();
                                     String newName = value.toString();
                                     sync(newName);
                                 }
                             }).setCancelable(false).show();
 
                 }
 
                 private void sync( final String description ) {
                     final ProgressDialog progressDialog = ProgressDialog.show(MapsActivity.this,
                             "", getString(R.string.loading_data)); //$NON-NLS-1$
                     new AsyncTask<String, Void, String>(){
                         private Exception e = null;
 
                         protected String doInBackground( String... params ) {
                             String response = null;
                             try {
                                 response = OsmUtilities.sendOsmNotes(MapsActivity.this, description);
                             } catch (Exception e) {
                                 e.printStackTrace();
                                 this.e = e;
                             }
                             return response;
                         }
 
                         protected void onPostExecute( String response ) {
                             progressDialog.dismiss();
                             if (e == null) {
                                 String msg = getResources().getString(R.string.osm_notes_properly_uploaded);
                                 if (response.toLowerCase().trim().startsWith(OsmUtilities.FEATURES_IMPORTED)) {
                                     String leftOver = response.replaceFirst(OsmUtilities.FEATURES_IMPORTED, ""); //$NON-NLS-1$
                                     if (leftOver.trim().length() > 0) {
                                         String text = leftOver.substring(1);
                                         text = text.replaceFirst("\\_", "/"); //$NON-NLS-1$//$NON-NLS-2$
 
                                         msg = MessageFormat.format(
                                                 "Some of the features were uploaded, but not all of them ({0}).", text); //$NON-NLS-1$
                                         openAlertDialog(msg);
                                     } else {
                                         AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
                                         builder.setMessage(msg).setCancelable(false)
                                                 .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener(){
                                                     public void onClick( DialogInterface dialog, int id ) {
                                                         try {
                                                             DaoNotes.deleteNotesByType(MapsActivity.this, NoteType.OSM);
                                                         } catch (IOException e) {
                                                             e.printStackTrace();
                                                         }
                                                     }
                                                 }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener(){
                                                     public void onClick( DialogInterface dialog, int id ) {
                                                     }
                                                 });
                                         AlertDialog alertDialog = builder.create();
                                         alertDialog.show();
                                     }
                                 } else if (response.toLowerCase().trim().contains(OsmUtilities.ERROR_JSON)) {
                                     msg = getString(R.string.error_json_osm);
                                     openAlertDialog(msg);
                                 } else if (response.toLowerCase().trim().contains(OsmUtilities.ERROR_OSM)) {
                                     msg = getString(R.string.error_osm_server);
                                     openAlertDialog(msg);
                                 }
 
                                 // TODO check if we want the slider to close
                                 // osmSlidingDrawer.close();
 
                             } else {
                                 String msg = getResources().getString(R.string.an_error_occurred_while_uploading_osm_tags);
                                 openAlertDialog(msg + e.getLocalizedMessage());
                             }
                         }
                     }.execute((String) null);
                 }
             });
         }
 
     }
 
     /**
      * Open an alert dialog with a message and an ok button.
      * 
      * @param msg the message to show.
      */
     private void openAlertDialog( String msg ) {
         AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
         builder.setMessage(msg).setCancelable(false).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener(){
             public void onClick( DialogInterface dialog, int id ) {
             }
         });
         AlertDialog alertDialog = builder.create();
         alertDialog.show();
     }
 
     @Override
     public void onWindowFocusChanged( boolean hasFocus ) {
         // SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
         if (hasFocus) {
             double[] lastCenter = PositionUtilities.getMapCenterFromPreferences(preferences, true, true);
 
             GeoPoint geoPoint = new GeoPoint(lastCenter[1], lastCenter[0]);
             mapView.getController().setZoom((int) lastCenter[2]);
             mapView.getController().setCenter(geoPoint);
 
             setZoomGuiText((int) lastCenter[2]);
 
             readData();
         }
         super.onWindowFocusChanged(hasFocus);
     }
 
     // @Override
     // protected void onPause() {
     // GeoPoint mapCenter = mapsView.getMapCenter();
     // SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
     // Editor editor = preferences.edit();
     // editor.putFloat(GPSLAST_LONGITUDE, mapCenter.getLongitudeE6() / E6);
     // editor.putFloat(GPSLAST_LATITUDE, mapCenter.getLatitudeE6() / E6);
     // editor.commit();
     // super.onPause();
     // }
 
     public MapView getMapView() {
         return mapView;
     }
 
     private void setZoomGuiText( int newZoom ) {
         int zoomInLevel = newZoom + 1;
         if (zoomInLevel > maxZoomLevel) {
             zoomInLevel = maxZoomLevel;
         }
         int zoomOutLevel = newZoom - 1;
         if (zoomOutLevel < minZoomLevel) {
             zoomOutLevel = minZoomLevel;
         }
         zoomInButton.setText(formatter.format(zoomInLevel));
         zoomOutButton.setText(formatter.format(zoomOutLevel));
         zoomBar.setProgress(newZoom);
     }
 
     public void setNewCenter( double lon, double lat, boolean drawIcon ) {
         mapView.getController().setCenter(new GeoPoint(lat, lon));
     }
 
     public void setNewCenterAtZoom( final double centerX, final double centerY, final int zoom ) {
         mapView.getController().setZoom(zoom);
         setZoomGuiText(zoom);
         mapView.getController().setCenter(
                 new GeoPoint((int) (centerX * LibraryConstants.E6), (int) (centerY * LibraryConstants.E6)));
     }
 
     public double[] getCenterLonLat() {
         GeoPoint mapCenter = mapView.getMapPosition().getMapCenter();
         double[] lonLat = {mapCenter.longitudeE6 / LibraryConstants.E6, mapCenter.latitudeE6 / LibraryConstants.E6};
         return lonLat;
     }
 
     public boolean onCreateOptionsMenu( Menu menu ) {
         super.onCreateOptionsMenu(menu);
         menu.add(Menu.NONE, MENU_GPSDATA, 1, R.string.mainmenu_gpsdataselect).setIcon(android.R.drawable.ic_menu_compass);
         menu.add(Menu.NONE, MENU_MAPDATA, 2, R.string.mainmenu_mapdataselect).setIcon(android.R.drawable.ic_menu_compass);
         menu.add(Menu.NONE, MENU_MINIMAP_ID, 3, R.string.mapsactivity_menu_toggle_minimap).setIcon(R.drawable.ic_menu_minimap);
         menu.add(Menu.NONE, MENU_SCALE_ID, 4, R.string.mapsactivity_menu_toggle_scalebar).setIcon(R.drawable.ic_menu_scalebar);
         menu.add(Menu.NONE, MENU_COMPASS_ID, 5, R.string.mapsactivity_menu_toggle_compass).setIcon(R.drawable.ic_menu_compass);
         menu.add(Menu.NONE, MENU_MIXARE_ID, 6, R.string.view_in_mixare).setIcon(R.drawable.icon_datasource);
         final SubMenu subMenu = menu.addSubMenu(Menu.NONE, MENU_TILE_SOURCE_ID, 7, R.string.mapsactivity_menu_tilesource)
                 .setIcon(R.drawable.ic_menu_tilesource);
         {
             Set<Entry<Integer, String>> entrySet = tileSourcesMap.entrySet();
             for( Entry<Integer, String> entry : entrySet ) {
                 subMenu.add(0, entry.getKey(), Menu.NONE, entry.getValue());
             }
         }
         menu.add(Menu.NONE, GO_TO, 8, R.string.goto_coordinate).setIcon(android.R.drawable.ic_menu_myplaces);
         return true;
     }
 
     public boolean onMenuItemSelected( int featureId, MenuItem item ) {
         switch( item.getItemId() ) {
         case MENU_GPSDATA:
             Intent gpsDatalistIntent = new Intent(this, GpsDataListActivity.class);
             startActivity(gpsDatalistIntent);
             return true;
 
         case MENU_MAPDATA:
             try {
                 List<MapItem> mapsList = DaoMaps.getMaps(this);
                 int mapsNum = mapsList.size();
                 if (mapsNum < 1) {
                     AlertDialog.Builder builder = new AlertDialog.Builder(this);
                     builder.setMessage(R.string.no_maps_in_list).setCancelable(false)
                             .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener(){
                                 public void onClick( DialogInterface dialog, int id ) {
                                 }
                             });
                     AlertDialog alertDialog = builder.create();
                     alertDialog.show();
                     return true;
                 } else {
                     Intent mapDatalistIntent = new Intent(this, MapDataListActivity.class);
                     startActivity(mapDatalistIntent);
                     return true;
                 }
             } catch (IOException e1) {
                 Logger.e(this, e1.getLocalizedMessage(), e1);
                 e1.printStackTrace();
                 return false;
             }
         case MENU_TILE_SOURCE_ID:
             return true;
 
         case MENU_SCALE_ID:
             MapScaleBar mapScaleBar = mapView.getMapScaleBar();
             boolean showMapScaleBar = mapScaleBar.isShowMapScaleBar();
             mapScaleBar.setShowMapScaleBar(!showMapScaleBar);
             return true;
             // case MENU_COMPASS_ID:
             // boolean isCompassEnabled = !mCompassOverlay.isEnabled();
             // mCompassOverlay.setEnabled(isCompassEnabled);
             // mapsView.invalidate();
             // Editor editor3 = preferences.edit();
             // editor3.putBoolean(PREFS_KEY_COMPASSON, isCompassEnabled);
             // return true;
         case MENU_MIXARE_ID:
             MixareHandler mixareHandler = new MixareHandler();
             if (!mixareHandler.isMixareInstalled(this)) {
                 mixareHandler.installMixareFromMarket(this);
                 return true;
             }
             float[] nswe = getMapWorldBounds();
 
             try {
                 MixareUtilities.runRegionOnMixare(this, nswe[0], nswe[1], nswe[2], nswe[3]);
                 return true;
             } catch (Exception e1) {
                 e1.printStackTrace();
                 return false;
             }
         case GO_TO: {
             Intent intent = new Intent(this, InsertCoordActivity.class);
             startActivityForResult(intent, INSERTCOORD_RETURN_CODE);
             return true;
         }
         default:
 
             String name = item.getTitle().toString();
             MapGeneratorInternal mapGeneratorInternalNew;
             try {
                 mapGeneratorInternalNew = MapGeneratorInternal.valueOf(name);
             } catch (IllegalArgumentException e) {
                 mapGeneratorInternalNew = MapGeneratorInternal.DATABASE_RENDERER;
             }
             // if (mapGeneratorInternalNew != this.mapGeneratorInternal) {
             MapGenerator mapGenerator = MapGeneratorFactory.createMapGenerator(mapGeneratorInternalNew);
             this.mapView.setMapGenerator(mapGenerator);
             // this.mapGeneratorInternal = mapGeneratorInternalNew;
             // }
 
         }
         return super.onMenuItemSelected(featureId, item);
     }
 
     /**
      * Retrieves the map world bounds in degrees. 
      * 
      * @return the [n,s,w,e] in degrees.
      */
     private float[] getMapWorldBounds() {
         float[] nswe = getMapWorldBoundsE6();
         float n = nswe[0] / LibraryConstants.E6;
         float s = nswe[1] / LibraryConstants.E6;
         float w = nswe[2] / LibraryConstants.E6;
         float e = nswe[3] / LibraryConstants.E6;
         return new float[]{n, s, w, e};
     }
 
     /**
      * Retrieves the map world bounds in microdegrees. 
      * 
      * @return the [n,s,w,e] in midrodegrees.
      */
     private float[] getMapWorldBoundsE6() {
         Projection projection = mapView.getProjection();
         int latitudeSpan = projection.getLatitudeSpan();
         int longitudeSpan = projection.getLongitudeSpan();
         MapViewPosition mapPosition = mapView.getMapPosition();
         GeoPoint c = mapPosition.getMapCenter();
         float n = (c.latitudeE6 + latitudeSpan / 2);
         float s = (c.latitudeE6 - latitudeSpan / 2);
         float w = (c.longitudeE6 - longitudeSpan / 2);
         float e = (c.longitudeE6 + longitudeSpan / 2);
         float[] nswe = {n, s, w, e};
         return nswe;
     }
 
     protected void onActivityResult( int requestCode, int resultCode, Intent data ) {
         if (Debug.D) {
             Logger.d(this, "Activity returned"); //$NON-NLS-1$
         }
         super.onActivityResult(requestCode, resultCode, data);
         switch( requestCode ) {
         case (INSERTCOORD_RETURN_CODE): {
             if (resultCode == Activity.RESULT_OK) {
                 double lon = data.getDoubleExtra(LibraryConstants.LONGITUDE, 0f);
                 double lat = data.getDoubleExtra(LibraryConstants.LATITUDE, 0f);
                 ViewportManager.INSTANCE.setCenterAndZoomForMapWindowFocus(lon, lat, null);
             }
             break;
         }
         }
     }
 
     private void deleteVisibleBookmarks() {
 
         new AlertDialog.Builder(this).setTitle(R.string.delete_visible_bookmarks_title)
                 .setMessage(R.string.delete_visible_bookmarks_prompt).setIcon(android.R.drawable.ic_dialog_alert)
                 .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener(){
                     public void onClick( DialogInterface dialog, int whichButton ) {
                     }
                 }).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener(){
                     public void onClick( DialogInterface dialog, int whichButton ) {
                         try {
                             float[] nswe = getMapWorldBounds();
                             final List<Bookmark> bookmarksInBounds = DaoBookmarks.getBookmarksInWorldBounds(MapsActivity.this,
                                     nswe[0], nswe[1], nswe[2], nswe[3]);
                             int bookmarksNum = bookmarksInBounds.size();
 
                             bookmarksRemoveDialog = new ProgressDialog(MapsActivity.this);
                             bookmarksRemoveDialog.setCancelable(true);
                             bookmarksRemoveDialog.setMessage(MessageFormat.format(
                                     getString(R.string.mapsactivity_delete_bookmarks), bookmarksNum));
                             bookmarksRemoveDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                             bookmarksRemoveDialog.setProgress(0);
                             bookmarksRemoveDialog.setMax(bookmarksNum);
                             bookmarksRemoveDialog.show();
 
                             new Thread(){
                                 public void run() {
                                     for( final Bookmark bookmark : bookmarksInBounds ) {
                                         bookmarksRemoveHandler.sendEmptyMessage(0);
                                         try {
                                             DaoBookmarks.deleteBookmark(MapsActivity.this, bookmark.getId());
                                         } catch (IOException e) {
                                             e.printStackTrace();
                                         }
                                     }
                                     bookmarksDeleted = true;
                                     bookmarksRemoveHandler.sendEmptyMessage(0);
                                 }
                             }.start();
                         } catch (IOException e) {
                             e.printStackTrace();
                         }
                     }
                 }).show();
 
     }
 
     private void deleteVisibleNotes() {
 
         new AlertDialog.Builder(this).setTitle(R.string.delete_visible_notes_title)
                 .setMessage(R.string.delete_visible_notes_prompt).setIcon(android.R.drawable.ic_dialog_alert)
                 .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener(){
                     public void onClick( DialogInterface dialog, int whichButton ) {
                     }
                 }).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener(){
                     public void onClick( DialogInterface dialog, int whichButton ) {
                         try {
                             float[] nswe = getMapWorldBounds();
 
                             final List<Note> notesInBounds = DaoNotes.getNotesInWorldBounds(MapsActivity.this, nswe[0], nswe[1],
                                     nswe[2], nswe[3]);
 
                             int notesNum = notesInBounds.size();
 
                             notesRemoveDialog = new ProgressDialog(MapsActivity.this);
                             notesRemoveDialog.setCancelable(true);
                             notesRemoveDialog.setMessage(MessageFormat.format(getString(R.string.mapsactivity_delete_notes),
                                     notesNum));
                             notesRemoveDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                             notesRemoveDialog.setProgress(0);
                             notesRemoveDialog.setMax(notesNum);
                             notesRemoveDialog.show();
 
                             new Thread(){
                                 public void run() {
                                     for( Note note : notesInBounds ) {
                                         notesRemoveHandler.sendEmptyMessage(0);
                                         try {
                                             DaoNotes.deleteNote(MapsActivity.this, note.getId());
                                         } catch (IOException e) {
                                             e.printStackTrace();
                                         }
                                     }
                                     notesDeleted = true;
                                     notesRemoveHandler.sendEmptyMessage(0);
                                 }
                             }.start();
                         } catch (IOException e) {
                             e.printStackTrace();
                         }
                     }
                 }).show();
 
     }
 
     private void addBookmark() {
         GeoPoint mapCenter = mapView.getMapPosition().getMapCenter();
         final float centerLat = mapCenter.latitudeE6 / LibraryConstants.E6;
         final float centerLon = mapCenter.longitudeE6 / LibraryConstants.E6;
         final EditText input = new EditText(this);
         final String newDate = LibraryConstants.TIME_FORMATTER.format(new Date());
         final String proposedName = "bookmark " + newDate; //$NON-NLS-1$
         input.setText(proposedName);
         Builder builder = new AlertDialog.Builder(this).setTitle(R.string.mapsactivity_new_bookmark);
         builder.setMessage(R.string.mapsactivity_enter_bookmark_name);
         builder.setView(input);
         builder.setIcon(android.R.drawable.ic_dialog_info)
                 .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener(){
                     public void onClick( DialogInterface dialog, int whichButton ) {
                     }
                 }).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener(){
                     public void onClick( DialogInterface dialog, int whichButton ) {
                         try {
                             Editable value = input.getText();
                             String newName = value.toString();
                             if (newName == null || newName.length() < 1) {
                                 newName = proposedName;;
                             }
 
                             int zoom = mapView.getMapPosition().getZoomLevel();
                             float[] nswe = getMapWorldBounds();
                             DaoBookmarks.addBookmark(getApplicationContext(), centerLon, centerLat, newName, zoom, nswe[0],
                                     nswe[1], nswe[2], nswe[3]);
                             mapView.invalidateOnUiThread();
                         } catch (IOException e) {
                             Logger.e(this, e.getLocalizedMessage(), e);
                             e.printStackTrace();
                             Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                         }
                     }
                 }).setCancelable(false).show();
     }
     private boolean bookmarksDeleted = false;
     private ProgressDialog bookmarksRemoveDialog;
     private Handler bookmarksRemoveHandler = new Handler(){
         public void handleMessage( Message msg ) {
             if (!bookmarksDeleted) {
                 bookmarksRemoveDialog.incrementProgressBy(1);
             } else {
                 bookmarksRemoveDialog.dismiss();
                 mapView.invalidateOnUiThread();
             }
         }
     };
 
     private boolean notesDeleted = false;
     private ProgressDialog notesRemoveDialog;
     private Handler notesRemoveHandler = new Handler(){
         public void handleMessage( Message msg ) {
             if (!notesDeleted) {
                 notesRemoveDialog.incrementProgressBy(1);
             } else {
                 notesRemoveDialog.dismiss();
                 mapView.invalidateOnUiThread();
             }
         }
     };
 
     private ArrayGenericOverlay dataOverlay;
 
     public void inalidateMap() {
         mapView.invalidateOnUiThread();
     }
 
     public boolean onKeyDown( int keyCode, KeyEvent event ) {
         // force to exit through the exit button
         if (keyCode == KeyEvent.KEYCODE_BACK && sliderIsOpen) {
             slidingDrawer.animateClose();
             return true;
         } else if (keyCode == KeyEvent.KEYCODE_BACK && osmSliderIsOpen) {
             osmSlidingDrawer.animateClose();
             return true;
         }
         return super.onKeyDown(keyCode, event);
     }
 
     private void enableDrawingWithDelay() {
         new Thread(new Runnable(){
             public void run() {
                 runOnUiThread(new Runnable(){
                     public void run() {
                         try {
                             Thread.sleep(300);
                         } catch (InterruptedException e) {
                             e.printStackTrace();
                         }
                         // mMapsOverlay.setDoDraw(true);
                         // mLogsOverlay.setDoDraw(true);
                         // mNotesOverlay.setDoDraw(true);
                         // mImagesOverlay.setDoDraw(true);
                         // mBookmarksOverlay.setDoDraw(true);
                         // mGpsOverlay.setDoDraw(true);
                         inalidateMap();
                     }
                 });
             }
         }).start();
     }
 
     private void disableDrawing() {
         // mMapsOverlay.setDoDraw(false);
         // mLogsOverlay.setDoDraw(false);
         // mNotesOverlay.setDoDraw(false);
         // mImagesOverlay.setDoDraw(false);
         // mBookmarksOverlay.setDoDraw(false);
         // mGpsOverlay.setDoDraw(false);
     }
 
     public void onLocationChanged( GpsLocation loc ) {
         if (loc == null) {
             return;
         }
 
         if (this.mapView.getWidth() <= 0 || this.mapView.getWidth() <= 0) {
             return;
         }
         float[] nsweE6 = getMapWorldBoundsE6();
         double lat = loc.getLatitude();
         double lon = loc.getLongitude();
         int latE6 = (int) ((float) lat * LibraryConstants.E6);
         int lonE6 = (int) ((float) lon * LibraryConstants.E6);
         boolean centerOnGps = preferences.getBoolean(Constants.PREFS_KEY_AUTOMATIC_CENTER_GPS, false);
 
         int nE6 = (int) nsweE6[0];
         int sE6 = (int) nsweE6[1];
         int wE6 = (int) nsweE6[2];
         int eE6 = (int) nsweE6[3];
 
         // Rect bounds = new Rect(wE6, nE6, eE6, sE6);
        if (lonE6 > wE6 && lonE6 < eE6 && latE6 > sE6 && latE6 < nE6) {
             GeoPoint point = new GeoPoint(latE6, lonE6);
             float accuracy = loc.getAccuracy();
             dataOverlay.setGpsPosition(point, accuracy);
             dataOverlay.requestRedraw();
         }
 
         Projection p = mapView.getProjection();
         int paddingX = (int) (p.getLongitudeSpan() * 0.2);
         int paddingY = (int) (p.getLatitudeSpan() * 0.2);
         int newEE6 = eE6 - paddingX;
         int newWE6 = wE6 + paddingX;
         int newSE6 = sE6 + paddingY;
         int newNE6 = nE6 - paddingY;
 
        Rect smallerBounds = new Rect(newWE6, newNE6, newEE6, newSE6);
         boolean doCenter = false;
        if (!smallerBounds.contains(lonE6, latE6)) {
             if (centerOnGps) {
                 doCenter = true;
             }
         }
        if (!smallerBounds.contains(lonE6, latE6)) {
            if (!centerOnGps) {
                return;
            }
        }
         if (doCenter) {
             setNewCenter(lon, lat, false);
             if (Debug.D)
                 Logger.i(this, "recentering triggered"); //$NON-NLS-1$                
         }
 
         // mapView.invalidateOnUiThread();
     }
 
     public void onStatusChanged( boolean hasFix ) {
     }
 
     // public boolean onScroll( ScrollEvent event ) {
     // saveCenterPref();
     // return true;
     // }
     //
     // public boolean onZoom( ZoomEvent event ) {
     // int zoomLevel = event.getZoomLevel();
     // if (zoomInButton != null) {
     // setZoomGuiText(zoomLevel);
     // }
     // saveCenterPref();
     // return true;
     // }
 
     private synchronized void saveCenterPref() {
         MapViewPosition mapPosition = mapView.getMapPosition();
         GeoPoint mapCenter = mapPosition.getMapCenter();
         double lon = mapCenter.longitudeE6 / LibraryConstants.E6;
         double lat = mapCenter.latitudeE6 / LibraryConstants.E6;
 
         if (Debug.D) {
             StringBuilder sb = new StringBuilder();
             sb.append("Map Center moved: "); //$NON-NLS-1$
             sb.append(lon);
             sb.append("/"); //$NON-NLS-1$
             sb.append(lat);
             Logger.i(this, sb.toString());
         }
 
         PositionUtilities.putMapCenterInPreferences(preferences, lon, lat, mapPosition.getZoomLevel());
     }
 
 }
