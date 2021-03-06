 package gr.ndre.scuttloid;
 
 import org.apache.http.HttpStatus;
 import org.xml.sax.helpers.DefaultHandler;
 
 import android.content.Context;

import java.util.Locale;
 
 /**
  * Enclose all API calls to Semantic Scuttle server
  */
 public class ScuttleAPI implements APITask.Callback {
 	
 	protected static final int BOOKMARKS = 0;
 
 	protected String url;
 	protected String username;
 	protected String password;
 	protected Integer handler;
 	
 	public static interface Callback {
 		public void onAPIError(String message);
 		public Context getContext();
 	}
 	
 	public static interface ResultCallback extends Callback {
 		public void onDataReceived(String data);
 	}
 	
 	public static interface BookmarksCallback extends Callback {
 		public void onBookmarksReceived(BookmarkContent bookmarks);
 	}
 	
 	protected Callback callback;
 	
 	/**
 	 * Constructor injecting mandatory preferences
 	 */
 	public ScuttleAPI(String url, String username, String password, Callback callback) {
 		this.url = url;
 		this.username = username;
 		this.password = password;
 		this.callback = callback;
 	}
 	
 	public void getBookmarks() {
 		this.handler = BOOKMARKS;
 		APITask task = this.getAPITask();
 		String url = this.getBaseURL();
 		url += "api/posts_all.php";
 		task.setURL(url);
 		task.setHandler(new BookmarksXMLHandler());
 		task.execute();
 	}
 	
 	@Override
 	public void onDataReceived(DefaultHandler handler) {
 		switch (this.handler) {
 			case BOOKMARKS:
 				BookmarkContent bookmarks = ((BookmarksXMLHandler)handler).getBookmarks();
 				((BookmarksCallback) this.callback).onBookmarksReceived(bookmarks);
 				break;
 			default:
 				
 		}
 	}
 	
 	protected APITask getAPITask() {
 		APITask task = new APITask(this, this.username, this.password);
 		return task;
 	}
 	
 	protected String getBaseURL() {
		String url = this.url;
		if( url.length() > 0 ) {
			String last = url.substring(url.length()-1);
			if( !last.equals("/") ) {
				url += "/";
			}
			String http = url.substring(0,7).toLowerCase(Locale.US);
			String https = url.substring(0,8).toLowerCase(Locale.US);
			if (http.compareTo("http://") != 0 && https.compareTo("https://") != 0) {
				url = "http://"+url;
			}
		}
		return url;
 	}
 
 	@Override
 	public void onError(int status) {
 		String message = "";
 		switch (status) {
 			case APITask.UNKNOWN_HOST:
 				message = this.callback.getContext().getString(R.string.error_unknownhost);
 				break;
 			case APITask.PARSE_ERROR:
 				message = this.callback.getContext().getString(R.string.error_xmlparse);
 				break;
 			case HttpStatus.SC_UNAUTHORIZED:
 				message = this.callback.getContext().getString(R.string.error_authentication);
 				break;
 			case HttpStatus.SC_NOT_FOUND:
 				// TODO : in some cases, 404 could mean "item not found" when deleting
 				message = this.callback.getContext().getString(R.string.error_notfound);
 				break;
 			default:
 				// TODO : Manage all issues ! Or display a generic message.
 				System.out.println(String.valueOf(status));
 		}
 		if (message != "") {
 			this.callback.onAPIError(message);
 		}
 	}
 
 }
