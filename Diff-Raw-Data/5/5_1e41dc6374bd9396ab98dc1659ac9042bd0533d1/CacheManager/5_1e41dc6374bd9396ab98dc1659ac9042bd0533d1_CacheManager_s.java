 /**
  * x lib is the library which includes the commonly used functions in 3 Sided Cube Android applications
  * 
  * @author Callum Taylor
 **/
 package x.lib;
 
 import java.io.*;
 import java.security.MessageDigest;
 import java.text.DecimalFormat;
 import java.util.*;
 
 import android.app.Activity;
 import android.app.AlertDialog;
 import android.app.ProgressDialog;
 import android.content.Context;
 import android.content.SharedPreferences;
 import android.graphics.Bitmap;
 import android.graphics.BitmapFactory;
 import android.graphics.Bitmap.CompressFormat;
 import android.os.AsyncTask;
 import android.os.FileObserver;
 import android.provider.OpenableColumns;
 import android.util.Log;
 
 /**
  * @brief This class is used to store and retrive data to the user's phone in a serialized form
  */
 public class CacheManager implements Serializable
 {
 	private String mCachePath;
 	private Context context;
 	private ArrayList<String> fileNames;
 	private String mPackageName;
 	
 	/**
 	 * The default constructor
 	 * @param context The application's context
 	 * @param packageName The application's unique package name identifier
 	 */
 	public CacheManager(Context context, String packageName)
 	{
 		this(context, packageName, false);
 	}
 
 	public CacheManager(Context context, String packageName, boolean useExternalCache)
 	{
 		this.context = context;
 		this.mPackageName = packageName;
 		fileNames = new ArrayList<String>();
 		
 		if (useExternalCache)
 		{
 			mCachePath = context.getExternalCacheDir().getAbsolutePath();
 		}
 		else
 		{
 			mCachePath = context.getCacheDir().getAbsolutePath();	
 		}
 	}
 	
 	public CacheManager(String path, String packageName)
 	{
 		mCachePath = path;
 		mPackageName = packageName;
 		fileNames = new ArrayList<String>();
 	}
 	
 	/**
 	 * Gets a base64'd MD5 hash of an input string
 	 * @param input The input string
 	 * @return The base64 MD5 hash of the input string
 	 */
 	public static String getHash(String input)
 	{
 		String hashFileName = "";
 		
     	try
     	{
     		MessageDigest md5 = MessageDigest.getInstance("MD5");
     		hashFileName = Base64.encodeBytes((md5.digest(input.getBytes()))).replace('/', '.');
     	}
     	catch (Exception e)
     	{
     		e.printStackTrace();
     	}
     	
     	return hashFileName;
 	}
     
 	/**
 	 * Gets a base64'd MD5 hash of an input string
 	 * @param input The serializable input data
 	 * @return The base64'd MD5 hash of the input string
 	 */
 	public static String getHash(Serializable input)
 	{
 		String hashFileName = "";
 		
     	try
     	{
     		ByteArrayOutputStream bos = new ByteArrayOutputStream();
     		ObjectOutput out = new ObjectOutputStream(bos);   
     		out.writeObject(input);
     		byte[] yourBytes = bos.toByteArray(); 
 
     		out.close();
     		bos.close();
     		
     		MessageDigest md5 = MessageDigest.getInstance("MD5");
     		hashFileName = Base64.encodeBytes((md5.digest(yourBytes))).replace('/', '.');
     	}
     	catch (Exception e)
     	{
     		e.printStackTrace();
     	}
     	
     	return hashFileName;
 	}
 
 	/**
 	 * Gets the total size of the cache in bytes
 	 * @return The size of the cache in bytes
 	 */
 	public long getCacheSize()
 	{
 		File files = new File(mCachePath);
 				
 		FileFilter filter = new FileFilter()
 		{
 			public boolean accept(File arg0) 
 			{				
 				if (arg0.getName().contains("cache_"))
 				{
 					return true;					
 				}
 				
 				return false;
 			};
 		};
 		
 		File[] fileList = files.listFiles(filter);
 		
 		long totalSize = 0;
 		for (File f : fileList)
 		{
 			totalSize += f.length();	
 		}
 						
 		return totalSize;
 	}
 	
 	/**
 	 * Checks if a file exists within the cache
 	 * @param fileName The file to check
 	 * @return True if the file exists, false if not
 	 */
 	public boolean fileExists(String fileName)
 	{
 		try
 		{
 			File f = new File(mCachePath, "cache_" + fileName);			
 			
 			return f.exists();
 		}
 		catch (Exception e)
 		{
 			e.printStackTrace();
 			return false;
 		}
 	}
 	
 	/**
 	 * Gets the modified date of the file
 	 * @param fileName The file
 	 * @return The modified date in ms since 1970 (EPOCH)
 	 */
 	public long fileModifiedDate(String fileName)
 	{
 		File f = new File(mCachePath, "cache_" + fileName);
 		
 		return f.lastModified();
 	}
 	
 	/**
 	 * Checks if a file was created before a certain date
 	 * @param fileName The file to check
 	 * @param date The date to check against
 	 * @return True if the file is older, false if not
 	 */
 	public boolean fileOlderThan(String fileName, long date)
 	{
 		long lastDate = fileModifiedDate(fileName);
 				
 		if (lastDate > date)
 		{						
 			return false;
 		}		
 		
 		return true;
 	}
 	
 	/**
 	 * Gets the file age in ms
 	 * @param fileName The file to check
 	 * @return The age of the file in ms
 	 */
 	public long getFileAge(String fileName)
 	{
 		return Math.abs(fileModifiedDate(fileName) - System.currentTimeMillis());
 	}
 	
 	/**
 	 * Gets the absolute file path of a file in cache
 	 * @param fileName The file name
 	 * @return The absolute file path 
 	 */
 	public String getFilePath(String fileName)
 	{
 		return getFilePath("", fileName);
 	}
 	
 	/**
 	 * Gets the absolute file path of a file in cache
 	 * @param folderName the folder name
 	 * @param fileName The file name 
 	 * @return The absolute file path 
 	 */
 	public String getFilePath(String folderName, String fileName)
 	{
 		try
 		{	
 			if (folderName != null && !folderName.equals(""))
 			{
 				folderName = "/cache_" + folderName;
 			}
 			
 			File file = new File(mCachePath + folderName, "cache_" + fileName);
 			return file.getAbsolutePath();
 		}
 		catch (Exception e)
 		{
 			return null;
 		}
 	}
 	
 	/**
 	 * Checks if the cache has reached the user's cache limit stored in user preference as "cacheLimit"
 	 * and removes the oldest files to make space
 	 */
 	public void checkCacheLimit()
 	{
 		long currentUsed = getCacheSize();
 		long currentCacheLimit;
 		
 		try
 		{
 			SharedPreferences mPrefs = context.getSharedPreferences(mPackageName, Context.MODE_WORLD_WRITEABLE);
 	        currentCacheLimit = mPrefs.getInt("cacheLimit", -1) * 1024 * 1024;
 		}
 		catch (Exception e) 
 		{
 			currentCacheLimit = -1;
 		}
 		
         if (currentCacheLimit > currentUsed || currentCacheLimit < 0) return;
         
 		File files = new File(mCachePath);	
 		FileFilter filter = new FileFilter()
 		{
 			public boolean accept(File arg0) 
 			{				
 				if (arg0.getName().contains("cache_"))
 				{
 					return true;					
 				}
 				
 				return false;
 			};
 		};
 		
 		Comparator c = new Comparator<File>()
 		{
 			public int compare(File object1, File object2)
 			{
 				if (object1.lastModified() > object2.lastModified())
 				{
 					return 1;
 				}
 				else if (object1.lastModified() < object2.lastModified())
 				{
 					return -1;
 				}
 				else
 				{
 					return 0;
 				}
 			}			
 		};
 		
 		File[] fileList = files.listFiles(filter);
 		Arrays.sort(fileList, c);
 		
 		for (File f : fileList)
 		{
 			if (currentUsed > currentCacheLimit)
 			{
 				currentUsed -= f.length();
 				f.delete();
 			}
 			else
 			{
 				break;			
 			}
 		}
 	}
 	
 	/**
 	 * Removes an image from the cache
 	 * @param imageName The image to remove
 	 * @return true if the file was deleted, otherwise false
 	 */
 	public boolean removeImage(String imageName)
 	{
 		return removeFile(null, imageName);
 	}
 	
 	/**
 	 * Removes an image from the cache
 	 * @param folderName The folder where the image is stored
 	 * @param imageName The image to remove
 	 * @return true if the file was deleted, otherwise false
 	 */
 	public boolean removeImage(String folderName, String imageName)
 	{
 		return removeFile(folderName, imageName);
 	}
 	
 	/**
 	 * Removes a file from the cache
 	 * @param fileName The file to remove
 	 * @return true if the file was deleted, otherwise false
 	 */
 	public boolean removeFile(String fileName)
 	{
		return removeFile(null, fileName);
 	}
 	
 	/**
 	 * Removes a file from the cache
 	 * @param folderName The folder where the image is stored 
 	 * @param fileName The file to remove
 	 * @return true if the file was deleted, otherwise false
 	 */
 	public boolean removeFile(String folderName, String fileName)
 	{
		if (folderName != null)
 		{	
 			folderName = "/cache_" + folderName;
 		}
 		
 		File f = new File(mCachePath + folderName, "cache_" + fileName);
 		return f.delete();
 	}
 	
 	/**
 	 * Deletes a folder
 	 * @param folderName The folder to delete
 	 * @return true if the folder was deleted, false if not
 	 */
 	public boolean removeFolder(String folderName)
 	{
 		File f = new File(mCachePath + "/cache_" + folderName);
 		
 		File[] fileList = f.listFiles();
 		double totalSize = 0;
 		for (File file : fileList)
 		{
 			f.delete();
 		}		
 		
 		return f.delete();
 	}
 	
 	/**
 	 * Adds an image to the cache
 	 * @param fileName The file name for the file
 	 * @param fileContents The contents for the file
 	 * @return true
 	 */
 	public boolean addImage(String fileName, Bitmap fileContents)
 	{
 		return addImage(null, fileName, fileContents, Bitmap.CompressFormat.PNG, null);
 	}
 	
 	/**
 	 * Adds an image to the cache
 	 * @param fileName The file name for the file
 	 * @param fileContents The contents for the file
 	 * @param l The on file written listener, called after the file was written to cache
 	 * @return true
 	 */
 	public boolean addImage(String fileName, Bitmap fileContents, OnFileWrittenListener l)
 	{
 		return addImage(null, fileName, fileContents, Bitmap.CompressFormat.PNG, l);
 	}
 	
 	/**
 	 * Adds an image to the cache
 	 * @param fileName The file name for the file
 	 * @param fileContents The contents for the file
 	 * @param l The on file written listener, called after the file was written to cache
 	 * @return true
 	 */
 	public boolean addImage(String folderName, String fileName, Bitmap fileContents, OnFileWrittenListener l)
 	{
 		return addImage(folderName, fileName, fileContents, Bitmap.CompressFormat.PNG, l);
 	}
 	
 	/**
 	 * Adds an image to the cache
 	 * @param fileName The file name for the file
 	 * @param fileContents The contents for the file
 	 * @param format The compression format for the image
 	 * @param l The on file written listener, called after the file was written to cache
 	 * @return true
 	 */
 	public boolean addImage(String folderName, String fileName, Bitmap fileContents, Bitmap.CompressFormat format, OnFileWrittenListener l)
 	{					
 		AddImageRunnable r = new AddImageRunnable(folderName, fileName, fileContents, format, l)
 		{						
 			public void run()
 			{
 				try
 				{			
 					File outputPath;
 					if (mFolderName != null)
 					{
 						File f = new File(mCachePath + "/cache_" + mFolderName);
 						f.mkdir();
 					
 						outputPath = new File(mCachePath + "/cache_" + mFolderName, "cache_" + mFileName);
 					}
 					else
 					{
 						outputPath = new File(mCachePath, "cache_" + mFileName);
 					}					
 										
 					FileOutputStream output = new FileOutputStream(outputPath);										
 					mImage.compress(mFormat, 40, output);
 								
 					output.flush();
 					output.close();	
 															
 					if (mListener != null)
 					{
 						mListener.onFileWritten(mFileName);
 					}
 					
 					mImage.recycle();
 					
 					//	Now delete to make up for more room
 					checkCacheLimit();
 				}
 				catch (Exception e)
 				{
 					e.printStackTrace();
 					checkCacheLimit();
 				}
 			}
 		};				
 		
 		((Activity)context).runOnUiThread(r);		
 		
 		return true;
 	}
 	
 	/**
 	 * Adds a file to the cache
 	 * @param fileName The file name for the file
 	 * @param fileContents The contents for the file
 	 * @return true
 	 */
 	public boolean addFile(String fileName, Serializable fileContents)
 	{
 		return addFile(null, fileName, fileContents, null);
 	}
 	
 	/**
 	 * Adds a file to the cache
 	 * @param folderName The folder for the file to be stored in
 	 * @param fileName The file name for the file
 	 * @param fileContents The contents for the file
 	 * @return true
 	 */
 	public boolean addFile(String folderName, String fileName, Serializable fileContents)
 	{
 		return addFile(folderName, fileName, fileContents, null);
 	}
 	
 	/**
 	 * Adds a file to the cache
 	 * @param folderName The folder for the file to be stored in
 	 * @param fileName The file name for the file
 	 * @param fileContents The contents for the file
 	 * @param l The listener for when the file has been written to cache
 	 * @return true
 	 */
 	public boolean addFile(String folderName, String fileName, Serializable fileContents, OnFileWrittenListener l)
 	{		
 		AddFileRunnable r = new AddFileRunnable(folderName, fileName, fileContents, l)
 		{						
 			public void run()
 			{
 				try
 				{							
 					String outputPath;
 					if (mFolderName != null && mFolderName.length() > 0)
 					{
 						File f = new File(mCachePath + "/cache_" + mFolderName);
 						f.mkdir();
 					
 						outputPath = "cache_" + mFolderName + "/cache_" + mFileName;
 					}
 					else
 					{
 						outputPath = "cache_" + mFileName;
 					}
 					
 					FileOutputStream fos = null;					
 					try
 					{
 						if (fos == null)
 						{
 							File f = new File(mCachePath + "/" + outputPath);
 							fos = new FileOutputStream(mCachePath + "/" + outputPath);
 						}
 						
 					    fos.write((byte[])mContents);
 					    fos.close();
 					}
 					catch (Exception e)
 					{
 						if (fos == null) 
 						{
 							File f = new File(mCachePath + "/" + outputPath);
 							fos = new FileOutputStream(mCachePath + "/" + outputPath);
 						}
 						
 						ObjectOutputStream stream = new ObjectOutputStream(fos);
 						
 						stream.writeObject(mContents);
 						stream.flush();
 						fos.flush();
 						stream.close();
 						fos.close();		
 					} 
 										
 					if (mListener != null)
 					{
 						mListener.onFileWritten(mFileName);
 					}					
 					
 					//	Now delete to make up for more room
 					checkCacheLimit();
 				}
 				catch (Exception e)
 				{
 					e.printStackTrace();
 					
 					checkCacheLimit();
 				}
 			} 
 		};				
 		
 		r.start();
 		
 		return true;		
 	}
 	
 	/**
 	 * Reads an image from cache
 	 * @param fileName The image to retrieve
 	 * @return The file as a bitmap or null if there was an OutOfMemoryError or Exception
 	 */
 	public Bitmap readImage(String fileName)
 	{
 		try
 		{					
 			File file = new File(mCachePath, "cache_" + fileName);
 			FileInputStream input = new FileInputStream(file);
 
 			BitmapFactory.Options opts = new BitmapFactory.Options();
 			opts.inDither = true;						
 			
 			Bitmap b = BitmapFactory.decodeStream(input, null, opts);	
 			
 			input.close();	
 			
 			return b;
 		}
 		catch (OutOfMemoryError e)
 		{
 			return null;
 		}
 		catch (Exception e)
 		{
 			e.printStackTrace();
 			return null;
 		}
 	}
 	
 	/**
 	 * Reads an image from cache
 	 * @paaram folderName The folder in which the cache file is stored
 	 * @param fileName The image to retrieve
 	 * @return The file as a bitmap or null if there was an OutOfMemoryError or Exception
 	 */
 	public Bitmap readImage(String folderName, String fileName)
 	{
 		try
 		{	
 			if (folderName != null && !folderName.equals(""))
 			{
 				folderName = "/cache_" + folderName;
 			}
 			
 			File file = new File(mCachePath + folderName, "cache_" + fileName);
 			FileInputStream input = new FileInputStream(file);
 
 			BitmapFactory.Options opts = new BitmapFactory.Options();
 			opts.inDither = true;						
 			
 			Bitmap b = BitmapFactory.decodeStream(input, null, opts);	
 			
 			input.close();	
 			
 			return b;
 		}
 		catch (OutOfMemoryError e)
 		{
 			return null;
 		}
 		catch (Exception e)
 		{
 			e.printStackTrace();
 			return null;
 		}
 	}
 	
 	/**
 	 * Reads a file from cache
 	 * @param fileName The file to retrieve
 	 * @return The file as an Object or null if there was an OutOfMemoryError or Exception
 	 */
 	public Object readFile(String fileName)
 	{
 		return readFile(null, fileName);
 	}
 	
 	/**
 	 * Reads a file from cache
 	 * @paaram folderName The folder in which the cache file is stored
 	 * @param fileName The file to retrieve
 	 * @return The file as an Object or null if there was an OutOfMemoryError or Exception
 	 */
 	public Object readFile(String folderName, String fileName)
 	{
 		try
 		{
 			String filePath = mCachePath;
 			if (folderName != null && !folderName.equals(""))
 			{
 				filePath += "/cache_" + folderName + "/";
 			}
 			
 			File file = new File(filePath, "cache_" + fileName);
 			FileInputStream input = new FileInputStream(file);
 			ObjectInputStream stream = new ObjectInputStream(input);
 			Object data = stream.readObject();
 			stream.close();
 			input.close();
 			
 			return data;
 		}
 		catch (OutOfMemoryError e)
 		{
 			return null;
 		}
 		catch (Exception e)
 		{
 			e.printStackTrace();
 			return null;
 		}
 	}
 	
 	/**
 	 * Clears the cache
 	 */
 	public void clearCache()
 	{
 		clearCache(false);
 	}
 
 	/**
 	 * Clears the cache
 	 * @param showProgress Shows a dialog or not
 	 */
 	public void clearCache(boolean showProgress)
 	{
 		ProgressDialog dialog = new ProgressDialog(context);
 		if (showProgress)
 		{			
 			dialog.setMessage("Clearing Cache");
 			dialog.show();
 		}
 		
 		File files = this.context.getCacheDir();		
 		FileFilter filter = new FileFilter()
 		{
 			public boolean accept(File arg0) 
 			{				
 				if (arg0.getName().contains("cache_"))
 				{
 					return true;					
 				}
 				
 				return false;
 			};
 		};
 		
 		File[] fileList = files.listFiles(filter);
 		double totalSize = 0;
 		for (File f : fileList)
 		{
 			f.delete();
 		}
 		
 		if (showProgress)
 		{
 			dialog.dismiss();
 		}
 	}
 	
 	/**
 	 * @brief The class that adds files to the cache in its own thread
 	 */
 	private class AddImageRunnable extends Thread
 	{
 		protected String mFolderName;
 		protected String mFileName;
 		protected Bitmap mImage;
 		protected Bitmap.CompressFormat mFormat;		
 		protected OnFileWrittenListener mListener;
 		
 		public AddImageRunnable()
 		{
 			
 		}
 		
 		public AddImageRunnable(String folderName, String fileName, Bitmap image, Bitmap.CompressFormat format, OnFileWrittenListener l)
 		{
 			mFolderName = folderName;
 			mFileName = fileName;
 			mImage = image;
 			mFormat = format;
 			mListener = l;
 		}
 	}
 	
 	/**
 	 * @brief The class that adds files to the cache in its own thread
 	 */
 	private class AddFileRunnable extends Thread
 	{
 		protected String mFolderName;
 		protected String mFileName;
 		protected Serializable mContents;
 		protected OnFileWrittenListener mListener;
 		
 		public AddFileRunnable()
 		{
 			
 		}
 		
 		public AddFileRunnable(String folderName, String fileName, Serializable contents, OnFileWrittenListener l)
 		{
 			mFolderName = folderName;
 			mFileName = fileName;
 			mContents = contents;			
 			mListener = l;
 		}
 	}
 	
 	/**
 	 * @brief Interface for when the file has been written to cache
 	 */
 	public interface OnFileWrittenListener
 	{
 		/**
 		 * Method called when the file has been written to the cache
 		 * @param fileName The file name of the file written
 		 */
 		public void onFileWritten(String fileName);
 	}
 	
 	/**
 	 * @brief The class that serailizes data
 	 */
 	public static class Serializer implements Serializable
 	{
 		public static byte[] serializeBitmap(Bitmap data)
 		{
 			try
 			{
 				ByteArrayOutputStream bos = new ByteArrayOutputStream();
 				data.compress(CompressFormat.PNG, 100, bos);
 				byte[] bytes = bos.toByteArray();
 				
 				return bytes;
 			}
 			catch (Exception e) 
 			{
 				e.printStackTrace();
 				return null;
 			}			
 		}
 		
 		/**
 		 * Serializes data into bytes
 		 * @param data The data to be serailized
 		 * @return The serialized data in a byte array
 		 */
 		public static byte[] serializeObject(Object data)
 		{		
 			try
 			{
 				ByteArrayOutputStream bos = new ByteArrayOutputStream();
 				ObjectOutput out = new ObjectOutputStream(bos);   
 				out.writeObject(data);
 				byte[] yourBytes = bos.toByteArray(); 		
 				
 				return yourBytes;
 			}
 			catch (Exception e)
 			{
 				e.printStackTrace();
 				return null;
 			}					
 		}
 		
 		/**
 		 * Deserailizes data into an object
 		 * @param data The byte array to be deserialized
 		 * @return The data as an object
 		 */
 		public static Object desterializeObject(byte[] data)
 		{
 			try 
 			{
 				ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(data));
 				Object objectData = input.readObject();
 				input.close();
 				
 				return objectData;
 			}
 			catch (Exception e)
 			{
 				e.printStackTrace();
 				return null;
 			}
 		}
 	}
 }
