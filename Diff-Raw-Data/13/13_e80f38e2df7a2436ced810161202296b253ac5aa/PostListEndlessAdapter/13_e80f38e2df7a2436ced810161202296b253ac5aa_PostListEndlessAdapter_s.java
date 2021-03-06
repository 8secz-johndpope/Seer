 package ie.broadsheet.app.adapters;
 
 import ie.broadsheet.app.BaseFragmentActivity;
 import ie.broadsheet.app.BroadsheetApplication;
 import ie.broadsheet.app.R;
 import ie.broadsheet.app.model.json.PostList;
 import ie.broadsheet.app.requests.PostListRequest;
 import android.content.Context;
 import android.util.Log;
 
 import com.commonsware.cwac.endless.EndlessAdapter;
 import com.octo.android.robospice.persistence.DurationInMillis;
 import com.octo.android.robospice.persistence.exception.SpiceException;
 import com.octo.android.robospice.request.listener.RequestListener;
 
 public class PostListEndlessAdapter extends EndlessAdapter {
     private static final String TAG = "PostListEndlessAdapter";
 
     private boolean hasMore = true;
 
     private boolean loaded = false;
 
     private int currentPage = 0;
 
     private String searchTerm;
 
     private PostListRequest postListRequest;
 
     public PostListEndlessAdapter(Context context) {
         super(context, new PostListAdapter(context), R.layout.post_list_load_more);
 
         setRunInBackground(false);
     }
 
     @Override
     protected boolean cacheInBackground() throws Exception {
         if (hasMore) {
             currentPage++;
             fetchPosts();
         }
 
         return hasMore;
     }
 
     @Override
     protected void appendCachedData() {
 
     }
 
     public boolean isLoaded() {
         return loaded;
     }
 
     public void setLoaded(boolean loaded) {
         this.loaded = loaded;
     }
 
     public String getSearchTerm() {
         return searchTerm;
     }
 
     public void setSearchTerm(String searchTerm) {
         this.searchTerm = searchTerm;
     }
 
     public void reset() {
         loaded = false;
         hasMore = true;
         searchTerm = null;
         currentPage = 0;
         ((PostListAdapter) getWrappedAdapter()).clear();
     }
 
     public void fetchPosts() {
         if (postListRequest == null) {
             postListRequest = new PostListRequest();
 
             postListRequest.setPage(currentPage);
             postListRequest.setSearchTerm(searchTerm);
 
             BaseFragmentActivity activity = (BaseFragmentActivity) getContext();
 
             activity.getSpiceManager().execute(postListRequest, postListRequest.generateUrl(),
                     DurationInMillis.ONE_MINUTE, new PostListListener());
         }
     }
 
     // ============================================================================================
     // INNER CLASSES
     // ============================================================================================
 
     public final class PostListListener implements RequestListener<PostList> {
 
         @Override
         public void onRequestFailure(SpiceException spiceException) {
             Log.d(TAG, "Failed to get results");
         }
 
         @Override
         public void onRequestSuccess(final PostList result) {
             Log.d(TAG, "we got results");
 
             loaded = true;
 
             hasMore = (result.getCount_total() > result.getCount());
 
             ((PostListAdapter) getWrappedAdapter()).addAll(result.getPosts());
             onDataReady();
 
             postListRequest = null;
 
             BroadsheetApplication app = (BroadsheetApplication) PostListEndlessAdapter.this.getContext()
                     .getApplicationContext();
             app.setPosts(result.getPosts());
         }
     }
 }
