 /*
  * Copyright (C) 2008 Esmertec AG.
  * Copyright (C) 2008 The Android Open Source Project
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *      http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 package com.android.mms.ui;
 
 import java.util.Map;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
 import android.app.AlertDialog;
 import android.content.Context;
 import android.content.DialogInterface;
 import android.content.Intent;
 import android.content.res.Resources;
 import android.graphics.Bitmap;
 import android.graphics.BitmapFactory;
 import android.graphics.Canvas;
 import android.graphics.Paint;
 import android.graphics.Path;
 import android.graphics.Typeface;
 import android.graphics.Paint.FontMetricsInt;
 import android.graphics.drawable.Drawable;
 import android.net.Uri;
 import android.os.Handler;
 import android.os.Message;
 import android.provider.Browser;
 import android.provider.ContactsContract.Profile;
 import android.provider.Telephony.Mms;
 import android.provider.Telephony.Sms;
 import android.telephony.PhoneNumberUtils;
 import android.text.Html;
 import android.text.SpannableStringBuilder;
 import android.text.TextUtils;
 import android.text.method.HideReturnsTransformationMethod;
 import android.text.style.ForegroundColorSpan;
 import android.text.style.LineHeightSpan;
 import android.text.style.StyleSpan;
 import android.text.style.TextAppearanceSpan;
 import android.text.style.URLSpan;
 import android.util.AttributeSet;
 import android.util.Log;
 import android.view.Gravity;
 import android.view.View;
 import android.view.ViewGroup;
 import android.view.View.OnClickListener;
 import android.widget.ArrayAdapter;
 import android.widget.Button;
 import android.widget.ImageButton;
 import android.widget.ImageView;
 import android.widget.LinearLayout;
 import android.widget.QuickContactBadge;
 import android.widget.RelativeLayout;
 import android.widget.TextView;
 
 import com.android.mms.MmsApp;
 import com.android.mms.R;
 import com.android.mms.data.Contact;
 import com.android.mms.data.WorkingMessage;
 import com.android.mms.transaction.Transaction;
 import com.android.mms.transaction.TransactionBundle;
 import com.android.mms.transaction.TransactionService;
 import com.android.mms.util.DownloadManager;
 import com.android.mms.util.SmileyParser;
 import com.google.android.mms.ContentType;
 import com.google.android.mms.pdu.PduHeaders;
 
 /**
  * This class provides view of a message in the messages list.
  */
 public class MessageListItem extends LinearLayout implements
         SlideViewInterface, OnClickListener {
     public static final String EXTRA_URLS = "com.android.mms.ExtraUrls";
 
     private static final String TAG = "MessageListItem";
     private static final StyleSpan STYLE_BOLD = new StyleSpan(Typeface.BOLD);
 
     static final int MSG_LIST_EDIT_MMS   = 1;
     static final int MSG_LIST_EDIT_SMS   = 2;
 
     private View mMmsView;
     private ImageView mImageView;
     private ImageView mLockedIndicator;
     private ImageView mDeliveredIndicator;
     private ImageView mDetailsIndicator;
     private ImageButton mSlideShowButton;
     private TextView mBodyTextView;
     private Button mDownloadButton;
     private TextView mDownloadingLabel;
     private Handler mHandler;
     private MessageItem mMessageItem;
     private String mDefaultCountryIso;
     private TextView mDateView;
     public View mMessageBlock;
     private Path mPath = new Path();
     private Paint mPaint = new Paint();
     private QuickContactDivot mAvatar;
     private boolean mIsLastItemInList;
     static private Drawable sDefaultContactImage;
 
     public MessageListItem(Context context) {
         super(context);
         mDefaultCountryIso = MmsApp.getApplication().getCurrentCountryIso();
 
         if (sDefaultContactImage == null) {
             sDefaultContactImage = context.getResources().getDrawable(R.drawable.ic_contact_picture);
         }
     }
 
     public MessageListItem(Context context, AttributeSet attrs) {
         super(context, attrs);
 
         int color = mContext.getResources().getColor(R.color.timestamp_color);
         mColorSpan = new ForegroundColorSpan(color);
         mDefaultCountryIso = MmsApp.getApplication().getCurrentCountryIso();
 
         if (sDefaultContactImage == null) {
             sDefaultContactImage = context.getResources().getDrawable(R.drawable.ic_contact_picture);
         }
     }
 
     @Override
     protected void onFinishInflate() {
         super.onFinishInflate();
 
         mBodyTextView = (TextView) findViewById(R.id.text_view);
         mDateView = (TextView) findViewById(R.id.date_view);
         mLockedIndicator = (ImageView) findViewById(R.id.locked_indicator);
         mDeliveredIndicator = (ImageView) findViewById(R.id.delivered_indicator);
         mDetailsIndicator = (ImageView) findViewById(R.id.details_indicator);
         mAvatar = (QuickContactDivot) findViewById(R.id.avatar);
         mMessageBlock = findViewById(R.id.message_block);
     }
 
     public void bind(MessageItem msgItem, boolean isLastItem) {
         mMessageItem = msgItem;
         mIsLastItemInList = isLastItem;
 
         setLongClickable(false);
 
         switch (msgItem.mMessageType) {
             case PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND:
                 bindNotifInd(msgItem);
                 break;
             default:
                 bindCommonMessage(msgItem);
                 break;
         }
     }
 
     public MessageItem getMessageItem() {
         return mMessageItem;
     }
 
     public void setMsgListItemHandler(Handler handler) {
         mHandler = handler;
     }
 
     private void bindNotifInd(final MessageItem msgItem) {
         hideMmsViewIfNeeded();
 
         String msgSizeText = mContext.getString(R.string.message_size_label)
                                 + String.valueOf((msgItem.mMessageSize + 1023) / 1024)
                                 + mContext.getString(R.string.kilobyte);
 
         mBodyTextView.setText(formatMessage(msgItem, msgItem.mContact, null, msgItem.mSubject,
                                             msgItem.mHighlight, msgItem.mTextContentType));
 
         mDateView.setText(msgSizeText + " " + msgItem.mTimestamp);
 
         int state = DownloadManager.getInstance().getState(msgItem.mMessageUri);
         switch (state) {
             case DownloadManager.STATE_DOWNLOADING:
                 inflateDownloadControls();
                 mDownloadingLabel.setVisibility(View.VISIBLE);
                 mDownloadButton.setVisibility(View.GONE);
                 break;
             case DownloadManager.STATE_UNSTARTED:
             case DownloadManager.STATE_TRANSIENT_FAILURE:
             case DownloadManager.STATE_PERMANENT_FAILURE:
             default:
                 setLongClickable(true);
                 inflateDownloadControls();
                 mDownloadingLabel.setVisibility(View.GONE);
                 mDownloadButton.setVisibility(View.VISIBLE);
                 mDownloadButton.setOnClickListener(new OnClickListener() {
                     public void onClick(View v) {
                         mDownloadingLabel.setVisibility(View.VISIBLE);
                         mDownloadButton.setVisibility(View.GONE);
                         Intent intent = new Intent(mContext, TransactionService.class);
                         intent.putExtra(TransactionBundle.URI, msgItem.mMessageUri.toString());
                         intent.putExtra(TransactionBundle.TRANSACTION_TYPE,
                                 Transaction.RETRIEVE_TRANSACTION);
                         mContext.startService(intent);
                     }
                 });
                 break;
         }
 
         // Hide the indicators.
         mLockedIndicator.setVisibility(View.GONE);
         mDeliveredIndicator.setVisibility(View.GONE);
         mDetailsIndicator.setVisibility(View.GONE);
     }
 
     private void updateAvatarView(String addr, boolean isSelf) {
         Drawable avatarDrawable;
         if (isSelf || !TextUtils.isEmpty(addr)) {
             Contact contact = isSelf ? Contact.getMe(false) : Contact.get(addr, false);
             avatarDrawable = contact.getAvatar(mContext, sDefaultContactImage);
 
             if (isSelf) {
                 mAvatar.assignContactUri(Profile.CONTENT_URI);
             } else {
                 if (contact.existsInDatabase()) {
                     mAvatar.assignContactUri(contact.getUri());
                 } else {
                     mAvatar.assignContactFromPhone(contact.getNumber(), true);
                 }
             }
         } else {
             avatarDrawable = sDefaultContactImage;
         }
         mAvatar.setImageDrawable(avatarDrawable);
     }
 
     private void bindCommonMessage(final MessageItem msgItem) {
         if (mDownloadButton != null) {
             mDownloadButton.setVisibility(View.GONE);
             mDownloadingLabel.setVisibility(View.GONE);
         }
         // Since the message text should be concatenated with the sender's
         // address(or name), I have to display it here instead of
         // displaying it by the Presenter.
         mBodyTextView.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
 
         boolean isSelf = Sms.isOutgoingFolder(msgItem.mBoxId);
         String addr = isSelf ? null : msgItem.mAddress;
         updateAvatarView(addr, isSelf);
 
         // Get and/or lazily set the formatted message from/on the
         // MessageItem.  Because the MessageItem instances come from a
         // cache (currently of size ~50), the hit rate on avoiding the
         // expensive formatMessage() call is very high.
         CharSequence formattedMessage = msgItem.getCachedFormattedMessage();
         if (formattedMessage == null) {
             formattedMessage = formatMessage(msgItem, msgItem.mContact, msgItem.mBody,
                                              msgItem.mSubject,
                                              msgItem.mHighlight, msgItem.mTextContentType);
         }
         mBodyTextView.setText(formattedMessage);
 
         // If we're in the process of sending a message (i.e. pending), then we show a "SENDING..."
         // string in place of the timestamp.
         mDateView.setText(msgItem.isSending() ?
                 mContext.getResources().getString(R.string.sending_message) :
                     msgItem.mTimestamp);
 
         if (msgItem.isSms()) {
             hideMmsViewIfNeeded();
         } else {
             Presenter presenter = PresenterFactory.getPresenter(
                     "MmsThumbnailPresenter", mContext,
                     this, msgItem.mSlideshow);
             presenter.present();
 
             if (msgItem.mAttachmentType != WorkingMessage.TEXT) {
                 inflateMmsView();
                 mMmsView.setVisibility(View.VISIBLE);
                 setOnClickListener(msgItem);
                 drawPlaybackButton(msgItem);
             } else {
                 hideMmsViewIfNeeded();
             }
         }
         drawRightStatusIndicator(msgItem);
 
         requestLayout();
     }
 
     private void hideMmsViewIfNeeded() {
         if (mMmsView != null) {
             mMmsView.setVisibility(View.GONE);
         }
     }
 
     public void startAudio() {
         // TODO Auto-generated method stub
     }
 
     public void startVideo() {
         // TODO Auto-generated method stub
     }
 
     public void setAudio(Uri audio, String name, Map<String, ?> extras) {
         // TODO Auto-generated method stub
     }
 
     public void setImage(String name, Bitmap bitmap) {
         inflateMmsView();
 
         try {
             if (null == bitmap) {
                 bitmap = BitmapFactory.decodeResource(getResources(),
                         R.drawable.ic_missing_thumbnail_picture);
             }
             mImageView.setImageBitmap(bitmap);
             mImageView.setVisibility(VISIBLE);
         } catch (java.lang.OutOfMemoryError e) {
             Log.e(TAG, "setImage: out of memory: ", e);
         }
     }
 
     private void inflateMmsView() {
         if (mMmsView == null) {
             //inflate the surrounding view_stub
             findViewById(R.id.mms_layout_view_stub).setVisibility(VISIBLE);
 
             mMmsView = findViewById(R.id.mms_view);
             mImageView = (ImageView) findViewById(R.id.image_view);
             mSlideShowButton = (ImageButton) findViewById(R.id.play_slideshow_button);
         }
     }
 
     private void inflateDownloadControls() {
         if (mDownloadButton == null) {
             //inflate the download controls
             findViewById(R.id.mms_downloading_view_stub).setVisibility(VISIBLE);
             mDownloadButton = (Button) findViewById(R.id.btn_download_msg);
             mDownloadingLabel = (TextView) findViewById(R.id.label_downloading);
         }
     }
 
 
     private LineHeightSpan mSpan = new LineHeightSpan() {
         public void chooseHeight(CharSequence text, int start,
                 int end, int spanstartv, int v, FontMetricsInt fm) {
             fm.ascent -= 10;
         }
     };
 
     TextAppearanceSpan mTextSmallSpan =
         new TextAppearanceSpan(mContext, android.R.style.TextAppearance_Small);
 
     ForegroundColorSpan mColorSpan = null;  // set in ctor
 
     private CharSequence formatMessage(MessageItem msgItem, String contact, String body,
                                        String subject, Pattern highlight,
                                        String contentType) {
         SpannableStringBuilder buf = new SpannableStringBuilder();
 
         boolean hasSubject = !TextUtils.isEmpty(subject);
         if (hasSubject) {
             buf.append(mContext.getResources().getString(R.string.inline_subject, subject));
         }
 
         if (!TextUtils.isEmpty(body)) {
             // Converts html to spannable if ContentType is "text/html".
             if (contentType != null && ContentType.TEXT_HTML.equals(contentType)) {
                 buf.append("\n");
                 buf.append(Html.fromHtml(body));
             } else {
                 if (hasSubject) {
                     buf.append(" - ");
                 }
                 SmileyParser parser = SmileyParser.getInstance();
                 buf.append(parser.addSmileySpans(body));
             }
         }
 
         if (highlight != null) {
             Matcher m = highlight.matcher(buf.toString());
             while (m.find()) {
                 buf.setSpan(new StyleSpan(Typeface.BOLD), m.start(), m.end(), 0);
             }
         }
         return buf;
     }
 
     private void drawPlaybackButton(MessageItem msgItem) {
         switch (msgItem.mAttachmentType) {
             case WorkingMessage.SLIDESHOW:
             case WorkingMessage.AUDIO:
             case WorkingMessage.VIDEO:
                 // Show the 'Play' button and bind message info on it.
                 mSlideShowButton.setTag(msgItem);
                 // Set call-back for the 'Play' button.
                 mSlideShowButton.setOnClickListener(this);
                 mSlideShowButton.setVisibility(View.VISIBLE);
                 setLongClickable(true);
 
                 // When we show the mSlideShowButton, this list item's onItemClickListener doesn't
                 // get called. (It gets set in ComposeMessageActivity:
                 // mMsgListView.setOnItemClickListener) Here we explicitly set the item's
                 // onClickListener. It allows the item to respond to embedded html links and at the
                 // same time, allows the slide show play button to work.
                 setOnClickListener(new OnClickListener() {
                     public void onClick(View v) {
                         onMessageListItemClick();
                     }
                 });
                 break;
             default:
                 mSlideShowButton.setVisibility(View.GONE);
                 break;
         }
     }
 
     // OnClick Listener for the playback button
     public void onClick(View v) {
         MessageItem mi = (MessageItem) v.getTag();
         switch (mi.mAttachmentType) {
             case WorkingMessage.VIDEO:
             case WorkingMessage.AUDIO:
             case WorkingMessage.SLIDESHOW:
                 MessageUtils.viewMmsMessageAttachment(mContext, mi.mMessageUri, mi.mSlideshow);
                 break;
         }
     }
 
     public void onMessageListItemClick() {
         URLSpan[] spans = mBodyTextView.getUrls();
 
         if (spans.length == 0) {
             // Do nothing.
         } else if (spans.length == 1) {
             Uri uri = Uri.parse(spans[0].getURL());
             Intent intent = new Intent(Intent.ACTION_VIEW, uri);
             intent.putExtra(Browser.EXTRA_APPLICATION_ID, mContext.getPackageName());
             intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
             mContext.startActivity(intent);
         } else {
             final java.util.ArrayList<String> urls = MessageUtils.extractUris(spans);
 
             ArrayAdapter<String> adapter =
                 new ArrayAdapter<String>(mContext, android.R.layout.select_dialog_item, urls) {
                 public View getView(int position, View convertView, ViewGroup parent) {
                     View v = super.getView(position, convertView, parent);
                     try {
                         String url = getItem(position).toString();
                         TextView tv = (TextView) v;
                         Drawable d = mContext.getPackageManager().getActivityIcon(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                         if (d != null) {
                             d.setBounds(0, 0, d.getIntrinsicHeight(), d.getIntrinsicHeight());
                             tv.setCompoundDrawablePadding(10);
                             tv.setCompoundDrawables(d, null, null, null);
                         }
                         final String telPrefix = "tel:";
                         if (url.startsWith(telPrefix)) {
                             url = PhoneNumberUtils.formatNumber(
                                             url.substring(telPrefix.length()), mDefaultCountryIso);
                         }
                         tv.setText(url);
                     } catch (android.content.pm.PackageManager.NameNotFoundException ex) {
                         ;
                     }
                     return v;
                 }
             };
 
             AlertDialog.Builder b = new AlertDialog.Builder(mContext);
 
             DialogInterface.OnClickListener click = new DialogInterface.OnClickListener() {
                 public final void onClick(DialogInterface dialog, int which) {
                     if (which >= 0) {
                         Uri uri = Uri.parse(urls.get(which));
                         Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                         intent.putExtra(Browser.EXTRA_APPLICATION_ID, mContext.getPackageName());
                         intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                         mContext.startActivity(intent);
                     }
                     dialog.dismiss();
                 }
             };
 
             b.setTitle(R.string.select_link_title);
             b.setCancelable(true);
             b.setAdapter(adapter, click);
 
             b.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                 public final void onClick(DialogInterface dialog, int which) {
                     dialog.dismiss();
                 }
             });
 
             b.show();
         }
     }
 
 
     private void setOnClickListener(final MessageItem msgItem) {
         switch(msgItem.mAttachmentType) {
         case WorkingMessage.IMAGE:
         case WorkingMessage.VIDEO:
             mImageView.setOnClickListener(new OnClickListener() {
                 public void onClick(View v) {
                     MessageUtils.viewMmsMessageAttachment(mContext, null, msgItem.mSlideshow);
                 }
             });
             mImageView.setOnLongClickListener(new OnLongClickListener() {
                 public boolean onLongClick(View v) {
                     return v.showContextMenu();
                 }
             });
             break;
 
         default:
             mImageView.setOnClickListener(null);
             break;
         }
     }
 
     private void setErrorIndicatorClickListener(final MessageItem msgItem) {
         String type = msgItem.mType;
         final int what;
         if (type.equals("sms")) {
             what = MSG_LIST_EDIT_SMS;
         } else {
             what = MSG_LIST_EDIT_MMS;
         }
         mDeliveredIndicator.setOnClickListener(new OnClickListener() {
             public void onClick(View v) {
                 if (null != mHandler) {
                     Message msg = Message.obtain(mHandler, what);
                     msg.obj = new Long(msgItem.mMsgId);
                     msg.sendToTarget();
                 }
             }
         });
     }
 
     private void drawRightStatusIndicator(MessageItem msgItem) {
         // Locked icon
         if (msgItem.mLocked) {
             mLockedIndicator.setImageResource(R.drawable.ic_lock_message_sms);
             mLockedIndicator.setVisibility(View.VISIBLE);
         } else {
             mLockedIndicator.setVisibility(View.GONE);
         }
 
         // Delivery icon
         if (msgItem.isOutgoingMessage() && msgItem.isFailedMessage()) {
             mDeliveredIndicator.setImageResource(R.drawable.ic_list_alert_sms_failed);
             setErrorIndicatorClickListener(msgItem);
             mDeliveredIndicator.setVisibility(View.VISIBLE);
         } else if (msgItem.mDeliveryStatus == MessageItem.DeliveryStatus.FAILED) {
             mDeliveredIndicator.setImageResource(R.drawable.ic_list_alert_sms_failed);
             mDeliveredIndicator.setVisibility(View.VISIBLE);
         } else if (msgItem.mDeliveryStatus == MessageItem.DeliveryStatus.RECEIVED) {
             mDeliveredIndicator.setImageResource(R.drawable.ic_sms_mms_delivered);
             mDeliveredIndicator.setVisibility(View.VISIBLE);
         } else {
             mDeliveredIndicator.setVisibility(View.GONE);
         }
 
         // Message details icon
         if (msgItem.mDeliveryStatus == MessageItem.DeliveryStatus.INFO || msgItem.mReadReport) {
             mDetailsIndicator.setImageResource(R.drawable.ic_sms_mms_details);
             mDetailsIndicator.setVisibility(View.VISIBLE);
         } else {
             mDetailsIndicator.setVisibility(View.GONE);
         }
     }
 
     public void setImageRegionFit(String fit) {
         // TODO Auto-generated method stub
     }
 
     public void setImageVisibility(boolean visible) {
         // TODO Auto-generated method stub
     }
 
     public void setText(String name, String text) {
         // TODO Auto-generated method stub
     }
 
     public void setTextVisibility(boolean visible) {
         // TODO Auto-generated method stub
     }
 
     public void setVideo(String name, Uri video) {
         inflateMmsView();
 
         try {
             Bitmap bitmap = VideoAttachmentView.createVideoThumbnail(mContext, video);
             if (null == bitmap) {
                 bitmap = BitmapFactory.decodeResource(getResources(),
                         R.drawable.ic_missing_thumbnail_video);
             }
             mImageView.setImageBitmap(bitmap);
             mImageView.setVisibility(VISIBLE);
         } catch (java.lang.OutOfMemoryError e) {
             Log.e(TAG, "setVideo: out of memory: ", e);
         }
     }
 
     public void setVideoVisibility(boolean visible) {
         // TODO Auto-generated method stub
     }
 
     public void stopAudio() {
         // TODO Auto-generated method stub
     }
 
     public void stopVideo() {
         // TODO Auto-generated method stub
     }
 
     public void reset() {
         if (mImageView != null) {
             mImageView.setVisibility(GONE);
         }
     }
 
     public void setVisibility(boolean visible) {
         // TODO Auto-generated method stub
     }
 
     public void pauseAudio() {
         // TODO Auto-generated method stub
 
     }
 
     public void pauseVideo() {
         // TODO Auto-generated method stub
 
     }
 
     public void seekAudio(int seekTo) {
         // TODO Auto-generated method stub
 
     }
 
     public void seekVideo(int seekTo) {
         // TODO Auto-generated method stub
 
     }
 
     /**
      * Override dispatchDraw so that we can put our own background and border in.
      * This is all complexity to support a shared border from one item to the next.
      */
     @Override
     public void dispatchDraw(Canvas c) {
         View v = mMessageBlock;
         if (v != null) {
             float l = v.getX();
             float t = v.getY();
             float r = v.getX() + v.getWidth();
             float b = v.getY() + v.getHeight();
 
             Path path = mPath;
             path.reset();
 
             // This block of code draws our own background but omits the top pixel so that
             // if the previous item draws it's border there we don't overwrite it.
             path.moveTo(l, t + 1);
             path.lineTo(r, t + 1);
             path.lineTo(r, b);
             path.lineTo(l, b);
             path.close();
 
             Paint paint = mPaint;
             paint.setStyle(Paint.Style.FILL);
             paint.setColor(0xffffffff);
             c.drawPath(path, paint);
 
             super.dispatchDraw(c);
 
             path.reset();
 
             r -= 1;
 
 
             // This block of code draws the border around the "message block" section
             // of the layout.  This would normally be a simple rectangle but we omit
             // the border at the point of the avatar's divot.  Also, the bottom is drawn
             // 1 pixel below our own bounds to get it to line up with the border of
             // the next item.
             //
             // But for the last item we draw the bottom in our own bounds -- so it will
             // show up.
             if (mIsLastItemInList) {
                 b -= 1;
             }
             if (mAvatar.getPosition() == Divot.RIGHT_UPPER) {
                 path.moveTo(l, t + mAvatar.getCloseOffset());
                 path.lineTo(l, t);
                 path.lineTo(r, t);
                 path.lineTo(r, b);
                 path.lineTo(l, b);
                 path.lineTo(l, t + mAvatar.getFarOffset());
             } else if (mAvatar.getPosition() == Divot.LEFT_UPPER) {
                 path.moveTo(r, t + mAvatar.getCloseOffset());
                 path.lineTo(r, t);
                 path.lineTo(l, t);
                 path.lineTo(l, b);
                 path.lineTo(r, b);
                 path.lineTo(r, t + mAvatar.getFarOffset());
             }
 
 //            paint.setColor(0xff00ff00);
             paint.setColor(0xffcccccc);
             paint.setStrokeWidth(1F);
             paint.setStyle(Paint.Style.STROKE);
             c.drawPath(path, paint);
         } else {
             super.dispatchDraw(c);
         }
     }
 }
