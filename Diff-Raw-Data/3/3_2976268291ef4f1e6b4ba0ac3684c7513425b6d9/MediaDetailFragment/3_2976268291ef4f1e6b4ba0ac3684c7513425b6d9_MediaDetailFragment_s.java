 package org.wikimedia.commons.media;
 
 import android.graphics.Bitmap;
 import android.os.Bundle;
 import android.text.Editable;
 import android.text.TextUtils;
 import android.text.TextWatcher;
 import android.view.LayoutInflater;
 import android.view.View;
 import android.view.ViewGroup;
 import android.widget.EditText;
 import android.widget.ImageView;
 import android.widget.ProgressBar;
 import android.widget.TextView;
 import com.actionbarsherlock.app.SherlockFragment;
 import com.nostra13.universalimageloader.core.DisplayImageOptions;
 import com.nostra13.universalimageloader.core.ImageLoader;
 import com.nostra13.universalimageloader.core.assist.FailReason;
 import com.nostra13.universalimageloader.core.assist.ImageLoadingListener;
 import com.nostra13.universalimageloader.core.assist.ImageScaleType;
 import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
 import org.wikimedia.commons.Media;
 import org.wikimedia.commons.R;
 import org.wikimedia.commons.Utils;
 
 public class MediaDetailFragment extends SherlockFragment {
 
     private boolean editable;
     private DisplayImageOptions displayOptions;
     private MediaDetailPagerFragment.MediaDetailProvider detailProvider;
     private int index;
 
     public static MediaDetailFragment forMedia(int index) {
         return forMedia(index, false);
     }
 
     public static MediaDetailFragment forMedia(int index, boolean editable) {
         MediaDetailFragment mf = new MediaDetailFragment();
 
         Bundle state = new Bundle();
         state.putBoolean("editable", editable);
         state.putInt("index", index);
 
         mf.setArguments(state);
 
         return mf;
     }
 
     private ImageView image;
     private EditText title;
     private ProgressBar loadingProgress;
     private ImageView loadingFailed;
 
 
     @Override
     public void onSaveInstanceState(Bundle outState) {
         super.onSaveInstanceState(outState);
         outState.putInt("index", index);
         outState.putBoolean("editable", editable);
     }
 
     @Override
     public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
         detailProvider = (MediaDetailPagerFragment.MediaDetailProvider)getActivity();
 
         if(savedInstanceState != null) {
             editable = savedInstanceState.getBoolean("editable");
             index = savedInstanceState.getInt("index");
         } else {
             editable = getArguments().getBoolean("editable");
             index = getArguments().getInt("index");
         }
         final Media media = detailProvider.getMediaAtPosition(index);
 
         View view = inflater.inflate(R.layout.fragment_media_detail, container, false);
         image = (ImageView) view.findViewById(R.id.mediaDetailImage);
         title = (EditText) view.findViewById(R.id.mediaDetailTitle);
         loadingProgress = (ProgressBar) view.findViewById(R.id.mediaDetailImageLoading);
         loadingFailed = (ImageView) view.findViewById(R.id.mediaDetailImageFailed);
 
         // Enable or disable editing on the title
         title.setClickable(editable);
         title.setFocusable(editable);
         title.setCursorVisible(editable);
         title.setFocusableInTouchMode(editable);
 
         String actualUrl = TextUtils.isEmpty(media.getImageUrl()) ? media.getLocalUri().toString() : media.getThumbnailUrl(640);
         ImageLoader.getInstance().displayImage(actualUrl, image, displayOptions, new ImageLoadingListener() {
             public void onLoadingStarted(String s, View view) {
                 loadingProgress.setVisibility(View.VISIBLE);
             }
 
             public void onLoadingFailed(String s, View view, FailReason failReason) {
                 loadingProgress.setVisibility(View.GONE);
                 loadingFailed.setVisibility(View.VISIBLE);
             }
 
             public void onLoadingComplete(String s, View view, Bitmap bitmap) {
                 loadingProgress.setVisibility(View.GONE);
                 loadingFailed.setVisibility(View.GONE);
                 image.setVisibility(View.VISIBLE);
                 if(bitmap.hasAlpha()) {
                     image.setBackgroundResource(android.R.color.white);
                 }
             }
 
             public void onLoadingCancelled(String s, View view) {
                 throw new RuntimeException("Image loading cancelled. But why?");
             }
         });
         title.setText(media.getDisplayTitle());
 
         title.addTextChangedListener(new TextWatcher() {
             public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
 
             }
 
             public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                 detailProvider.getMediaAtPosition(index).setFilename(title.getText().toString());
                 detailProvider.getMediaAtPosition(index).setTag("isDirty", true);
                 detailProvider.notifyDatasetChanged();
             }
 
             public void afterTextChanged(Editable editable) {
 
             }
         });
         return view;
     }
 
     @Override
     public void onActivityCreated(Bundle savedInstanceState) {
         super.onActivityCreated(savedInstanceState);
 
         displayOptions = Utils.getGenericDisplayOptions().build();
     }
 }
