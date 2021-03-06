 /*
  * Copyright (c) 2005-2011, Eugene Stahov (evgs@bombus-im.org), 
  * http://bombus-im.org
  *
  * This program is free software; you can redistribute it and/or
  * modify it under the terms of the GNU General Public License
  * as published by the Free Software Foundation; either version 2
  * of the License, or (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this software; if not, write to the Free Software
  * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
  */
 
 package org.bombusim.lime.activity;
 
 import java.security.InvalidParameterException;
 
 import org.bombusim.lime.Lime;
 import org.bombusim.lime.R;
 import org.bombusim.lime.data.Chat;
 import org.bombusim.lime.data.ChatHistoryDbAdapter;
 import org.bombusim.lime.data.Contact;
 import org.bombusim.lime.data.Message;
 import org.bombusim.lime.data.Roster;
 import org.bombusim.lime.service.XmppService;
 import org.bombusim.lime.service.XmppServiceBinding;
 import org.bombusim.lime.widgets.ChatEditText;
 import org.bombusim.xmpp.handlers.ChatStates;
 import org.bombusim.xmpp.handlers.MessageDispatcher;
 import org.bombusim.xmpp.stanza.XmppPresence;
 import org.bombusim.xmpp.stanza.XmppMessage;
 
 import android.app.Activity;
 import android.content.BroadcastReceiver;
 import android.content.Context;
 import android.content.Intent;
 import android.content.IntentFilter;
 import android.content.res.TypedArray;
 import android.database.Cursor;
 import android.graphics.Bitmap;
 import android.graphics.Color;
 import android.graphics.drawable.Drawable;
 import android.os.Bundle;
 import android.text.ClipboardManager;
 import android.text.Editable;
 import android.text.Spannable;
 import android.text.SpannableString;
 import android.text.SpannableStringBuilder;
 import android.text.TextWatcher;
 import android.text.format.Time;
 import android.text.method.LinkMovementMethod;
 import android.text.style.ForegroundColorSpan;
 import android.text.util.Linkify;
 import android.view.ContextMenu;
 import android.view.KeyEvent;
 import android.view.Menu;
 import android.view.MenuInflater;
 import android.view.MenuItem;
 import android.view.View;
 import android.view.View.OnCreateContextMenuListener;
 import android.view.ViewGroup;
 import android.view.ContextMenu.ContextMenuInfo;
 import android.view.View.OnClickListener;
 import android.view.View.OnKeyListener;
 import android.view.Window;
 import android.view.inputmethod.EditorInfo;
 import android.widget.AdapterView.AdapterContextMenuInfo;
 import android.widget.BaseAdapter;
 import android.widget.CursorAdapter;
 import android.widget.EditText;
 import android.widget.ImageButton;
 import android.widget.ImageView;
 import android.widget.LinearLayout;
 import android.widget.ListView;
 import android.widget.TextView;
 import android.widget.TextView.OnEditorActionListener;
 import android.widget.Toast;
 
 public class ChatActivity extends Activity {
 	public static final String MY_JID = "fromJid";
 	public static final String TO_JID = "toJid";
 
 	private String jid;
 	private String rJid;
 	
 	private ChatEditText messageBox;
 	private ImageButton sendButton;
 	
 	private ListView chatListView;
 	
 	private View contactHead;
 	
 	private XmppServiceBinding serviceBinding;
 	
 	Contact visavis;
 	
 	Chat chat;
 	
 	String sentChatState;
 	
 	protected String visavisNick;
 	protected String myNick;
 	
 	/*
 	 * called when android:launchMode="singleTop"
 	 * single-chat mode, replaces existing chat;
 	 * @see android.app.Activity#onNewIntent(android.content.Intent)
 	 */
 	@Override
 	protected void onNewIntent(Intent intent) {
 		super.onNewIntent(intent);
 
 		attachToChat(intent);
 	}
 	
 	private void attachToChat(Intent intent){
         jid = intent.getStringExtra(TO_JID);
         rJid = intent.getStringExtra(MY_JID);
         
         if (jid == null || rJid ==null) throw new InvalidParameterException("No parameters specified for ChatActivity");
 
         //TODO: move into ChatFactory
         visavis = Lime.getInstance().getRoster().findContact(jid, rJid);
         
         chat = Lime.getInstance().getChatFactory().getChat(jid, rJid);
 
         updateContactBar();
         
         contactHead.requestFocus(); //stealing focus from messageBox
 	}
 
 	private void updateContactBar() {
 		
         ImageView vAvatar = (ImageView) findViewById(R.id.rit_photo);        
 		
 		Bitmap avatar = visavis.getLazyAvatar(true);
 		if (avatar != null) {
 			vAvatar.setImageBitmap(avatar);
 		} else {
 			vAvatar.setImageResource(R.drawable.ic_contact_picture);
 		}
 		
 		((TextView) findViewById(R.id.rit_jid))
         			.setText(visavis.getScreenName());
 		
         ((TextView) findViewById(R.id.rit_presence))
                     .setText(visavis.getStatusMessage());
         
         TypedArray si = getResources().obtainTypedArray(R.array.statusIcons);
         Drawable std = si.getDrawable(visavis.getPresence());
         
         ((ImageView) findViewById(R.id.rit_statusIcon))
         			.setImageDrawable(std);
         
         ((ImageView) findViewById(R.id.composing))
         			.setVisibility( (chat.isComposing()) ? View.VISIBLE : View.GONE );
 
 	}
 	
 	@Override
 	protected void onCreate(Bundle savedInstanceState) {
 		super.onCreate(savedInstanceState);
 		
 		requestWindowFeature(Window.FEATURE_NO_TITLE);
 		
         setContentView(R.layout.chat);
         
         
         serviceBinding = new XmppServiceBinding(this);
   
         contactHead = findViewById(R.id.contact_head);
 
         messageBox = (ChatEditText) findViewById(R.id.messageBox);
         sendButton = (ImageButton) findViewById(R.id.sendButton);
 
         chatListView = (ListView) findViewById(R.id.chatListView);
 
         registerForContextMenu(chatListView);
         
         enableTrackballTraversing();
         
         attachToChat(getIntent());
         
         sendButton.setOnClickListener(new OnClickListener() {
 			@Override
 			public void onClick(View v) { 	sendMessage(); 	}
 		});
 
         //TODO: optional
         messageBox.setOnKeyListener(new OnKeyListener() {
 			@Override
 			public boolean onKey(View v, int keyCode, KeyEvent event) {
 
 				if (event.getAction() != KeyEvent.ACTION_DOWN) return false; //filtering only KEY_DOWN
 				
 				if (keyCode != KeyEvent.KEYCODE_ENTER) return false;
 				//if (event.isShiftPressed()) return false; //typing multiline messages with SHIFT+ENTER
 				sendMessage();
 				return true; //Key was processed
 			}
 		});
 
         messageBox.addTextChangedListener(new TextWatcher() {
 			
 			@Override
 			public void onTextChanged(CharSequence s, int start, int before, int count) {}
 			
 			@Override
 			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
 			
 			@Override
 			public void afterTextChanged(Editable s) {
				sendChatState(ChatStates.COMPOSING);
 			}
 		});
         
         //TODO: optional behavior
         //messageBox.setImeActionLabel("Send", EditorInfo.IME_ACTION_SEND); //Keeps IME opened
         messageBox.setImeActionLabel(getString(R.string.sendMessage), EditorInfo.IME_ACTION_DONE); //Closes IME
         
         
         messageBox.setOnEditorActionListener(new OnEditorActionListener() {
 			
 			@Override
 			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
 				switch (actionId) {
 				case EditorInfo.IME_ACTION_SEND:
 					sendMessage();
 					return true;
 				case EditorInfo.IME_ACTION_DONE:
 					sendMessage();
 					return false; //let IME to be closed
 				}
 				return false;
 			}
 		});
         
 	}
 
 	@Override
 	public boolean onCreateOptionsMenu(android.view.Menu menu) {
 		getMenuInflater().inflate(R.menu.chat_menu, menu);
 
 		//TODO: enable items available only if logged in
 		
 		return true;
 	};
 	
 	
 	@Override
 	public boolean onOptionsItemSelected(MenuItem item) {
 		switch (item.getItemId()) {
 		case R.id.closeChat:
 			Lime.getInstance().getChatFactory().closeChat(chat);
 			finish();
 			break;
 		
 		case R.id.addSmile:   messageBox.showAddSmileDialog();  break;
 		
 		case R.id.addMe:      messageBox.addMe(); break;
 		
 		default: return true; // on submenu
 		}
 		
 		return false;
 	}
 	
 	
 	@Override
 	public void onCreateContextMenu(ContextMenu menu, View v,
 			ContextMenuInfo menuInfo) {
 		
 		super.onCreateContextMenu(menu, v, menuInfo);
 		
 		menu.setHeaderTitle(R.string.messageMenuTitle);
 		
 		MenuInflater inflater = getMenuInflater();
 		inflater.inflate(R.menu.message_menu, menu);
 	}
 	
 	@Override
 	public boolean onContextItemSelected(MenuItem item) {
 		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
 		switch (item.getItemId()) {
 		case R.id.cmdCopy:
 			try {
 				String s = ((MessageView)(info.targetView)).toString();
 
 				// Gets a handle to the clipboard service.
 				ClipboardManager clipboard = (ClipboardManager)
 				        getSystemService(Context.CLIPBOARD_SERVICE);
 			
 				// Set the clipboard's primary clip.
 				clipboard.setText(s);
 				
 			} catch (Exception e) {}
 			return true;
 			
 		case R.id.cmdDelete:
 
 			chatListView.setVisibility(View.GONE);
 			chat.removeFromHistory(info.id);
 			refreshVisualContent();
 			
 			return true;
 
 		default:
 			return super.onContextItemSelected(item);
 	  }
 	}	
 	
 	
     private void enableTrackballTraversing() {
     	//TODO: http://stackoverflow.com/questions/2679948/focusable-edittext-inside-listview
     	chatListView.setItemsCanFocus(true);
 	}
 
 	private class ChatListAdapter extends CursorAdapter {
     	
         
         public ChatListAdapter(Context context, Cursor c) {
 			super(context, c);
 		}
 
 		@Override
 		public void bindView(View view, Context context, Cursor cursor) {
 			MessageView sv = (MessageView) view;
 			
 			// TODO Auto-generated method stub
 			Message m = ChatHistoryDbAdapter.getMessageFromCursor(cursor);
 			
             String sender = (m.type == Message.TYPE_MESSAGE_OUT)? myNick : visavisNick ;  
             sv.setText(m.timestamp, sender, m.messageBody, m.type);
             sv.setUnread(m.unread);
 
 		}
 
 		@Override
 		public View newView(Context context, Cursor cursor, ViewGroup parent) {
 			View v = new MessageView(context);
 			bindView(v, context, cursor);
 			return v;
 		}
         
     }
 	
 	// Time formatter
 	private Time tf=new Time(Time.getCurrentTimezone());
 	private final static long MS_PER_DAY = 1000*60*60*24;
     
     private class MessageView extends LinearLayout {
         public MessageView(Context context) {
             super(context);
             
             this.setOrientation(VERTICAL);
             
             mMessageBody = new TextView(context);
             
             //TODO: available in API 11
             //mMessageBody.setTextIsSelectable(true);
             
             addView(mMessageBody, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
         }
         
         public void setUnread(boolean unread) {
         	setBackgroundColor(Message.getBkColor(unread));
         }
         
         public void setText(long time, String sender, String message, int messageType) {
 
         	//TODO: smart time formatting
         	// 1 minute ago,
         	// hh:mm (after 1 hour)
         	// etc...
         	
         	long delay = System.currentTimeMillis() - time;
         	
         	String fmt = "%H:%M ";
         	if (delay > MS_PER_DAY)  {
         		fmt = "%d.%m.%Y %H:%M ";
         	}
 
         	tf.set(time);
         	String tm=tf.format(fmt);
 
         	SpannableStringBuilder ss = new SpannableStringBuilder(tm);
 
         	int addrEnd=0;
         	
         	if (message.startsWith("/me ")) {
         		message = "*" + message.replaceAll("(/me)(?:\\s|$)", sender+' ');;
         	} else {
         		ss.append('<').append(sender).append("> ");
         	}
         	
     		addrEnd = ss.length()-1;
 
     		int color= Message.getColor(messageType);
        		ss.setSpan(new ForegroundColorSpan(color), 0, addrEnd, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
 
        		SpannableString msg = new SpannableString(message);
         	
             Linkify.addLinks(msg, Linkify.EMAIL_ADDRESSES | Linkify.WEB_URLS);
             Lime.getInstance().getSmilify().addSmiles(msg);
 
             ss.append(msg);
             
             mMessageBody.setText(ss);
             mMessageBody.setMovementMethod(LinkMovementMethod.getInstance());
             
         }
         
         @Override
         public String toString() {
         	return mMessageBody.getText().toString();
         }
         
         private TextView mMessageBody;
     }
     
 	
 	protected void sendMessage() {
 		String text = messageBox.getText().toString();
 		
 		//avoid sending of empty messages
 		if (text.length() == 0) return;
 		
 		String to = visavis.getJid();
 		
 		Message out = new Message(Message.TYPE_MESSAGE_OUT, to, text);
 		chat.addMessage(out);
 
 		//TODO: resource magic
 		XmppMessage msg = new XmppMessage(to, text, null, false);
 		msg.setAttribute("id", String.valueOf(out.getId()) );
 		
 		//TODO: optional delivery confirmation request
 		msg.addChildNs("request", MessageDispatcher.URN_XMPP_RECEIPTS);
 		
 		//TODO: optional chat state notifications
 		msg.addChildNs(ChatStates.ACTIVE, ChatStates.XMLNS_CHATSTATES);
 		sentChatState = ChatStates.ACTIVE;
 		
 		//TODO: message queue
 		if ( serviceBinding.sendStanza(visavis.getRosterJid(), msg) ) {
 			
 			//clear box after success sending
 			messageBox.setText("");
 
 			if (visavis.getPresence() == XmppPresence.PRESENCE_OFFLINE) {
 				Toast.makeText(this, R.string.chatSentOffline, Toast.LENGTH_LONG).show();
 			}
 		
 		} else {
 			Toast.makeText(this, R.string.shouldBeLoggedIn, Toast.LENGTH_LONG).show();
 			//not sent - removing from history
 			chat.removeFromHistory(out.getId());
 		}
 		
 		refreshVisualContent();
 	}
 
 	protected void sendChatState(String state) {
 
 		//TODO: optional chat state notifications
 
 		if (!chat.acceptComposingEvents()) return;
 		
 		if (state.equals(sentChatState)) return; //no duplicates
 		
 		//state machine check: composing->paused
 		if (state.equals(ChatStates.PAUSED))
 			if (!ChatStates.COMPOSING.equals(sentChatState)) return;
 		
 		if ( visavis.getPresence() == XmppPresence.PRESENCE_OFFLINE ) return;
 		
 		String to = visavis.getJid(); 
 
 		//TODO: resource magic
 		XmppMessage msg = new XmppMessage(to);
 		//msg.setAttribute("id", "chatState");
 		
 		msg.addChildNs(state, ChatStates.XMLNS_CHATSTATES);
 		sentChatState = state;
 		
 		serviceBinding.sendStanza(visavis.getRosterJid(), msg);
 		
 	}
 	
 	
 	private class ChatBroadcastReceiver extends BroadcastReceiver {
 
 		@Override
 		public void onReceive(Context context, Intent intent) {
 			refreshVisualContent();
 		}
 		
 	}
 	
 	private class DeliveredReceiver extends BroadcastReceiver {
 		@Override
 		public void onReceive(Context context, Intent intent) {
 			Toast.makeText(ChatActivity.this, R.string.messageDelivered, Toast.LENGTH_SHORT).show();
 		}
 	}
 	
 	private class PresenceReceiver extends BroadcastReceiver {
 		@Override
 		public void onReceive(Context context, Intent intent) {
 			String jid = intent.getStringExtra("param");
 			if (jid==null || visavis.getJid().equals(jid) ) {
 				updateContactBar();
 			}
 		}
 	}
 	
 	private PresenceReceiver bcPresence;
 	private ChatBroadcastReceiver bcUpdateChat;
 	private DeliveredReceiver bcDelivered;
 	
 	@Override
 	protected void onResume() {
 		//TODO: refresh message list, focus to last unread
 		serviceBinding.doBindService();
 
         visavisNick = visavis.getScreenName();
         //TODO: get my nick
         myNick = "Me"; //serviceBinding.getXmppStream(visavis.getRosterJid()).jid; 
 
 		refreshVisualContent();
 		
 		BaseAdapter ca = (BaseAdapter) (chatListView.getAdapter());
         chatListView.setSelection(ca.getCount()-1);
 
 		bcUpdateChat = new ChatBroadcastReceiver();
 		//TODO: presence receiver
 		registerReceiver(bcUpdateChat, new IntentFilter(Chat.UPDATE_CHAT));
 
 		bcDelivered = new DeliveredReceiver();
 		registerReceiver(bcDelivered, new IntentFilter(Chat.DELIVERED));
 		
 		bcPresence = new PresenceReceiver();
 		registerReceiver(bcPresence, new IntentFilter(Roster.UPDATE_CONTACT));
 		
 		String s = chat.getSuspendedText();
 		if (s!=null) {
 			messageBox.setText(s);
 		}
 		
 		messageBox.setDialogHostActivity(this);
 		
 		super.onResume();
 	}
 	
 	public void refreshVisualContent() {
 		
 		chatListView.setVisibility(View.GONE);
 		Cursor c = chat.getCursor();
 		
 		CursorAdapter ca = (CursorAdapter) (chatListView.getAdapter());
 		if (ca == null) {
 			ca = new ChatListAdapter(this, c);
 	        chatListView.setAdapter(ca);
 	        startManagingCursor(c);
 		} else {
 	        //TODO: detach old cursor
 			ca.changeCursor(c);
 	        startManagingCursor(c);
 			
 	        ca.notifyDataSetChanged();
 
 			chatListView.invalidate();
 		}
 		
 
 		chatListView.setVisibility(View.VISIBLE);
 
 		//Move focus to last message is now provided with transcript mode
 		//chatListView.setSelection(chatSize-1);
 		
 	}
 	
 	
 	@Override
 	protected void onPause() {
 		chat.saveSuspendedText(messageBox.getText().toString());
 		
 		sendChatState(ChatStates.PAUSED);
 		
 		serviceBinding.doUnbindService();
 		unregisterReceiver(bcUpdateChat);
 		unregisterReceiver(bcDelivered);
 		unregisterReceiver(bcPresence);
 		
 		markAllRead();
 		
 		//avoid memory leak
 		messageBox.setDialogHostActivity(null);
 
 		super.onPause();
 	}
 
 	private void markAllRead() {
 		synchronized(visavis) {
 			int unreadCount = visavis.getUnread();
 			
 			visavis.setUnread(0);
 
 			CursorAdapter ca = (CursorAdapter) chatListView.getAdapter();
 			Cursor cursor = ca.getCursor();
 			
 			if (cursor.moveToLast()) do {
 				Message m = ChatHistoryDbAdapter.getMessageFromCursor(cursor);
 				if (m.unread) {
 					chat.markRead(m.getId());
 					Lime.getInstance().notificationMgr().cancelChatNotification(m.getId());
 					
 					unreadCount--;
 				}
 			} while ( (unreadCount != 0) && cursor.moveToPrevious());
 		}
 	}
 }
