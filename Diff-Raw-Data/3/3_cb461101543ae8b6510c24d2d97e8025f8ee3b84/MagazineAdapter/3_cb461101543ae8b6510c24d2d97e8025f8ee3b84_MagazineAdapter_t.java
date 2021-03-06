 package com.librelio.lib.adapter;
 
 import java.io.File;
 import java.text.BreakIterator;
 import java.util.ArrayList;
 
 import android.app.Activity;
 import android.content.BroadcastReceiver;
 import android.content.Context;
 import android.content.Intent;
 import android.graphics.BitmapFactory;
 import android.os.Handler;
 import android.util.Log;
 import android.view.LayoutInflater;
 import android.view.View;
 import android.view.View.OnClickListener;
 import android.view.ViewGroup;
 import android.widget.BaseAdapter;
 import android.widget.Button;
 import android.widget.ImageView;
 import android.widget.TextView;
 
 import com.librelio.lib.LibrelioApplication;
 import com.librelio.lib.model.MagazineModel;
 import com.librelio.lib.ui.BillingActivity;
 import com.librelio.lib.ui.DownloadActivity;
 import com.librelio.lib.ui.MainMagazineActivity;
 import com.librelio.lib.utils.BillingService;
 import com.librelio.lib.utils.BillingService.RequestPurchase;
 import com.librelio.lib.utils.BillingService.RestoreTransactions;
 import com.librelio.lib.utils.Consts.PurchaseState;
 import com.librelio.lib.utils.Consts.ResponseCode;
 import com.librelio.lib.utils.PurchaseObserver;
 import com.niveales.wind.R;
 
 public class MagazineAdapter extends BaseAdapter{
 	private static final String TAG = "MagazineAdapter";
 	private Context context;
 	private ArrayList<MagazineModel> magazine;
 
 	
 	public MagazineAdapter(ArrayList<MagazineModel> magazine,Context context){
 		this.context = context;
 		this.magazine = magazine;
 	}
 	
 	/*private class MagazineObserver extends PurchaseObserver{
 
 		public MagazineObserver(Activity activity, Handler handler) {
 			super(activity, handler);
 		}
 
 		@Override
 		public void onBillingSupported(boolean supported, String type) {
 			Log.d(TAG,"onBillingSupported");
 		}
 
 		@Override
 		public void onPurchaseStateChange(PurchaseState purchaseState,
 				String itemId, int quantity, long purchaseTime,
 				String developerPayload) {
 			Log.d(TAG,"onPurchaseStateChange");
 		}
 
 		@Override
 		public void onRequestPurchaseResponse(RequestPurchase request,
 				ResponseCode responseCode) {
 			Log.d(TAG,"onRequestPurchaseResponse");
 		}
 
 		@Override
 		public void onRestoreTransactionsResponse(RestoreTransactions request,
 				ResponseCode responseCode) {
 			Log.d(TAG,"onRestoreTransactionsResponse");
 		}
 		
 	}*/
 	
 	@Override
 	public int getCount() {
 		return magazine.size();
 	}
 
 	@Override
 	public Object getItem(int position) {
 		return position;
 	}
 
 	@Override
 	public long getItemId(int position) {
 		return position;
 	}
 
 	@Override
 	public View getView(int position, View convertView, ViewGroup parent) {
 		View res;
 		final MagazineModel currentMagazine = magazine.get(position);
 		
 		if(convertView == null){
 			res = LayoutInflater.from(context).inflate(R.layout.magazine_list_item, null);
 		} else {
 			res = convertView;
 		}
 		TextView title = (TextView)res.findViewById(R.id.item_title);
 		TextView subtitle = (TextView)res.findViewById(R.id.item_subtitle);
 		ImageView thumbnail = (ImageView)res.findViewById(R.id.item_thumbnail);
 
 		title.setText(currentMagazine.getTitle());
 		subtitle.setText(currentMagazine.getSubtitle());
 		
 		String imagePath = currentMagazine.getPngPath();
 		thumbnail.setImageBitmap(BitmapFactory.decodeFile(imagePath));
 		/**
 		 * downloadOrReadButton - this button can be "Download button" or "Read button"
 		 */
 		Button downloadOrReadButton = (Button)res.findViewById(R.id.item_button_one);
 		/**
 		 * sampleOrDeleteButton - this button can be "Delete button" or "Sample button"
 		 */
 		Button sampleOrDeleteButton = (Button)res.findViewById(R.id.item_button_two);
 
 		if (currentMagazine.isDownloaded()) {
 			// Read case
 			downloadOrReadButton.setText(context.getResources().getString(
 					R.string.read));
 			downloadOrReadButton.setOnClickListener(new OnClickListener() {
 				@Override
 				public void onClick(View v) {
 					LibrelioApplication.startPDFActivity(context,
 							currentMagazine.getPdfPath());
 					/*Intent intent = new Intent(context,
 							DownloadActivity.class);
 					intent.putExtra(DownloadActivity.FILE_NAME_KEY,currentMagazine.getFileName());
 					intent.putExtra(DownloadActivity.FILE_URL_KEY,currentMagazine.getPdfUrl());
 					intent.putExtra(DownloadActivity.FILE_PATH_KEY,currentMagazine.getPdfPath());
 					intent.putExtra(DownloadActivity.PNG_PATH_KEY,currentMagazine.getPngPath());
 					context.startActivity(intent);*/
 				}
 			});
 		} else {
 			// download case
 			downloadOrReadButton.setText(context.getResources().getString(
 					R.string.download));
 			downloadOrReadButton.setOnClickListener(new OnClickListener() {
 				@Override
 				public void onClick(View v) {
 					if(currentMagazine.isPaid()){
 						//Intent intent = new Intent("123");
 						//context.sendBroadcast(intent);
 						Intent intent = new Intent(context,BillingActivity.class);
 						intent.putExtra(DownloadActivity.FILE_NAME_KEY,currentMagazine.getFileName());
 						intent.putExtra(DownloadActivity.TITLE_KEY,currentMagazine.getTitle());
 						intent.putExtra(DownloadActivity.SUBTITLE_KEY,currentMagazine.getSubtitle());
 						context.startActivity(intent);
 					} else {
 						Intent intent = new Intent(context,
 								DownloadActivity.class);
 						intent.putExtra(DownloadActivity.FILE_NAME_KEY,currentMagazine.getFileName());
 						intent.putExtra(DownloadActivity.TITLE_KEY,currentMagazine.getTitle());
 						intent.putExtra(DownloadActivity.SUBTITLE_KEY,currentMagazine.getSubtitle());
 						intent.putExtra(DownloadActivity.IS_SAMPLE_KEY,false);
 						intent.putExtra(DownloadActivity.ORIENTATION_KEY,
 								context.getResources().getConfiguration().orientation);
 						context.startActivity(intent);
 					}
 				}
 			});
 		}
 		//
		sampleOrDeleteButton.setVisibility(View.VISIBLE);
 		if (!currentMagazine.isPaid() && !currentMagazine.isDownloaded()) {
 			sampleOrDeleteButton.setVisibility(View.INVISIBLE);
 		} else if (currentMagazine.isDownloaded()) {
 				// delete case
 				sampleOrDeleteButton.setText(context.getResources().getString(
 						R.string.delete));
 				sampleOrDeleteButton.setOnClickListener(new OnClickListener() {
 					@Override
 					public void onClick(View v) {
 						currentMagazine.delete();
 					}
 				});
 		} else {
 			// Sample case
 			sampleOrDeleteButton.setText(context.getResources().getString(
 					R.string.sample));
 			sampleOrDeleteButton.setOnClickListener(new OnClickListener() {
 				@Override
 				public void onClick(View v) {
 					File sample = new File(currentMagazine.getSamplePath());
 					Log.d(TAG, "test: " + sample.exists() + " "
 							+ currentMagazine.isSampleDownloaded());
 					if (currentMagazine.isSampleDownloaded()) {
 						LibrelioApplication.startPDFActivity(context,
 								currentMagazine.getSamplePath());
 					} else {
 						Intent intent = new Intent(context,
 								DownloadActivity.class);
 						intent.putExtra(DownloadActivity.FILE_NAME_KEY,currentMagazine.getFileName());
 						intent.putExtra(DownloadActivity.TITLE_KEY,currentMagazine.getTitle());
 						intent.putExtra(DownloadActivity.SUBTITLE_KEY,currentMagazine.getSubtitle());
 						intent.putExtra(DownloadActivity.ORIENTATION_KEY,
 								context.getResources().getConfiguration().orientation);
 						intent.putExtra(DownloadActivity.IS_SAMPLE_KEY,true);
 						context.startActivity(intent);
 					}
 				}
 			});
 		}
 
 		return res;
 	}
 
 }
