 package com.quanleimu.view.fragment;
 
 import java.io.IOException;
 import java.io.UnsupportedEncodingException;
 import java.lang.ref.WeakReference;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.List;
 
 import org.json.JSONException;
 import org.json.JSONObject;
 
 import android.app.Activity;
 import android.app.AlertDialog;
 import android.app.Dialog;
 import android.content.Context;
 import android.content.DialogInterface;
 import android.content.Intent;
 import android.graphics.Bitmap;
 import android.graphics.BitmapFactory;
 import android.net.Uri;
 import android.os.Bundle;
 import android.os.Handler;
 import android.os.Message;
 import android.support.v4.view.PagerAdapter;
 import android.support.v4.view.ViewPager;
 import android.support.v4.view.ViewPager.OnPageChangeListener;
 import android.util.Base64;
 import android.view.LayoutInflater;
 import android.view.MenuItem;
 import android.view.MotionEvent;
 import android.view.View;
 import android.view.ViewGroup;
 import android.view.WindowManager;
 import android.view.animation.Animation;
 import android.view.animation.Animation.AnimationListener;
 import android.view.animation.AnimationUtils;
 import android.widget.AdapterView;
 import android.widget.AdapterView.OnItemSelectedListener;
 import android.widget.BaseAdapter;
 import android.widget.Gallery;
 import android.widget.ImageButton;
 import android.widget.ImageView;
 import android.widget.LinearLayout;
 import android.widget.LinearLayout.LayoutParams;
 import android.widget.RelativeLayout;
 import android.widget.TextView;
 import android.widget.Toast;
 
 import com.quanleimu.activity.BaiduMapActivity;
 import com.quanleimu.activity.BaseActivity;
 import com.quanleimu.activity.BaseFragment;
 import com.quanleimu.activity.QuanleimuApplication;
 import com.quanleimu.activity.R;
 import com.quanleimu.entity.GoodsDetail;
 import com.quanleimu.entity.GoodsDetail.EDATAKEYS;
 import com.quanleimu.entity.GoodsList;
 import com.quanleimu.entity.UserBean;
 import com.quanleimu.imageCache.SimpleImageLoader;
 import com.quanleimu.jsonutil.JsonUtil;
 import com.quanleimu.util.BXStatsHelper;
 import com.quanleimu.util.Communication;
 import com.quanleimu.util.ErrorHandler;
 import com.quanleimu.util.GoodsListLoader;
 import com.quanleimu.util.Helper;
 import com.quanleimu.util.TextUtil;
 import com.quanleimu.util.Util;
 import com.quanleimu.util.ViewUtil;
 import com.quanleimu.view.AuthController;
 import com.quanleimu.widget.ContextMenuItem;
 //import com.quanleimu.entity.AuthDialogListener;
 //import com.quanleimu.entity.WeiboAccessTokenWrapper;
 //import com.tencent.mm.sdk.openapi.WXAppExtendObject;
 //import com.tencent.mm.sdk.openapi.WXMediaMessage;
 //import com.tencent.mm.sdk.platformtools.Log;
 //import com.weibo.net.AccessToken;
 //import com.weibo.net.Oauth2AccessTokenHeader;
 //import com.weibo.net.Utility;
 //import com.weibo.net.Weibo;
 //import com.weibo.net.WeiboException;
 //import com.weibo.net.WeiboParameters;
 
 public class GoodDetailFragment extends BaseFragment implements AnimationListener, View.OnTouchListener,View.OnClickListener, OnItemSelectedListener/*, PullableScrollView.PullNotifier, View.OnTouchListener*/, GoodsListLoader.HasMoreListener{
 
 	public interface IListHolder{
 		public void startFecthingMore();
 		public boolean onResult(int msg, GoodsListLoader loader);//return true if getMore succeeded, else otherwise
 	};
 	
 	
 //	final private String strCollect = "收藏";
 //	final private String strCancelCollect = "取消收藏";
 	final private int msgRefresh = 5;
 	final private int msgUpdate = 6;
 	final private int msgDelete = 7;
 	
 	public static final int MSG_ADINVERIFY_DELETED = 0x00010000;
 	public static final int MSG_MYPOST_DELETED = 0x00010001;
 	private static final int MSG_HIDE_ARROW = 0x00010002;
 
 	public GoodsDetail detail = new GoodsDetail();
 //	private View titleControlView = null;
 	private AuthController authCtrl;
 	
 	private String json = "";
 	
 //	private Bundle mBundle;
 	
 	private Bitmap mb_loading = null;
 	
 	private int type = 240;//width of screen
 //	private int paddingLeftMetaPixel = 16;//meta, right part, value
 	
 	private boolean keepSilent = false;
 	
 	private GoodsListLoader mListLoader;
 //	private int mCurIndex = 0;
 	
 	private IListHolder mHolder = null;
 	
 	private Dialog manageDlg = null;
 	
 	private WeakReference<View> loadingMorePage;
 	
 	private boolean initCalled = false;
 	
 	enum REQUEST_TYPE{
 		REQUEST_TYPE_REFRESH,
 		REQUEST_TYPE_UPDATE,
 		REQUEST_TYPE_DELETE
 	}
 	
 	@Override
 	public void onDestroy(){
 		this.keepSilent = true;
 		
 //		if(null != listUrl && listUrl.size() > 0)
 //			SimpleImageLoader.Cancel(listUrl);
 //		this.mListLoader = null;
 		
 		Thread t = new Thread(new Runnable(){
 			public void run(){
 				Helper.saveDataToLocate(QuanleimuApplication.getApplication().getApplicationContext(), "listLookHistory", QuanleimuApplication.getApplication().getListLookHistory());
 			}
 		});
 		t.start();
 	
 		super.onDestroy();
 	}
 	
 	@Override
 	public boolean handleBack(){
 		this.keepSilent = false;
 
 		return false;
 	}
 	
 	
 	
 	@Override
 	public void onPause() {
 		this.keepSilent = true;
 		super.onPause();
 		BXStatsHelper.getInstance().send();
 		Gallery glDetail = (Gallery) getView().findViewById(R.id.glDetail);
 		if(glDetail != null){
 //			glDetail.getc
 		}
 		glDetail = null;
 		
 	}
 	
 	@Override
 	public void onResume(){
 		updateButtonStatus();
 		
 //		QuanleimuApplication.addViewCounter(this.detail.getValueByKey(GoodsDetail.EDATAKEYS.EDATAKEYS_ID));
 		this.keepSilent = false;
 		super.onResume();
 	}
 	
 	private void updateButtonStatus()
 	{
 //		if(isMyAd()){
 //			btnStatus = 1;
 //		}
 //		else{
 //			if(isInMyStore()){
 //				btnStatus = 0;
 //			}
 //			else{
 //				btnStatus = -1;
 //			}
 //		}
 	}
 	
 	private void saveToHistory(){
 		List<GoodsDetail> listLookHistory = QuanleimuApplication.getApplication().getListLookHistory();
 		if(listLookHistory != null){
 			for(int i=0;i<listLookHistory.size();i++)
 			{
 				if(listLookHistory.get(i).getValueByKey(GoodsDetail.EDATAKEYS.EDATAKEYS_ID)
 						.equals(detail.getValueByKey(GoodsDetail.EDATAKEYS.EDATAKEYS_ID)))
 				{
 					return;
 				}
 			}
 		}
 		if(null == listLookHistory){
 			listLookHistory = new ArrayList<GoodsDetail>();
 		}
 		listLookHistory.add(0, detail);
 		QuanleimuApplication.getApplication().setListLookHistory(listLookHistory);
 //		Helper.saveDataToLocate(this.getContext(), "listLookHistory", listLookHistory);		
 	}
 
 	private boolean isMyAd(){
 		if(detail == null) return false;
 		List<GoodsDetail> myPost = QuanleimuApplication.getApplication().getListMyPost();
 		if(null != myPost){
 			for(int i = 0; i < myPost.size(); ++ i){
 				if(myPost.get(i).getValueByKey(GoodsDetail.EDATAKEYS.EDATAKEYS_ID)
 						.equals(detail.getValueByKey(GoodsDetail.EDATAKEYS.EDATAKEYS_ID))){
 					return true;
 				}
 			}
 		}
 		return false;
 	}
 	private boolean isInMyStore(){
 		if(detail == null) return false;
 		List<GoodsDetail> myStore = QuanleimuApplication.getApplication().getListMyStore();
 		if(myStore == null) return false;
 		for(int i = 0; i < myStore.size(); ++ i){
 			if(myStore.get(i).getValueByKey(GoodsDetail.EDATAKEYS.EDATAKEYS_ID)
 					.equals(detail.getValueByKey(GoodsDetail.EDATAKEYS.EDATAKEYS_ID))){
 				return true;
 			}
 		}
 		return false;		
 	}
 //	
 //	private int mLastY = 0;
 //	private int mLastExceedingY = 0;
 	public boolean onTouch (View v, MotionEvent event){
 //		if(!keepSilent){
 //			switch(event.getAction()){
 //			case MotionEvent.ACTION_MOVE:
 //				
 //				break;
 //			case MotionEvent.ACTION_CANCEL:
 //			case MotionEvent.ACTION_UP:
 //				//mLastExceedingY = 0;
 //				break;
 //			}
 //		}		
 	    switch (event.getAction()) {
 	    case MotionEvent.ACTION_MOVE: 
 	    	((ViewPager)getView().findViewById(R.id.svDetail)).requestDisallowInterceptTouchEvent(true);
 	        break;
 	    case MotionEvent.ACTION_UP:
 	    case MotionEvent.ACTION_CANCEL:
 	    	((ViewPager)getView().findViewById(R.id.svDetail)).requestDisallowInterceptTouchEvent(false);
 	        break;		
 	    }
 		return this.keepSilent;
 	}
 
 	@Override
 	public void onCreate(Bundle savedInstanceState) {
 		super.onCreate(savedInstanceState);
 		this.mListLoader = (GoodsListLoader) getArguments().getSerializable("loader");
 		int index = getArguments().getInt("index", 0);
 		detail = mListLoader.getGoodsList().getData().get(index);
 		if (savedInstanceState != null) //
 		{
 //			this.mListLoader.setHandler(handler);
 			this.mListLoader.setHasMoreListener(this);
 		}
         
 		//the ad is viewed once
 		QuanleimuApplication.addViewCounter(this.detail.getValueByKey(GoodsDetail.EDATAKEYS.EDATAKEYS_ID));	
         initCalled = true;		
 	}
 
 	@Override
 	public View onCreateView(LayoutInflater inflater, ViewGroup container,
 			Bundle savedInstanceState) {
 		
 		final int mCurIndex = getArguments().getInt("index", 0);
 		this.keepSilent = false;//magic flag to refuse unexpected touch event
 		
 		WindowManager wm = 
 				(WindowManager)QuanleimuApplication.getApplication().getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
 		type = wm.getDefaultDisplay().getWidth();
 		
 		//different padding for meta value to avoid overlapping display
 //		if (type < 480) {
 //			this.paddingLeftMetaPixel = 0;
 //		}else{
 //			this.paddingLeftMetaPixel = 16;
 //		}
 		
 		
 		final View v = inflater.inflate(R.layout.gooddetailview, null);
 		
 		BitmapFactory.Options o =  new BitmapFactory.Options();
         o.inPurgeable = true;
         mb_loading = BitmapFactory.decodeResource(getActivity().getResources(), R.drawable.moren1, o);
         
         final ViewPager vp = (ViewPager) v.findViewById(R.id.svDetail);
         vp.setAdapter(new PagerAdapter() {
 			
 			public Object instantiateItem(View arg0, int position) 
 			{
 				Integer posObj = Integer.valueOf(position);
 				View detail = LayoutInflater.from(vp.getContext()).inflate(R.layout.gooddetailcontent, null);
 				detail.setTag(posObj);
 				
 				detail.setTag(R.id.accountEt, detail);
 				((ViewPager) arg0).addView(detail, 0);
 				if (position == mListLoader.getGoodsList().getData().size())
 				{
 					detail.findViewById(R.id.loading_more_progress_parent).setVisibility(View.VISIBLE);
 					detail.findViewById(R.id.llDetail).setVisibility(View.GONE);
 					loadMore(detail);
 				}
 				else
 				{
 					initContent(detail, mListLoader.getGoodsList().getData().get(position), position, ((ViewPager) arg0), true);
 				}
 				return detail;
 			}
 			
             public void destroyItem(View arg0, int index, Object arg2)
             {
             	Gallery glDetail = (Gallery) ((View)arg2).findViewById(R.id.glDetail);
             	if(glDetail != null){
             		glDetail.setAdapter(null);
             	}
                 ((ViewPager) arg0).removeView((View) arg2);
                 
                 final Integer pos = (Integer) ((View) arg2).getTag();
                 arg2 = null;
                 if (pos < mListLoader.getGoodsList().getData().size())
                 {
 //                	Log.d("imagecount", "imagecount, destroyItem: " + pos + "  " + mListLoader.getGoodsList().getData().get(pos).toString());
                 	List<String> listUrl = getImageUrls(mListLoader.getGoodsList().getData().get(pos));
                 	if(null != listUrl && listUrl.size() > 0){
                 		SimpleImageLoader.Cancel(listUrl);
 	            		for(int i = 0; i < listUrl.size(); ++ i){
 	            			decreaseImageCount(listUrl.get(i), pos);
 	//            			QuanleimuApplication.getImageLoader().forceRecycle(listUrl.get(i));
 	            		}
                 	}
                 }
                 
                 
             }
 
 			public boolean isViewFromObject(View arg0, Object arg1) {
 				return arg0 == arg1;
 			}
 			
 			public int getCount() {
 				return mListLoader.getGoodsList().getData().size() + (mListLoader.hasMore() ? 1 : 0);
 			}
 		});
         vp.setCurrentItem(mCurIndex);
         vp.setOnPageChangeListener(new OnPageChangeListener() {
 			
 			public void onPageSelected(int pos) {
 				keepSilent = false;//magic flag to refuse unexpected touch event
 				if (pos != mListLoader.getGoodsList().getData().size())
 				{
 					detail = mListLoader.getGoodsList().getData().get(pos);
 					mListLoader.setSelection(pos);
 					updateTitleBar(getTitleDef());
 					updateContactBar(v.getRootView(), false);
 //					reCreateTitle();
 //					refreshHeader();
 					saveToHistory();
 //					if(!initCalled){
 //						//the ad is viewed once
 						QuanleimuApplication.addViewCounter(detail.getValueByKey(GoodsDetail.EDATAKEYS.EDATAKEYS_ID));
 //					}else{
 //						initCalled = false;
 //					}
 					
 					updateButtonStatus();
 					if(pos == 0){
 						v.findViewById(R.id.btn_prev).setVisibility(View.GONE);
 					}else{
 						v.findViewById(R.id.btn_prev).setVisibility(View.VISIBLE);
 					}
 					v.findViewById(R.id.btn_next).setVisibility(View.VISIBLE);
 //					GoodDetailFragment.this.myHandler.sendEmptyMessageDelayed(MSG_HIDE_ARROW, 1000);
 					handler.sendEmptyMessageDelayed(MSG_HIDE_ARROW, 1000);
 				
 				}
 				else
 				{
 					updateTitleBar(getTitleDef());
 					updateContactBar(v.getRootView(), true);
 				}
 			}
 			
 			public void onPageScrolled(int arg0, float arg1, int arg2) {
 				
 			}
 			
 			public void onPageScrollStateChanged(int arg0) {
 				
 			}
 		});
         
         
         
         mListLoader.setSelection(mCurIndex);
         mListLoader.setHandler(new Handler(){
 				@Override
 				public void handleMessage(Message msg) {
 					if(null != mHolder){
 						if(mHolder.onResult(msg.what, mListLoader)){
 							onGotMore();
 						}else{
 							onNoMore();
 						}
 						
 						if(msg.what == ErrorHandler.ERROR_NETWORK_UNAVAILABLE){
 							onLoadMoreFailed();
 						}
 					}else{
 						switch (msg.what) {
 						case GoodsListLoader.MSG_FINISH_GET_FIRST:				 
 							GoodsList goodsList = JsonUtil.getGoodsListFromJson(mListLoader.getLastJson());
 							mListLoader.setGoodsList(goodsList);
 							if (goodsList == null || goodsList.getData().size() == 0) {
 								Message msg1 = Message.obtain();
 								msg1.what = ErrorHandler.ERROR_COMMON_FAILURE;
 								Bundle bundle = new Bundle();
 								bundle.putString("popup_message", "没有符合的结果，请稍后并重试！");
 								msg1.setData(bundle);
 								QuanleimuApplication.getApplication().getErrorHandler().sendMessage(msg1);
 							} else {
 								//QuanleimuApplication.getApplication().setListGoods(goodsList.getData());
 							}
 							mListLoader.setHasMore(true);
 							
 							break;
 						case GoodsListLoader.MSG_NO_MORE:					
 							onNoMore();
 							
 							mListLoader.setHasMore(false);
 							
 							break;
 						case GoodsListLoader.MSG_FINISH_GET_MORE:	
 							GoodsList goodsList1 = JsonUtil.getGoodsListFromJson(mListLoader.getLastJson());
 							if (goodsList1 == null || goodsList1.getData().size() == 0) {
 								Message msg2 = Message.obtain();
 								msg2.what = ErrorHandler.ERROR_COMMON_WARNING;
 								Bundle bundle1 = new Bundle();
 								bundle1.putString("popup_message", "后面没有啦！");
 								msg2.setData(bundle1);
 								QuanleimuApplication.getApplication().getErrorHandler().sendMessage(msg2);
 								
 								onNoMore();
 								
 								mListLoader.setHasMore(false);
 							} else {
 								List<GoodsDetail> listCommonGoods =  goodsList1.getData();
 								for(int i=0;i<listCommonGoods.size();i++)
 								{
 									mListLoader.getGoodsList().getData().add(listCommonGoods.get(i));
 								}
 								//QuanleimuApplication.getApplication().setListGoods(mListLoader.getGoodsList().getData());	
 								
 								mListLoader.setHasMore(true);
 								
 								onGotMore();
 							}
 							break;
 						case ErrorHandler.ERROR_NETWORK_UNAVAILABLE:
 							Message msg2 = Message.obtain();
 							msg2.what = ErrorHandler.ERROR_NETWORK_UNAVAILABLE;
 							QuanleimuApplication.getApplication().getErrorHandler().sendMessage(msg2);
 							
 							onLoadMoreFailed();
 							
 							break;
 						}
 					}
 					
 					super.handleMessage(msg);
 				}
 
 				private void onGotMore() {
 					final View page = loadingMorePage == null ? null : (View) loadingMorePage.get();
 					if (page != null)
 					{
 						page.postDelayed(new Runnable() {
 
 							@Override
 							public void run() {
 								page.findViewById(R.id.loading_more_progress_parent).setVisibility(View.GONE);
 								page.findViewById(R.id.llDetail).setVisibility(View.VISIBLE);
 								final Integer tag = (Integer)page.getTag();
 								initContent(page, mListLoader.getGoodsList().getData().get(tag.intValue()), tag.intValue(), null, false);
 							}
 							
 						}, 10);
 					}
 				}
 
 				private void onNoMore() {
 					View root = getView();
 					if (root != null)
 					{
 						ViewUtil.postShortToastMessage(root, "后面没有啦！", 0);
 					}
 				}
 			});        
         
         handler.sendEmptyMessageDelayed(MSG_HIDE_ARROW, 3000);
         this.saveToHistory();
         return v;
 	}
 	
 	private boolean isValidMessage()
 	{
 		return !detail.getValueByKey("status").equals("4") && !detail.getValueByKey("status").equals("20");
 	}
 	
 	private void initContent(View contentView, final GoodsDetail detail, final int pageIndex, ViewPager pager, boolean useRoot)
 	{
 		if(this.getView() == null) return;
 		if(useRoot)
 			contentView = contentView.getRootView();
 		
 		WindowManager wm = 
 				(WindowManager)QuanleimuApplication.getApplication().getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
 		type = wm.getDefaultDisplay().getWidth();
 		
 		//different padding for meta value to avoid overlapping display
 //		if (type < 480) {
 //			this.paddingLeftMetaPixel = 0;
 //		}else{
 //			this.paddingLeftMetaPixel = 16;
 //		}
 		
 		RelativeLayout llgl = (RelativeLayout) contentView.findViewById(R.id.llgl);
 		
 		if(isValidMessage()){
 			contentView.findViewById(R.id.ll_appeal).setVisibility(View.GONE);
 			contentView.findViewById(R.id.graymask).setVisibility(View.GONE);
 			contentView.findViewById(R.id.verifyseperator).setVisibility(View.GONE);
 		}
 		else{
 			
 			contentView.findViewById(R.id.llMainContent).setEnabled(false);
 			contentView.findViewById(R.id.ll_appeal).setVisibility(View.VISIBLE);
 			contentView.findViewById(R.id.graymask).setVisibility(View.VISIBLE);
 			contentView.findViewById(R.id.verifyseperator).setVisibility(View.VISIBLE);
 			
 			if(detail.getValueByKey("tips").equals("")){
 				((TextView)contentView.findViewById(R.id.verifyreason)).setText("该信息不符合《百姓网公约》");
 			}
 			else{
 				((TextView)contentView.findViewById(R.id.verifyreason)).setText(detail.getValueByKey("tips"));
 			}
 //			contentView.findViewById(R.id.fenxianglayout).setEnabled(false);
 //			contentView.findViewById(R.id.showmap).setEnabled(false);
 //			contentView.findViewById(R.id.jubaolayout).setEnabled(false);
 //			findViewById(R.id.sms).setEnabled(false);
 //			findViewById(R.id.call).setEnabled(false);
 			contentView.findViewById(R.id.appealbutton).setOnClickListener(this);
 		}
 		
 		if(detail.getImageList() != null){
 			List<String>listUrl = getImageUrls(detail);
 			
 			if(listUrl.size() == 0){
 				llgl = (RelativeLayout) contentView.findViewById(R.id.llgl);
 				llgl.setVisibility(View.GONE);
 			}else{
 				Gallery glDetail = (Gallery) contentView.findViewById(R.id.glDetail);
 				glDetail.setOnItemSelectedListener(this);
 				glDetail.setFadingEdgeLength(10);
 				glDetail.setSpacing(40);
 				
 				MainAdapter adapter = new MainAdapter(contentView.getContext(), listUrl, pageIndex);
 				glDetail.setAdapter(adapter);
 				glDetail.setOnTouchListener(this);
 				glDetail.setOnItemClickListener(new AdapterView.OnItemClickListener() {
 
 					@Override
 					public void onItemClick(AdapterView<?> arg0, View arg1,
 							int arg2, long arg3) {
 						Bundle bundle = createArguments(null, null);
 						bundle.putInt("postIndex", arg2);
 						bundle.putSerializable("goodsDetail", detail);
 						
 						pushFragment(new BigGalleryFragment(), bundle);
 					}
 				});
 				
 			}
 		}else{
 			llgl = (RelativeLayout) contentView.findViewById(R.id.llgl);
 			llgl.setVisibility(View.GONE);
 		}
 //		rl_test = (RelativeLayout) findViewById(R.id.detailLayout);
 		
 
 		TextView txt_tittle = (TextView) contentView.findViewById(R.id.goods_tittle);
 		TextView txt_message1 = (TextView) contentView.findViewById(R.id.sendmess1);
 //		rl_address.setOnTouchListener(this);
 
 		LinearLayout ll_meta = (LinearLayout) contentView.findViewById(R.id.meta);
 		
 //		View fenxiang = contentView.findViewById(R.id.fenxianglayout);
 //		fenxiang.setOnClickListener(this);
 
 		
 //		if(QuanleimuApplication.wxapi != null && QuanleimuApplication.wxapi.isWXAppInstalled() && QuanleimuApplication.wxapi.isWXAppSupportAPI()){
 //			contentView.findViewById(R.id.wxlayout).setOnClickListener(this);
 //		}
 //		else{
 //			contentView.findViewById(R.id.wxlayout).setVisibility(View.GONE);
 //		}
 		
 
 		this.setMetaObject(contentView, detail);
 		
 		String title = detail.getValueByKey(GoodsDetail.EDATAKEYS.EDATAKEYS_TITLE);
 		String description = detail.getValueByKey(GoodsDetail.EDATAKEYS.EDATAKEYS_DESCRIPTION);
 		if ((title == null || title.length() == 0) && description != null)
 		{
 			title = description.length() > 40 ? description.substring(0, 40) : description;
 		}
 		
 		txt_message1.setText(description);
 		txt_tittle.setText(title);
 
 		final ViewPager vp = pager != null ? pager : (ViewPager) getActivity().findViewById(R.id.svDetail);
 		if (vp != null && pageIndex == vp.getCurrentItem())
 		{
 			updateTitleBar(getTitleDef());
 			updateContactBar(vp.getRootView(), false);
 		}
 		
 	}
 	
 	private void updateContactBar(View rootView, boolean forceHide)
 	{
 		LinearLayout rl_phone = (LinearLayout)rootView.findViewById(R.id.phonelayout);
 		if (forceHide)
 		{
 			rl_phone.setVisibility(View.GONE);
 			return;
 		}
 		else if (isMyAd() || !isValidMessage())
 		{
 			rootView.findViewById(R.id.phone_parent).setVisibility(View.GONE);
 			rootView.findViewById(R.id.vad_tool_bar).setVisibility(View.VISIBLE);
 			
 			
 			rootView.findViewById(R.id.vad_btn_edit).setOnClickListener(this);
 			rootView.findViewById(R.id.vad_btn_refresh).setOnClickListener(this);
 			rootView.findViewById(R.id.vad_btn_delete).setOnClickListener(this);
 			
 			if (!isValidMessage())
 			{
 				rootView.findViewById(R.id.vad_btn_edit).setVisibility(View.GONE);
 				rootView.findViewById(R.id.vad_btn_refresh).setVisibility(View.GONE);
 			}
 			return;
 		}
 		
 		
 		rootView.findViewById(R.id.phone_parent).setVisibility(View.VISIBLE);
 		rootView.findViewById(R.id.vad_tool_bar).setVisibility(View.GONE);
 	
 		
 		ImageButton btnBuzz = (ImageButton) rootView.findViewById(R.id.vad_buzz_btn);
 		btnBuzz.setImageResource(isCurrentAdFromMobile() ? R.drawable.icon_buzz : R.drawable.icon_sms);
 		
 //		TextView txt_phone = (TextView) rootView.findViewById(R.id.number);
 		ContextMenuItem iv_contact = (ContextMenuItem) rootView.findViewById(R.id.vad_send_message);
 		iv_contact.updateOptionList("请选择", 
 		new String[] {"发送手机短信", "发送即时消息"}, 
 		new int[] {R.id.vad_send_message + 1, R.id.vad_send_message + 2});
 		//FIXME: prepare context menu for currnet vad.
 		rootView.findViewById(R.id.vad_buzz_btn).setOnClickListener(this);
 //		View iv_buzz = rootView.findViewById(R.id.buzz);
 		String mobileV = detail.getValueByKey(GoodsDetail.EDATAKEYS.EDATAKEYS_CONTACT);
 		rl_phone.setVisibility(View.VISIBLE);
 		
 //		iv_buzz.setOnClickListener(this);
 		
 		if (mobileV != null
 				&& !mobileV.equals("")
 				&& !mobileV.equals("无")) {
 //			txt_phone.setVisibility(View.VISIBLE);
 //			txt_phone.setText(mobileV);
 //			iv_contact.updateOptionList(mobileV, 
 //					new String[] {"拨打电话", "发送短信","保存到联系人", "取消"}, 
 //					new int[] {R.id.contact + 1, R.id.contact + 2, R.id.contact, R.id.contact + 4});
 //			txt_phone.setOnClickListener(this);
 //			iv_contact.assignContactFromPhone(mobileV, false);
 			rootView.findViewById(R.id.vad_call_btn).setVisibility(View.VISIBLE);
 			rootView.findViewById(R.id.vad_call_btn).setOnClickListener(this);
 	
 		} else {
 //			txt_phone.setText("无手机号码");
 //			txt_phone.setOnClickListener(null);
 //			txt_phone.setVisibility(View.INVISIBLE);
 			rootView.findViewById(R.id.vad_call_btn).setVisibility(View.INVISIBLE);
 			rootView.findViewById(R.id.vad_call_btn).setOnClickListener(null);
 		}
 	}
 	
 	private boolean isCurrentAdFromMobile()
 	{
 		if (detail == null)
 		{
 			return false;
 		}
 		
 		String postFrom = detail.getValueByKey("postMethod");
 		
 		return "api_mobile_android".equals(postFrom) || "baixing_ios".equalsIgnoreCase(postFrom);
 	}
 	
 	private void requireAuth4Talk()
 	{
 //		if(null != m_viewInfoListener){
 //			m_viewInfoListener.onNewView(new LoginView(getContext(), "返回"));
 			Bundle bundle = createArguments(null, "返回");
 			pushFragment(new LoginFragment(), bundle);
 			if (authCtrl != null)
 			{
 				authCtrl.cancelAuth();
 			}
 			
 			authCtrl = new AuthController();
 			authCtrl.startWaitingAuth(new Runnable() {
 				public void run() {
 					startChat();
 				}
 				
 			}, null);
 			
 //		}
 	}
 	
 	private void startChat()
 	{
 		if (/*m_viewInfoListener != null &&*/ this.detail != null)
 		{
 			Bundle bundle = new Bundle();
 //			bundle.putString("senderId", myUserId);
 			bundle.putString("receiverId", detail.getValueByKey("userId"));
 			bundle.putString("adId", detail.getValueByKey("id"));
 			bundle.putString("adTitle", detail.getValueByKey(GoodsDetail.EDATAKEYS.EDATAKEYS_TITLE));
 			
 			pushFragment(new TalkFragment(), bundle);
 		}
 	}
 	
 	
 //	private int btnStatus = -1;//-1:strCollect, 0: strCancelCollect, 1:strManager
 	
 	private boolean handleRightBtnIfInVerify(){
 		if(!detail.getValueByKey("status").equals("0")){
 			showSimpleProgress();
 			new Thread(new RequestThread(REQUEST_TYPE.REQUEST_TYPE_DELETE)).start();
 
 			return true;	
 		}
 		return false;
 	}
 	
 	private void handleStoreBtnClicked(){
 		if(handleRightBtnIfInVerify()) return;
 		if(/*-1 == btnStatus*/!isInMyStore()){
 //			btnStatus = 0;
 			List<GoodsDetail> myStore = QuanleimuApplication.getApplication().getListMyStore();
 			
 //			TitleDef title = getTitleDef();
 //			title.m_rightActionHint = strCancelCollect;
 //			m_viewInfoListener.onTitleChanged(title);
 //			refreshHeader();
 			
 			if (myStore == null){
 				myStore = new ArrayList<GoodsDetail>();
 				myStore.add(0, detail);
 			} else {
 				if (myStore.size() >= 100) {
 					myStore.remove(0);
 				}
 				myStore.add(0, detail);
 			}		
 			QuanleimuApplication.getApplication().setListMyStore(myStore);
 			Helper.saveDataToLocate(QuanleimuApplication.context, "listMyStore", myStore);
 			updateTitleBar(getTitleDef());
 			Toast.makeText(QuanleimuApplication.context, "收藏成功", 3).show();
 		}
 		else /*if (0 == btnStatus)*/ {
 //			btnStatus = -1;
 			List<GoodsDetail> myStore = QuanleimuApplication.getApplication().getListMyStore();
 			for (int i = 0; i < myStore.size(); i++) {
 				if (detail.getValueByKey(GoodsDetail.EDATAKEYS.EDATAKEYS_ID)
 						.equals(myStore.get(i).getValueByKey(GoodsDetail.EDATAKEYS.EDATAKEYS_ID))) {
 					myStore.remove(i);
 					break;
 				}
 			}
 			QuanleimuApplication.getApplication().setListMyStore(myStore);
 			Helper.saveDataToLocate(this.getAppContext(), "listMyStore", myStore);
 //			TitleDef title = getTitleDef();
 //			title.m_rightActionHint = strCollect;
 //			refreshHeader();
 			updateTitleBar(getTitleDef());
 			Toast.makeText(this.getActivity(), "取消收藏", 3).show();
 		}
 //		else if(1 == btnStatus){
 //			final String[] names = {"编辑","刷新","删除"};
 //			new AlertDialog.Builder(this.getActivity()).setTitle("选择操作")
 //					.setItems(names, new DialogInterface.OnClickListener(){
 //						public void onClick(DialogInterface dialog, int which){
 //							switch(which){
 //								case 0:
 //									Bundle bundle = createArguments(null, null);
 //									bundle.putSerializable("goodsDetail", detail);
 //									bundle.putString("cateNames", detail.getValueByKey(GoodsDetail.EDATAKEYS.EDATAKEYS_CATEGORYENGLISHNAME));
 //									pushFragment(new PostGoodsFragment(), bundle);
 //									
 //									break;
 //								case 1:
 //									showSimpleProgress();
 //									new Thread(new RequestThread(REQUEST_TYPE.REQUEST_TYPE_REFRESH)).start();
 //									dialog.dismiss();
 //									break;									
 //								case 2:
 //									showSimpleProgress();
 //									new Thread(new RequestThread(REQUEST_TYPE.REQUEST_TYPE_DELETE)).start();
 //									dialog.dismiss();
 //									break;
 //								default:
 //									break;
 //							}
 //						}
 //					})
 //					.setNegativeButton(
 //				     "取消", new DialogInterface.OnClickListener() {
 //						@Override
 //						public void onClick(DialogInterface dialog, int which) {
 //							dialog.cancel();							
 //						}
 //					}).show();
 //		}
 	}
 	
 	@Override
 	public void handleRightAction(){
 		handleStoreBtnClicked();
 	}
 	
 	class ManagerAlertDialog extends AlertDialog{
 		public ManagerAlertDialog(Context context, int theme){
 			super(context, theme);
 		}
 	}
 
 	@Override
 	public void onClick(View v) {
 		View rootView = getView();
 		switch (v.getId()) {
 //		case R.id.number:{
 //			rootView.findViewById(R.id.contact).performLongClick();
 //			break;
 //		}
 		case R.id.btn_fav_unfav:
 			handleStoreBtnClicked();
 			break;
 		case R.id.vad_call_btn:
 		{
 			startContact(false);
 			break;
 		}
 		case R.id.retry_load_more:
 			retryLoadMore();
 			break;
 		case R.id.appealbutton:
 			Bundle bundle = createArguments(null, null);
 			bundle.putInt("type", 1);
 			bundle.putString("adId", detail.getValueByKey(EDATAKEYS.EDATAKEYS_ID));
 			pushFragment(new FeedbackFragment(), bundle);
 			break;
 		case R.id.vad_buzz_btn:
 			if (isCurrentAdFromMobile())
 			{
 				getView().findViewById(R.id.vad_send_message).performLongClick();
 			}
 			else
 			{
 				startContact(true);
 			}
 			break;
 		/*case R.id.jubaolayout:{
 
 			UserBean user = (UserBean) Util.loadDataFromLocate(this.getActivity(), "user");
 			if(user == null){
 				new AlertDialog.Builder(getActivity())
 				.setMessage("请登陆后举报")
 				.setPositiveButton("现在登陆", new DialogInterface.OnClickListener() {							
 					@Override
 					public void onClick(DialogInterface dialog, int which) {
 						dialog.dismiss();
 						Bundle bundle = createArguments(null, "返回");
 						pushFragment(new LoginFragment(), bundle);
 //						// TODO Auto-generated method stub
 //						if(GoodDetailView.this.m_viewInfoListener != null){
 //							GoodDetailView.this.m_viewInfoListener.onNewView(new LoginView(GoodDetailView.this.getContext(), mBundle));
 //						}						
 					}
 				})
 				.setNegativeButton(
 			     "取消", new DialogInterface.OnClickListener() {
 					@Override
 					public void onClick(DialogInterface dialog, int which) {
 						dialog.cancel();							
 					}
 				})
 			     .show();				
 			}else{
 				Bundle args = createArguments(null, null);
 				args.putInt("type", 0);
 				args.putString("adId", detail.getValueByKey("id"));
 				pushFragment(new FeedbackFragment(), args);
 			}
 			
 			break;
 		}*/
 		case R.id.vad_btn_refresh:{
 			showSimpleProgress();
 			new Thread(new RequestThread(REQUEST_TYPE.REQUEST_TYPE_REFRESH)).start();
 			
 			break;
 		}
 		case R.id.vad_btn_edit:{
 			
 			Bundle args = createArguments(null, null);
 			args.putSerializable("goodsDetail", detail);
 			args.putString("cateNames", detail.getValueByKey(GoodsDetail.EDATAKEYS.EDATAKEYS_CATEGORYENGLISHNAME));
 			this.initCalled = true;
 			pushFragment(new PostGoodsFragment(), args);
 			break;
 		}
 		case R.id.vad_btn_delete:{
 			new AlertDialog.Builder(getActivity()).setTitle("提醒")
 			.setMessage("是否确定删除")
 			.setPositiveButton("确定", new DialogInterface.OnClickListener() {							
 				@Override
 				public void onClick(DialogInterface dialog, int which) {
 					showSimpleProgress();
 					new Thread(new RequestThread(REQUEST_TYPE.REQUEST_TYPE_DELETE)).start();			
 				}
 			})
 			.setNegativeButton(
 		     "取消", new DialogInterface.OnClickListener() {
 				@Override
 				public void onClick(DialogInterface dialog, int which) {
 					dialog.cancel();							
 				}
 			})
 		     .show();
 			break;
 		}
 		}
 	}
 	
 	@Override
 	protected void onFragmentBackWithData(int requestCode, Object result){
 		if(PostGoodsFragment.MSG_POST_SUCCEED == requestCode){
 			this.finishFragment(requestCode, result);
 		}
 	}
 
 	
 	private void setMetaObject(View currentPage, GoodsDetail detail){
 		LinearLayout ll_meta = (LinearLayout) currentPage.findViewById(R.id.meta);
 		if(ll_meta == null) return;
 		ll_meta.removeAllViews();
 		
 		LayoutInflater inflater = LayoutInflater.from(currentPage.getContext());
 		
 		String price = detail.getValueByKey(EDATAKEYS.EDATAKEYS_PRICE);
 		if (price != null && !"".equals(price))
 		{
 			ll_meta.addView(createMetaView(inflater, "价格:", price, null));
 		}
 		
 		
 		
 		String area = detail.getValueByKey(EDATAKEYS.EDATAKEYS_AREANAME);
 		if (area != null)
 		{
 			View areaV = createMetaView(inflater, "地区:", area, new View.OnClickListener() {
 				public void onClick(View v) {
 					showMap();
 				}
 			});
 			ll_meta.addView(areaV);
 		}
 		
 		final String contact = detail.getValueByKey(EDATAKEYS.EDATAKEYS_CONTACT);
 		if (contact != null)
 		{
 			View contacV = createMetaView(inflater, "联系方式:",  contact, new View.OnClickListener() {
 				
 				@Override
 				public void onClick(View v) {
 					startContact(false);
 				}
 			});
 			ll_meta.addView(contacV);
 		}
 		
 		ArrayList<String> allMeta = detail.getMetaData();
 		for (String meta : allMeta)
 		{
 			if (!meta.startsWith("价格") &&
 					!meta.startsWith("地区") && !meta.startsWith("查看"))
 			{
 				final int splitIndex = meta.indexOf(" ");
 				if (splitIndex != -1)
 				{
 					ll_meta.addView(createMetaView(inflater, meta.substring(0, splitIndex), meta.substring(splitIndex), null));
 				}
 			}
 		}
 	}
 	
 	private View createMetaView(LayoutInflater inflater, String label, String value, View.OnClickListener clickListener)
 	{
 		View v = inflater.inflate(R.layout.item_meta, null);
 		
 		TextView tvmetatxt = (TextView) v.findViewById(R.id.tvmetatxt);
 		TextView tvmeta = (TextView) v.findViewById(R.id.tvmeta);
 		
 		tvmetatxt.setText(label);
 		tvmeta.setText(value);
 		
 		if (clickListener != null)
 		{
 			v.findViewById(R.id.action_indicator_img).setVisibility(View.VISIBLE);
 			v.setOnClickListener(clickListener);
 		}
 		else
 		{
 			v.findViewById(R.id.action_indicator_img).setVisibility(View.INVISIBLE);
 		}
 		
 		LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) v.getLayoutParams();
 		if (layoutParams== null)  layoutParams =  new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
 		layoutParams.height = (int) getResources().getDimension(R.dimen.vad_meta_item_height);
 		v.setLayoutParams(layoutParams);
 		
 		return v;
 	}
 	
 	
 	@Override
 	public void onAnimationEnd(Animation animation) {
 		// TODO Auto-generated method stub
 		View root = getView();
 		if (root != null)
 		{
 			if (root.findViewById(R.id.btn_prev) != null)
 			{
 				root.findViewById(R.id.btn_prev).setVisibility(View.GONE);
 			}
 			
 			if (root.findViewById(R.id.btn_next) != null)
 			{
 				
 				root.findViewById(R.id.btn_next).setVisibility(View.GONE);
 			}
 		}
 	}
 
 	@Override
 	public void onAnimationRepeat(Animation animation) {
 		// TODO Auto-generated method stub
 		
 	}
 
 	@Override
 	public void onAnimationStart(Animation animation) {
 		// TODO Auto-generated method stub
 		
 	}
 	
 
 	@Override
 	protected void handleMessage(Message msg, Activity activity, View rootView) {
 
 		switch (msg.what) {
 		case MSG_HIDE_ARROW:
 			final View prev = rootView.findViewById(R.id.btn_prev);
 			if(prev != null){
 				Animation animation = AnimationUtils.loadAnimation(activity, R.anim.alpha);
 				prev.startAnimation(animation);
 				animation.setAnimationListener(GoodDetailFragment.this);
 			}
 			View next = rootView.findViewById(R.id.btn_next);
 			if(next != null){
 				Animation animation = AnimationUtils.loadAnimation(activity, R.anim.alpha);
 				next.startAnimation(animation);
 				animation.setAnimationListener(GoodDetailFragment.this);
 			}
 			break;
 		case msgRefresh:
 			if(json == null){
 				Toast.makeText(activity, "刷新失败，请稍后重试！", 0).show();
 				break;
 			}
 			try {
 				JSONObject jb = new JSONObject(json);
 				JSONObject js = jb.getJSONObject("error");
 				String message = js.getString("message");
 				int code = js.getInt("code");
 				if (code == 0) {
 					new Thread(new RequestThread(REQUEST_TYPE.REQUEST_TYPE_UPDATE)).start();
 					Toast.makeText(getActivity(), message, 0).show();
 				}else if(2 == code){
 					hideProgress();
 					new AlertDialog.Builder(getActivity()).setTitle("提醒")
 					.setMessage(message)
 					.setPositiveButton("确定", new DialogInterface.OnClickListener() {							
 						@Override
 						public void onClick(DialogInterface dialog, int which) {
 							showSimpleProgress();
 							new Thread(new RequestThread(REQUEST_TYPE.REQUEST_TYPE_REFRESH, 1)).start();
 							dialog.dismiss();
 						}
 					})
 					.setNegativeButton(
 				     "取消", new DialogInterface.OnClickListener() {
 						@Override
 						public void onClick(DialogInterface dialog, int which) {
 							dialog.cancel();							
 						}
 					})
 				     .show();
 
 				}else {
 					hideProgress();
 					Toast.makeText(getActivity(), message, 0).show();
 				}
 			} catch (JSONException e) {
 				// TODO Auto-generated catch block
 				e.printStackTrace();
 			}
 			break;			
 		case msgUpdate:
 			hideProgress();
 			GoodsList goods = JsonUtil.getGoodsListFromJson(json);
 			List<GoodsDetail> goodsDetails = goods.getData();
 			if(goodsDetails != null && goodsDetails.size() > 0){
 				for(int i = 0; i < goodsDetails.size(); ++ i){
 					if(goodsDetails.get(i).getValueByKey(GoodsDetail.EDATAKEYS.EDATAKEYS_ID)
 							.equals(detail.getValueByKey(GoodsDetail.EDATAKEYS.EDATAKEYS_ID))){
 						detail = goodsDetails.get(i);
 						break;
 					}
 				}
 				List<GoodsDetail>listMyPost = QuanleimuApplication.getApplication().getListMyPost();
 				if(listMyPost != null){
 					for(int i = 0; i < listMyPost.size(); ++ i){
 						if(listMyPost.get(i).getValueByKey(GoodsDetail.EDATAKEYS.EDATAKEYS_ID)
 								.equals(detail.getValueByKey(GoodsDetail.EDATAKEYS.EDATAKEYS_ID))){
 							listMyPost.set(i, detail);
 							break;
 						}
 					}
 				}
 				//QuanleimuApplication.getApplication().setListMyPost(listMyPost);
 			}
 
 //			setMetaObject(); FIXME: should update current UI.
 			break;
 		case msgDelete:
 			hideProgress();
 			try {
 				JSONObject jb = new JSONObject(json);
 				JSONObject js = jb.getJSONObject("error");
 				String message = js.getString("message");
 				int code = js.getInt("code");
 				if (code == 0) {
 					if(detail.getValueByKey("status").equals("0")){
 						List<GoodsDetail> listMyPost = QuanleimuApplication.getApplication().getListMyPost();
 						if(null != listMyPost){
 							for(int i = 0; i < listMyPost.size(); ++ i){
 								if(listMyPost.get(i).getValueByKey(GoodsDetail.EDATAKEYS.EDATAKEYS_ID)
 										.equals(detail.getValueByKey(GoodsDetail.EDATAKEYS.EDATAKEYS_ID))){
 									listMyPost.remove(i);
 									break;
 								}
 							}
 						}
 						finishFragment(MSG_MYPOST_DELETED, null);
 					}
 					else{
 						finishFragment(MSG_ADINVERIFY_DELETED, detail.getValueByKey(EDATAKEYS.EDATAKEYS_ID));
 					}
 //					finish();
 					Toast.makeText(activity, message, 0).show();
 				} else {
 					// 删除失败
 					Toast.makeText(activity, "删除失败,请稍后重试！", 0).show();
 				}
 			} catch (JSONException e) {
 				// TODO Auto-generated catch block
 				e.printStackTrace();
 			}
 			break;
 		default:
 			break;
 		}
 	
 	}
 
 	class RequestThread implements Runnable{
 		private REQUEST_TYPE type;
 		private int pay = 0;
 		public RequestThread(REQUEST_TYPE type){
 			this.type = type;
 		}
 		public RequestThread(REQUEST_TYPE type, int pay) {
 			this.type = type;
 			this.pay = pay;
 		}
 		@Override
 		public void run(){
 			synchronized(GoodDetailFragment.this){
 				ArrayList<String> requests = null;
 				String apiName = null;
 				int msgToSend = -1;
 				if(REQUEST_TYPE.REQUEST_TYPE_DELETE == type){
 					requests = doDelete();
 					apiName = "ad_delete";
 					msgToSend = msgDelete;
 				}
 				else if(REQUEST_TYPE.REQUEST_TYPE_REFRESH == type){
 					requests = doRefresh(this.pay);
 					apiName = "ad_refresh";
 					msgToSend = msgRefresh;
 				}
 				else if(REQUEST_TYPE.REQUEST_TYPE_UPDATE == type){
 					requests = doUpdate();
 					apiName = "ad_list";
 					msgToSend = msgUpdate;
 				}
 				if(requests != null){
 					String url = Communication.getApiUrl(apiName, requests);
 					try {
 						json = Communication.getDataByUrl(url, true);
 					} catch (UnsupportedEncodingException e) {
 						QuanleimuApplication.getApplication().getErrorHandler().sendEmptyMessage(ErrorHandler.ERROR_NETWORK_UNAVAILABLE);
 						hideProgress();
 					} catch (IOException e) {
 						QuanleimuApplication.getApplication().getErrorHandler().sendEmptyMessage(ErrorHandler.ERROR_NETWORK_UNAVAILABLE);
 						hideProgress();
 					} catch (Communication.BXHttpException e){
 						
 					}
 //					myHandler.sendEmptyMessage(msgToSend);
 					sendMessage(msgToSend, null);
 				}
 			}
 		}
 	}
 	
 	private ArrayList<String> doRefresh(int pay){
 		json = "";
 		ArrayList<String> list = new ArrayList<String>();
 
 		UserBean user = (UserBean) Util.loadDataFromLocate(this.getActivity(), "user");
		if(user != null){
 			String mobile = user.getPhone();
 			String password = user.getPassword();
 	
 			list.add("mobile=" + mobile);
 			String password1 = Communication.getMD5(password);
 			password1 += Communication.apiSecret;
 			String userToken = Communication.getMD5(password1);
 			list.add("userToken=" + userToken);
 		}
 		list.add("adId=" + detail.getValueByKey(GoodsDetail.EDATAKEYS.EDATAKEYS_ID));
 		list.add("rt=1");
 		if(pay != 0){
 			list.add("pay=1");
 		}
 
 		return list;
 	}
 	
 	private ArrayList<String> doUpdate(){
 		json = "";
 		ArrayList<String> list = new ArrayList<String>();
 		
 		UserBean user = (UserBean) Util.loadDataFromLocate(this.getActivity(), "user");
		if(user != null){
 			String mobile = user.getPhone();
 			String password = user.getPassword();
 	
 			list.add("mobile=" + mobile);
 			String password1 = Communication.getMD5(password);
 			password1 += Communication.apiSecret;
 			String userToken = Communication.getMD5(password1);
 			list.add("userToken=" + userToken);
 		}
 		list.add("query=id:" + detail.getValueByKey(GoodsDetail.EDATAKEYS.EDATAKEYS_ID));
 		list.add("rt=1");
 		return list;		
 	}
 	
 	private ArrayList<String> doDelete(){
 		// TODO Auto-generated method stub
 		json = "";
 		ArrayList<String> list = new ArrayList<String>();
 
 		UserBean user = (UserBean) Util.loadDataFromLocate(this.getAppContext(), "user");
		if(user != null){
 			String mobile = user.getPhone();
 			String password = user.getPassword();
 			list.add("mobile=" + mobile);
 			String password1 = Communication.getMD5(password);
 			password1 += Communication.apiSecret;
 			String userToken = Communication.getMD5(password1);
 			list.add("userToken=" + userToken);
 		}
 		list.add("adId=" + detail.getValueByKey(GoodsDetail.EDATAKEYS.EDATAKEYS_ID));
 		list.add("rt=1");
 		
 		return list;		
 	}
 	
 	private HashMap<String, List<Integer> > imageMap = new HashMap<String, List<Integer> >();
 	private void increaseImageCount(String url, int pos){
 		if(url == null) return;
 //		Log.d("imagecount", "imagecount, increase:  " + url);
 		if(imageMap.containsKey(url)){
 			List<Integer> values = imageMap.get(url);
 			for(int i = 0; i < values.size(); ++ i){
 				if(values.get(i) ==  pos){
 					return;
 				}
 			}
 			values.add(pos);
 			imageMap.put(url, values);
 		}else{
 			List<Integer> value = new ArrayList<Integer>();
 			value.add(pos);
 			imageMap.put(url, value);
 		}
 	}
 	
 	private void decreaseImageCount(String url, int pos){
 		if(url == null) return;
 //		Log.d("imagecount", "imagecount, decrease:  " + url);
 		if(imageMap.containsKey(url)){
 			List<Integer> values = imageMap.get(url);
 			for(int i = 0; i < values.size(); ++ i){
 				if(values.get(i) == pos){
 					values.remove(i);
 					break;
 				}
 			}
 			if(values.size() == 0){
 				QuanleimuApplication.getImageLoader().forceRecycle(url);
 				imageMap.remove(url);
 //				Log.d("remove", "imagecount    do remove");
 			}else{
 				imageMap.put(url, values);
 //				Log.d("0, remove", "imagecount not 0, no remove~~~~:   " + values.size());
 			}
 		}
 	}	
 
 	class MainAdapter extends BaseAdapter {
 		Context context;
 		List<String> listUrl;
 		private int position;
 		public MainAdapter(Context context, List<String> listUrl, int detailPostion) {
 			this.context = context;
 			this.listUrl = listUrl;
 			position = detailPostion;
 		}
 
 		@Override
 		public int getCount() {
 			return listUrl.size();
 		}
 		
 		public List<String> getImages()
 		{
 			return listUrl;
 		}
 
 		@Override
 		public Object getItem(int arg0) {
 			return listUrl.get(arg0);
 		}
 
 		@Override
 		public long getItemId(int position) {
 			return 0;
 		}
 
 		@Override
 		public View getView(int position, View convertView, ViewGroup parent) {
 			LayoutInflater inflater = LayoutInflater.from(context);
 			View v = null;
 //			if (convertView != null) {
 //				v = (ImageView) convertView;
 //			} else {
 				v = inflater.inflate(R.layout.item_detailview, null);
 //			}
 			ImageView iv = (ImageView) v.findViewById(R.id.ivGoods);
 			
 			if (type <= 240) {
 				iv.setLayoutParams(new Gallery.LayoutParams(86, 86));
 			} else if (type <= 320) {
 				iv.setLayoutParams(new Gallery.LayoutParams(145, 145));
 			} else if (type <= 480) {
 				iv.setLayoutParams(new Gallery.LayoutParams(210, 210));
 			} else if (type <= 540) {
 				iv.setLayoutParams(new Gallery.LayoutParams(235, 235));
 			} else if (type <= 640) {
 				iv.setLayoutParams(new Gallery.LayoutParams(240, 240));
 			}else{
 				iv.setLayoutParams(new Gallery.LayoutParams(245,245));
 			}
 
 			iv.setImageBitmap(mb_loading);
 			
 			if (listUrl.size() != 0 && listUrl.get(position) != null) {
 				String prevTag = (String)iv.getTag();
 				iv.setTag(listUrl.get(position));
 				SimpleImageLoader.showImg(iv, listUrl.get(position), prevTag, getActivity());
 				increaseImageCount(listUrl.get(position), this.position);
 			}
 			
 			//Log.d("GoodDetailView: ", "getView for position-" + position);
 			
 			return iv;
 		}
 	}
 	
 	@Override
 	public void initTitle(TitleDef title){
 		title.m_leftActionHint = "返回";
 		title.m_rightActionHint = "";//detail.getValueByKey("status").equals("0") ? "收藏" : null;
 		title.m_title = ( this.mListLoader.getSelection() + 1 ) + "/" + 
 				this.mListLoader.getGoodsList().getData().size();
 		title.m_visible = true;
 		
 		LayoutInflater inflater = LayoutInflater.from(this.getActivity());
 		title.m_titleControls = title.m_titleControls = inflater.inflate(R.layout.vad_title, null); 
 		
 		updateTitleBar(title);
 	}
 	
 	private void updateTitleBar(TitleDef title)
 	{
 		
 		if(isMyAd() || !isValidMessage()){
 			title.m_titleControls.findViewById(R.id.vad_title_fav_parent).setVisibility(View.GONE);
 		}
 		else{
 			title.m_titleControls.findViewById(R.id.vad_title_fav_parent).setVisibility(View.VISIBLE);
 		}
 		
 		ImageView favBtn = (ImageView) title.m_titleControls.findViewById(R.id.btn_fav_unfav);
 		if (favBtn != null)
 		{
 			favBtn.setOnClickListener(this);
 			favBtn.setImageResource(isInMyStore() ? R.drawable.icon_unfav : R.drawable.icon_fav);
 		}
 		
 		TextView createTimeView = (TextView) title.m_titleControls.findViewById(R.id.vad_create_time);
 		String dateV = detail.getValueByKey(EDATAKEYS.EDATAKEYS_DATE);
 		if (dateV != null)
 		{
 			try {
 				long timeL = Long.parseLong(dateV) * 1000;
 				createTimeView.setText(TextUtil.getTimeDesc(timeL));
 			}
 			catch(Throwable t)
 			{
 				createTimeView.setText("");
 			}
 		}
 		
 		TextView viewTimes = (TextView) getTitleDef().m_titleControls.findViewById(R.id.vad_viewed_time);
 		viewTimes.setText(detail.getValueByKey("count") + "次查看");
 	}
 	
 	@Override
 	public void initTab(TabDef tab){
 		tab.m_visible = false;
 	}
 	
 	
     @Override
     public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
     {
     	if (parent.getAdapter() instanceof MainAdapter)
     	{
     		MainAdapter mainAdapter = (MainAdapter) parent.getAdapter();
     		
     		List<String> listUrl = mainAdapter.getImages();
     		ArrayList<String> urls = new ArrayList<String>();
     		urls.add(listUrl.get(position));
     		for(int index = 0; (index + position < listUrl.size() || position - index >= 0); ++index){
     			if(index + position < listUrl.size())
     				urls.add(listUrl.get(index+position));
     			
     			if(position - index >= 0)
     				urls.add(listUrl.get(position-index));				
     		}
     		SimpleImageLoader.AdjustPriority(urls);
     	}
     }
 
     @Override
     public void onNothingSelected(AdapterView<?> arg0)
     {
         // TODO Auto-generated method stub
     }	
 
 
 	@Override
 	public void onHasMoreStatusChanged() {
 	}
 	
 	private void onLoadMoreFailed()
 	{
 		final View page = loadingMorePage == null ? null : (View) loadingMorePage.get();
 		if (page != null)
 		{
 			page.findViewById(R.id.retry_load_more).setOnClickListener(GoodDetailFragment.this);
 			page.postDelayed(new Runnable() {
 				@Override
 				public void run() {
 					page.findViewById(R.id.loading_more_progress_parent).setVisibility(View.GONE);
 					page.findViewById(R.id.retry_more_parent).setVisibility(View.VISIBLE);
 					page.findViewById(R.id.llDetail).setVisibility(View.GONE);
 				}
 				
 			}, 10);
 		}
 	}
 	
 	private void retryLoadMore()
 	{
 		//We assume that this action always on UI thread.
 		final View page = loadingMorePage == null ? null : (View) loadingMorePage.get();
 		if (page != null)
 		{
 			page.findViewById(R.id.loading_more_progress_parent).setVisibility(View.VISIBLE);
 			page.findViewById(R.id.retry_more_parent).setVisibility(View.GONE);
 			page.findViewById(R.id.llDetail).setVisibility(View.GONE);
 		}
 		
 		if (null != mHolder) {
 			mHolder.startFecthingMore();
 		} else {
 			mListLoader
 					.startFetching(
 							false,
 							((GoodsListLoader.E_LISTDATA_STATUS.E_LISTDATA_STATUS_ONLINE == mListLoader
 									.getDataStatus()) ? Communication.E_DATA_POLICY.E_DATA_POLICY_NETWORK_CACHEABLE
 									: Communication.E_DATA_POLICY.E_DATA_POLICY_ONLY_LOCAL));
 		}
 	}
 	
 	private void loadMore(View page) {
 		loadingMorePage = new WeakReference(page);
 		
 		retryLoadMore();
 	}
 	
 	public void setListHolder(IListHolder holder)
 	{
 		this.mHolder = holder;
 	}
 	
 	private static List<String> getImageUrls(GoodsDetail goodDetail)
 	{
 		List<String> listUrl = null;
 		
 		if (goodDetail.getImageList() != null)
 		{
 			listUrl = new ArrayList<String>();
 			String b = (goodDetail.getImageList().getResize180());//.substring(1, (goodDetail.getImageList().getResize180()).length()-1);
 			if(b == null) return listUrl;
 			b = Communication.replace(b);
 			String[] c = b.split(",");
 			for(int i=0;i<c.length;i++) 
 			{
 				listUrl.add(c[i]);
 			}
 		}
 		
 		return listUrl;
 	}
 	
 	@Override
 	public boolean onContextItemSelected(MenuItem menuItem) {
 		
 		View v = getView();
 		switch (menuItem.getItemId())
 		{
 		case R.id.vad_send_message + 1: {
 			startContact(true);
 			return true;
 		}
 		case R.id.vad_send_message + 2: {
 			startChat();
 			return true;
 		}
 		case R.id.vad_send_message + 3:
 			return true;
 		}
 		
 		return super.onContextItemSelected(menuItem);
 	}
 	
 	private void startContact(boolean sms)
 	{
 		BXStatsHelper.getInstance().increase(sms ? BXStatsHelper.TYPE_SMS_SEND : BXStatsHelper.TYPE_CALL, null);
 		String contact = detail.getValueByKey(EDATAKEYS.EDATAKEYS_CONTACT);
 		if (contact != null)
 		{
 			Intent intent = new Intent(
 					sms ? Intent.ACTION_SENDTO : Intent.ACTION_DIAL,
 					Uri.parse((sms ? "smsto:" : "tel:") + contact));
 			startActivity(intent);
 		}
 	}
 	
 	private void showMap()
 	{
 		if (detail == null)
 		{
 			return;
 		}
 		final GoodsDetail requestDetail = this.detail;
 		
 		String latV = requestDetail.getValueByKey(GoodsDetail.EDATAKEYS.EDATAKEYS_LAT);
 		String lonV = requestDetail.getValueByKey(GoodsDetail.EDATAKEYS.EDATAKEYS_LON);
 		if(latV != null && !latV.equals("false") && !latV.equals("") && lonV != null && !lonV.equals("false") && !lonV.equals(""))
 		{
 			final double lat = Double.valueOf(latV);
 			final double lon = Double.valueOf(lonV);
 			Thread convertThread = new Thread(new Runnable(){
 				@Override
 				public void run(){
 					String baiduUrl = String.format("http://api.map.baidu.com/ag/coord/convert?from=2&to=4&x=%s&y=%s", 
 							String.valueOf(lat), String.valueOf(lon));
 					try{
 						String baiduJsn = Communication.getDataByUrlGet(baiduUrl);
 						JSONObject js = new JSONObject(baiduJsn);
 						Object errorCode = js.get("error");
 						if(errorCode instanceof Integer && (Integer)errorCode == 0){
 							String x = (String)js.get("x");
 							String y = (String)js.get("y");
 							byte[] bytes = Base64.decode(x, Base64.DEFAULT);
 							x = new String(bytes, "UTF-8");
 							
 							bytes = Base64.decode(y, Base64.DEFAULT);
 							y = new String(bytes, "UTF-8");
 							
 							Double dx = Double.valueOf(x);
 							Double dy = Double.valueOf(y);
 							
 							int ix = (int)(dx * 1E6);
 							int iy = (int)(dy * 1E6);
 							
 							x = String.valueOf(ix);
 							y = String.valueOf(iy);
 							
 							Bundle bundle = new Bundle();
 							bundle.putString("detailPosition", x +"," + y);
 							String areaname = requestDetail.getValueByKey(EDATAKEYS.EDATAKEYS_AREANAME);
 							if(areaname != null){
 								String[] aryArea = areaname.split(",");
 								if(aryArea != null && aryArea.length > 0){
 									bundle.putString("title", aryArea[aryArea.length - 1]);
 								}
 							}
 							
 							final BaseActivity baseActivity = (BaseActivity)getActivity();
 							if (baseActivity != null && requestDetail == detail)
 							{
 								baseActivity.getIntent().putExtras(bundle);
 								
 								baseActivity.getIntent().setClass(baseActivity, BaiduMapActivity.class);
 								baseActivity.startActivity(baseActivity.getIntent());
 							}
 							return;
 						}
 
 					}catch(UnsupportedEncodingException e){
 						e.printStackTrace();
 					}catch(Exception e){
 						e.printStackTrace();
 					}
 					String positions = Integer.toString((int)(lat*1E6)) + "," + Integer.toString((int)(lon*1E6));
 					Bundle bundle = new Bundle();
 					bundle.putString("detailPosition", positions);
 					bundle.putString("title", requestDetail.getValueByKey(EDATAKEYS.EDATAKEYS_AREANAME));
 					
 					final BaseActivity baseActivity = (BaseActivity)getActivity();
 					if (baseActivity != null && requestDetail == detail)
 					{
 						baseActivity.getIntent().putExtras(bundle);
 						
 						baseActivity.getIntent().setClass(baseActivity, BaiduMapActivity.class);
 						baseActivity.startActivity(baseActivity.getIntent());
 					}
 				}
 			});
 			convertThread.start();
 		}
 		else{
 			Thread getCoordinate = new Thread(new Runnable(){
 	            @Override
 	            public void run() {
 	            	if(getActivity() == null) return;
 					String city = QuanleimuApplication.getApplication().cityName;
 					if(!city.equals("")){
 						String googleUrl = String.format("http://maps.google.com/maps/geo?q=%s&output=csv", city);
 						try{
 							String googleJsn = Communication.getDataByUrlGet(googleUrl);
 							String[] info = googleJsn.split(",");
 							if(info != null && info.length == 4){
 								String positions = 
 										Integer.toString((int)(Double.parseDouble(info[2]) * 1E6))
 										+ "," + Integer.toString((int)(Double.parseDouble(info[3]) * 1E6));
 								Bundle bundle = new Bundle();
 								bundle.putString("detailPosition", positions);
 								bundle.putString("title", requestDetail.getValueByKey(EDATAKEYS.EDATAKEYS_AREANAME));
 
 								final BaseActivity baseActivity = (BaseActivity)getActivity();
 								if (baseActivity != null && requestDetail == detail)
 								{
 									baseActivity.getIntent().putExtras(bundle);
 									
 									baseActivity.getIntent().setClass(baseActivity, BaiduMapActivity.class);
 									baseActivity.startActivity(baseActivity.getIntent());
 								}
 							}
 						}catch(UnsupportedEncodingException e){
 							e.printStackTrace();
 						}catch(Exception e){
 							e.printStackTrace();
 						}
 					}	
 	            }
 			});
 			getCoordinate.start();
 
 		}
 	}
 	
 	
 }
