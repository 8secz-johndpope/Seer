 package com.shortstack.hackertracker.Adapter;
 
 import android.content.Context;
 import android.database.SQLException;
 import android.database.sqlite.SQLiteDatabase;
 import android.database.sqlite.SQLiteException;
 import android.database.sqlite.SQLiteOpenHelper;
 
 import java.io.FileOutputStream;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.OutputStream;
 
 /**
  * Created with IntelliJ IDEA.
  * User: Whitney Champion
  * Date: 8/29/12
  * Time: 5:52 PM
  */
 public class DatabaseAdapter extends SQLiteOpenHelper {
 
     //The Android's default system path of your application database.
     private static String DB_PATH = "/data/data/com.shortstack.hackertracker/databases/";
 
     private static String DB_NAME = "hackertracker";
 
     private SQLiteDatabase myDataBase;
 
     private final Context myContext;
 
     public static final String KEY_ID = "_id";
     public static final String KEY_TITLE = "title";
     public static final String KEY_BODY = "body";
     public static final String KEY_NAME = "speaker";
     public static final String KEY_STARTTIME = "startTime";
     public static final String KEY_ENDTIME = "endTime";
     public static final String KEY_DATE = "date";
     public static final String KEY_LOCATION = "location";
     public static final String KEY_DEMO = "demo";
     public static final String KEY_TOOL = "tool";
     public static final String KEY_EXPLOIT = "exploit";
     public static final String KEY_FORUM = "forum";
     public static final String KEY_DAY = "day";
     public static final String KEY_MONTH = "month";
     public static final String KEY_YEAR = "year";
 
 
 
 
     /**
      * Constructor
      * Takes and keeps a reference of the passed context in order to access to the application assets and resources.
      * @param context
      */
     public DatabaseAdapter(Context context)  {
 
        super(context, DB_NAME, null, 2);
         this.myContext = context;
     }
 
 
 
     /**
      * Creates a empty database on the system and rewrites it with your own database.
      * */
     public void createDataBase() throws IOException {
 
         boolean dbExist = checkDataBase();
 
         if(dbExist){
             //do nothing - database already exist
         }else{
 
             //By calling this method and empty database will be created into the default system path
             //of your application so we are gonna be able to overwrite that database with our database.
             this.getReadableDatabase();
 
             try {
 
                 copyDataBase();
 
             } catch (IOException e) {
 
                 throw new Error("Error copying database");
 
             }
         }
 
     }
 
     /**
      * Check if the database already exist to avoid re-copying the file each time you open the application.
      * @return true if it exists, false if it doesn't
      */
     private boolean checkDataBase(){
 
         SQLiteDatabase checkDB = null;
 
         try{
             String myPath = DB_PATH + DB_NAME;
             checkDB = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY);
 
         }catch(SQLiteException e){
 
             //database does't exist yet.
 
         }
 
         if(checkDB != null){
 
             checkDB.close();
 
         }
 
         return checkDB != null ? true : false;
     }
 
     /**
      * Copies your database from your local assets-folder to the just created empty database in the
      * system folder, from where it can be accessed and handled.
      * This is done by transfering bytestream.
      * */
     private void copyDataBase() throws IOException{
 
         //Open your local db as the input stream
         InputStream myInput = myContext.getAssets().open(DB_NAME);
 
         // Path to the just created empty db
         String outFileName = DB_PATH + DB_NAME;
 
         //Open the empty db as the output stream
         OutputStream myOutput = new FileOutputStream(outFileName);
 
         //transfer bytes from the inputfile to the outputfile
         byte[] buffer = new byte[1024];
         int length;
         while ((length = myInput.read(buffer))>0){
             myOutput.write(buffer, 0, length);
         }
 
         //Close the streams
         myOutput.flush();
         myOutput.close();
         myInput.close();
 
     }
 
     public void openDataBase() throws SQLException {
 
         //Open the database
         String myPath = DB_PATH + DB_NAME;
         myDataBase = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY);
 
     }
 
     @Override
     public synchronized void close() {
 
         if(myDataBase != null)
             myDataBase.close();
 
         super.close();
 
     }
 
     @Override
     public void onCreate(SQLiteDatabase db) {
 
     }
 
     @Override
     public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS speakers");
        db.execSQL("DROP TABLE IF EXISTS contests");
        db.execSQL("DROP TABLE IF EXISTS entertainment");
        db.execSQL("DROP TABLE IF EXISTS vendors");
        onCreate(db);
     }
 
     // Add your public helper methods to access and get content from the database.
     // You could return cursors by doing "return myDataBase.query(....)" so it'd be easy
     // to you to create adapters for your views.
 
 }
