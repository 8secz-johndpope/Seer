 /**
  * Copyright (C) 2011 Whisper Systems
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
 package org.thoughtcrime.securesms;
 
 import android.content.Context;
 import android.content.Intent;
 import android.graphics.Color;
 import android.graphics.Typeface;
 import android.net.Uri;
 import android.os.Build;
 import android.os.Handler;
 import android.provider.Contacts.Intents;
 import android.provider.ContactsContract.QuickContact;
 import android.text.Spannable;
 import android.text.SpannableStringBuilder;
 import android.text.format.DateUtils;
 import android.text.style.ForegroundColorSpan;
 import android.text.style.StyleSpan;
 import android.util.AttributeSet;
 import android.view.View;
 import android.widget.CheckBox;
 import android.widget.CompoundButton;
 import android.widget.ImageView;
 import android.widget.QuickContactBadge;
 import android.widget.RelativeLayout;
 import android.widget.TextView;
 
 import org.thoughtcrime.securesms.database.model.ThreadRecord;
 import org.thoughtcrime.securesms.recipients.Recipient;
 import org.thoughtcrime.securesms.recipients.Recipients;
 
 import java.util.Set;
 
 /**
  * A view that displays the element in a list of multiple conversation threads.
  * Used by SecureSMS's ListActivity via a ConversationListAdapter.
  *
  * @author Moxie Marlinspike
  */
 
 public class ConversationListItem extends RelativeLayout
                                   implements Recipient.RecipientModifiedListener
 {
 
   private Context           context;
   private Set<Long>         selectedThreads;
   private Recipients        recipients;
   private long              threadId;
   private TextView          subjectView;
   private TextView          fromView;
   private TextView          dateView;
   private CheckBox          checkbox;
   private long              count;
   private boolean           read;
 
   private ImageView         contactPhotoImage;
   private QuickContactBadge contactPhotoBadge;
 
   private final Handler handler = new Handler();
 
   public ConversationListItem(Context context) {
     super(context);
     this.context = context;
   }
 
   public ConversationListItem(Context context, AttributeSet attrs) {
     super(context, attrs);
     this.context = context;
   }
 
   @Override
   protected void onFinishInflate() {
     this.subjectView     = (TextView)findViewById(R.id.subject);
     this.fromView        = (TextView)findViewById(R.id.from);
     this.dateView        = (TextView)findViewById(R.id.date);
     this.checkbox        = (CheckBox)findViewById(R.id.checkbox);
 
     this.contactPhotoBadge = (QuickContactBadge)findViewById(R.id.contact_photo_badge);
     this.contactPhotoImage = (ImageView)findViewById(R.id.contact_photo_image);
 
     initializeContactWidgetVisibility();
     intializeListeners();
   }
 
   public void set(ThreadRecord thread, Set<Long> selectedThreads, boolean batchMode) {
     this.selectedThreads = selectedThreads;
     this.recipients      = thread.getRecipients();
     this.threadId        = thread.getThreadId();
     this.count           = thread.getCount();
     this.read            = thread.isRead();
 
     this.recipients.addListener(this);
     this.fromView.setText(formatFrom(recipients, count, read));
 
     if (thread.isKeyExchange())
       this.subjectView.setText(R.string.ConversationListItem_key_exchange_message,
                                TextView.BufferType.SPANNABLE);
     else
       this.subjectView.setText(thread.getBody(), TextView.BufferType.SPANNABLE);
 
     if (thread.getEmphasis())
       ((Spannable)this.subjectView.getText()).setSpan(new StyleSpan(android.graphics.Typeface.ITALIC), 0, this.subjectView.getText().length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
 
     if (thread.getDate() > 0)
       this.dateView.setText(DateUtils.getRelativeTimeSpanString(getContext(), thread.getDate(), false));
 
     if (selectedThreads != null)
       this.checkbox.setChecked(selectedThreads.contains(threadId));
 
     if (batchMode) checkbox.setVisibility(View.VISIBLE);
     else           checkbox.setVisibility(View.GONE);
 
     setContactPhoto(this.recipients.getPrimaryRecipient());
   }
 
   public void unbind() {
     this.recipients.removeListener(this);
   }
 
   private void intializeListeners() {
     checkbox.setOnCheckedChangeListener(new CheckedChangedListener());
   }
 
   private void initializeContactWidgetVisibility() {
     if (isBadgeEnabled()) {
       contactPhotoBadge.setVisibility(View.VISIBLE);
       contactPhotoImage.setVisibility(View.GONE);
     } else {
       contactPhotoBadge.setVisibility(View.GONE);
       contactPhotoImage.setVisibility(View.VISIBLE);
     }
   }
 
   private void setContactPhoto(final Recipient recipient) {
     if (recipient == null) return;
 
     if (isBadgeEnabled()) {
       contactPhotoBadge.setImageBitmap(recipient.getContactPhoto());
       contactPhotoBadge.assignContactFromPhone(recipient.getNumber(), true);
     } else {
       contactPhotoImage.setImageBitmap(recipient.getContactPhoto());
       contactPhotoImage.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View v) {
           if (recipient.getContactUri() != null) {
             QuickContact.showQuickContact(context, contactPhotoImage, recipient.getContactUri(), QuickContact.MODE_LARGE, null);
           } else {
             Intent intent = new Intent(Intents.SHOW_OR_CREATE_CONTACT,  Uri.fromParts("tel", recipient.getNumber(), null));
             context.startActivity(intent);
           }
         }
       });
     }
   }
 
   private boolean isBadgeEnabled() {
     return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
   }
 
   private CharSequence formatFrom(Recipients from, long count, boolean read) {
    SpannableStringBuilder builder = new SpannableStringBuilder(from.toShortString());
 
     if (count > 0) {
       builder.append(" " + count);
       builder.setSpan(new ForegroundColorSpan(Color.parseColor("#66333333")),
                      from.toShortString().length(), builder.length(),
                       Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
     }
 
     if (!read) {
       builder.setSpan(new StyleSpan(Typeface.BOLD), 0, builder.length(),
                       Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
     }
 
     return builder;
   }
 
   public Recipients getRecipients() {
     return recipients;
   }
 
   public long getThreadId() {
     return threadId;
   }
 
   private class CheckedChangedListener implements CompoundButton.OnCheckedChangeListener {
     public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
       if (isChecked) selectedThreads.add(threadId);
       else           selectedThreads.remove(threadId);
     }
   }
 
   @Override
   public void onModified(Recipient recipient) {
     handler.post(new Runnable() {
       @Override
       public void run() {
         ConversationListItem.this.fromView.setText(formatFrom(recipients, count, read));
         setContactPhoto(ConversationListItem.this.recipients.getPrimaryRecipient());
       }
     });
   }
 }
