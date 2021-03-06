 package ru.sawim.view;
 
 import DrawControls.icons.Icon;
 import DrawControls.icons.ImageList;
 import android.app.Activity;
 import android.content.*;
 import android.content.res.Configuration;
 import android.content.res.Resources;
 import android.os.Bundle;
 import android.support.v4.app.Fragment;
 import android.text.Editable;
 import android.text.TextWatcher;
 import android.view.*;
 import android.view.inputmethod.EditorInfo;
 import android.view.inputmethod.InputMethodManager;
 import android.widget.*;
 import protocol.Contact;
 import protocol.ContactMenu;
 import protocol.Protocol;
 import protocol.jabber.*;
 import ru.sawim.General;
 import ru.sawim.R;
 import ru.sawim.models.MessagesAdapter;
 import sawim.FileTransfer;
 import sawim.Options;
 import sawim.SawimUI;
 import sawim.chat.Chat;
 import sawim.chat.ChatHistory;
 import sawim.chat.MessData;
 import sawim.cl.ContactList;
 import sawim.comm.StringConvertor;
 import ru.sawim.Scheme;
 import sawim.util.JLocale;
 
 import java.util.Hashtable;
 import java.util.List;
 
 /**
  * Created with IntelliJ IDEA.
  * User: Gerc
  * Date: 24.01.13
  * Time: 20:30
  * To change this template use File | Settings | File Templates.
  */
 public class ChatView extends Fragment implements AbsListView.OnScrollListener, General.OnUpdateChat {
 
     public static final String PASTE_TEXT = "ru.sawim.PASTE_TEXT";
 
     private static Hashtable<String, Integer> positionHash = new Hashtable<String, Integer>();
     private Chat chat;
     private Protocol protocol;
     private Contact currentContact;
     private List<MessData> messData;
     private MyListView chatListView;
     private EditText messageEditor;
     private boolean sendByEnter;
     private MessagesAdapter adapter;
     private BroadcastReceiver textReceiver;
     private LinearLayout sidebar;
     private ImageButton usersImage;
     private ListView nickList;
     private ImageButton chatsImage;
     private TextView contactName;
     private TextView contactStatus;
     private LinearLayout chatBarLayout;
     private LinearLayout chat_viewLayout;
     private MucUsersView mucUsersView;
 
     @Override
     public void onActivityCreated(Bundle b) {
         super.onActivityCreated(b);
         chatBarLayout.setBackgroundColor(General.getColorWithAlpha(Scheme.THEME_CAP_BACKGROUND));
         chat_viewLayout.setBackgroundColor(General.getColorWithAlpha(Scheme.THEME_BACKGROUND));
         usersImage.setImageBitmap(General.iconToBitmap(ImageList.createImageList("/participants.png").iconAt(0)));
         usersImage.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View view) {
                 sidebar.setVisibility(sidebar.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
                 updateChat();
             }
         });
         chatsImage.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View view) {
                 forceGoToChat();
             }
         });
         updateChatsIcon();
         registerReceivers();
     }
 
     @Override
     public View onCreateView(LayoutInflater inflater, ViewGroup container,
                              Bundle savedInstanceState) {
         View v = inflater.inflate(R.layout.chat, container, false);
         chat_viewLayout = (LinearLayout) v.findViewById(R.id.chat_view);
         chatBarLayout = (LinearLayout) v.findViewById(R.id.chat_bar);
         usersImage = (ImageButton) v.findViewById(R.id.usersImage);
         contactName = (TextView) v.findViewById(R.id.item_name);
         contactStatus = (TextView) v.findViewById(R.id.item_description);
         chatsImage = (ImageButton) v.findViewById(R.id.chatsImage);
         return v;
     }
 
     public static final int MENU_COPY_TEXT = 1;
     private static final int ACTION_ADD_TO_HISTORY = 2;
     private static final int ACTION_TO_NOTES = 3;
     private static final int ACTION_QUOTE = 4;
     private static final int ACTION_DEL_CHAT = 5;
 
     public void onCreateMenu(Menu menu) {
         boolean accessible = chat.getWritable() && (currentContact.isSingleUserContact() || currentContact.isOnline());
         if (0 < chat.getAuthRequestCounter()) {
             menu.add(Menu.FIRST, Contact.USER_MENU_GRANT_AUTH, 0, JLocale.getString("grant"));
             menu.add(Menu.FIRST, Contact.USER_MENU_DENY_AUTH, 0, JLocale.getString("deny"));
         }
         if (!currentContact.isAuth()) {
             menu.add(Menu.FIRST, Contact.USER_MENU_REQU_AUTH, 0, JLocale.getString("requauth"));
         }
         if (accessible) {
             if (sawim.modules.fs.FileSystem.isSupported()) {
                 menu.add(Menu.FIRST, Contact.USER_MENU_FILE_TRANS, 0, JLocale.getString("ft_name"));
             }
             if (FileTransfer.isPhotoSupported()) {
                 menu.add(Menu.FIRST, Contact.USER_MENU_CAM_TRANS, 0, JLocale.getString("ft_cam"));
             }
         }
         if (!currentContact.isSingleUserContact() && currentContact.isOnline()) {
             menu.add(Menu.FIRST, Contact.CONFERENCE_DISCONNECT, 0, JLocale.getString("leave_chat"));
         }
         menu.add(Menu.FIRST, Contact.USER_MENU_STATUSES, 2, R.string.user_statuses);
         menu.add(Menu.FIRST, ACTION_DEL_CHAT, 0, JLocale.getString("delete_chat"));
     }
 
     public void onMenuItemSelected(MenuItem item) {
         if (item.getItemId() == ACTION_DEL_CHAT) {
             /*chat.removeMessagesAtCursor(chatListView.getFirstVisiblePosition() + 1);
             if (0 < messData.size()) {
                 updateChat();
             }*/
             ChatHistory.instance.unregisterChat(chat);
             getActivity().finish();
             return;
         }
         new ContactMenu(protocol, currentContact).doAction(getActivity(), item.getItemId());
     }
 
     @Override
     public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
         super.onCreateContextMenu(menu, v, menuInfo);
         menu.add(Menu.FIRST, MENU_COPY_TEXT, 0, android.R.string.copy);
         menu.add(Menu.FIRST, ACTION_QUOTE, 0, JLocale.getString("quote"));
         if (protocol instanceof Jabber) {
             menu.add(Menu.FIRST, ACTION_TO_NOTES, 0, R.string.add_to_notes);
         }
         if (!Options.getBoolean(Options.OPTION_HISTORY) && chat.hasHistory()) {
             menu.add(Menu.FIRST, ACTION_ADD_TO_HISTORY, 0, JLocale.getString("add_to_history"));
         }
         currentContact.addChatMenuItems(menu);
     }
 
     @Override
     public boolean onContextItemSelected(MenuItem item) {
         AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
         MessData md = messData.get(info.position);
         String msg = md.getText();
         switch (item.getItemId()) {
             case MENU_COPY_TEXT:
                 if (null == md) {
                     return false;
                 }
                 if (md.isMe()) {
                     msg = "*" + md.getNick() + " " + msg;
                 }
                 SawimUI.setClipBoardText(md.isIncoming(), md.getNick(), md.strTime, msg + "\n");
                 break;
 
             case ACTION_QUOTE:
                 StringBuffer sb = new StringBuffer();
                 if (md.isMe()) {
                     msg = "*" + md.getNick() + " " + msg;
                 }
                 sb.append(SawimUI.serialize(md.isIncoming(), md.getNick() + " " + md.strTime, msg));
                 sb.append("\n-----\n");
                 SawimUI.setClipBoardText(0 == sb.length() ? null : sb.toString());
                 break;
 
             case ACTION_ADD_TO_HISTORY:
                 chat.addTextToHistory(md);
                 break;
 
             case ACTION_TO_NOTES:
                 MirandaNotes notes = ((Jabber)protocol).getMirandaNotes();
                 notes.showIt();
                 MirandaNotes.Note note = notes.addEmptyNote();
                 note.tags = md.getNick() + " " + md.strTime;
                 note.text = md.getText();
                 notes.showNoteEditor(note);
                 break;
         }
         return super.onContextItemSelected(item);
     }
 
     private void registerReceivers() {
         textReceiver = new BroadcastReceiver() {
             @Override
             public void onReceive(Context c, final Intent i) {
                 getActivity().runOnUiThread(new Runnable() {
                     @Override
                     public void run() {
                         insert(" " + i.getExtras().getString("text") + " ");
                         showKeyboard();
                     }
                 });
             }
         };
         getActivity().registerReceiver(textReceiver, new IntentFilter(PASTE_TEXT));
     }
 
     private void unregisterReceivers() {
         try {
             getActivity().unregisterReceiver(textReceiver);
         } catch (java.lang.IllegalArgumentException e) {
         }
     }
 
     @Override
     public void onDestroy() {
         super.onDestroy();
         destroy(chat);
     }
 
     @Override
     public void onPause() {
         super.onPause();
         pause(chat);
     }
 
     @Override
     public void onResume() {
         super.onResume();
         resume(chat);
     }
 
     public void destroy(Chat chat) {
         General.getInstance().setOnUpdateChat(null);
         if (chat == null) return;
         chat.resetUnreadMessages();
         chat.setVisibleChat(false);
         unregisterReceivers();
     }
 
     public void pause(Chat chat) {
         if (chat == null) return;
         addLastPosition(chat.getContact().getUserId(), chatListView.getFirstVisiblePosition());
     }
 
     public void resume(final Chat chat) {
         if (chat == null) return;
         getActivity().runOnUiThread(new Runnable() {
             @Override
             public void run() {
                 int count = chatListView.getCount();
                 int unreadMessages = chat.getUnreadMessageCount();
                 int lastPosition = getLastPosition(chat.getContact().getUserId()) + 1;
                 if (lastPosition >= 0) {
                     chatListView.setScroll(false);
                     chatListView.setSelection(lastPosition);
                 } else {
                     if (unreadMessages > 0) {
                         chatListView.setScroll(false);
                         chatListView.setSelection(count - (unreadMessages + 1));
                     } else {
                         if (chatListView.isScroll()) chatListView.setSelection(count);
                     }
                 }
             }
         });
         chat.resetUnreadMessages();
         updateChatsIcon();
     }
 
     private void forceGoToChat() {
         addLastPosition(chat.getContact().getUserId(), chatListView.getFirstVisiblePosition());
         chat.resetUnreadMessages();
         chat.setVisibleChat(false);
         ChatHistory chatHistory = ChatHistory.instance;
         Chat current = chatHistory.chatAt(chatHistory.getPreferredItem());
         if (0 < current.getUnreadMessageCount()) {
             openChat(current.getProtocol(), current.getContact());
             resume(current);
         }
     }
 
     public void openChat(Protocol p, Contact c) {
         General.getInstance().setOnUpdateChat(null);
         General.getInstance().setOnUpdateChat(this);
         final Activity currentActivity = getActivity();
         protocol = p;
         currentContact = c;
         chat = protocol.getChat(currentContact);
         messData = chat.getMessData();
         adapter = new MessagesAdapter(currentActivity, chat, messData);
         chatListView = (MyListView) currentActivity.findViewById(R.id.chat_history_list);
         chat.setVisibleChat(true);
 
         contactName.setTextColor(General.getColor(Scheme.THEME_CAP_TEXT));
         contactName.setText(currentContact.getName());
         contactStatus.setTextColor(General.getColor(Scheme.THEME_CAP_TEXT));
         contactStatus.setText(ContactList.getInstance().getManager().getStatusMessage(currentContact));
         messageEditor = (EditText) currentActivity.findViewById(R.id.messageBox);
         int background = General.getColorWithAlpha(Scheme.THEME_BACKGROUND);
         LinearLayout chatLayout = (LinearLayout) currentActivity.findViewById(R.id.chat_view);
         chatLayout.setBackgroundColor(General.getColorWithAlpha(Scheme.THEME_BACKGROUND));
         messageEditor.setBackgroundColor(background);
         messageEditor.setTextColor(General.getColor(Scheme.THEME_TEXT));
 
         nickList = (ListView) currentActivity.findViewById(R.id.muc_user_list);
         sidebar = (LinearLayout) currentActivity.findViewById(R.id.sidebar);
         sidebar.setVisibility(View.GONE);
         sidebar.setBackgroundColor(General.getColor(Scheme.THEME_BACKGROUND));
         if (currentContact instanceof JabberServiceContact && currentContact.isConference()) {
             final JabberServiceContact jabberServiceContact = (JabberServiceContact) currentContact;
             mucUsersView = new MucUsersView(protocol, jabberServiceContact);
             mucUsersView.show(getActivity(), nickList, usersImage, this);
 
             if (sidebar.getVisibility() == View.VISIBLE) {
                 sidebar.setVisibility(View.VISIBLE);
             } else {
                 sidebar.setVisibility(View.GONE);
             }
         } else {
             usersImage.setVisibility(View.GONE);
             nickList.setVisibility(View.GONE);
         }
         ImageButton smileButton = (ImageButton) currentActivity.findViewById(R.id.input_smile_button);
         smileButton.setBackgroundColor(background);
         smileButton.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View view) {
                 new SmilesView().show(getActivity().getSupportFragmentManager(), "show-smiles");
             }
         });
         sendByEnter = Options.getBoolean(Options.OPTION_SIMPLE_INPUT);
         ImageButton sendButton = (ImageButton) currentActivity.findViewById(R.id.input_send_button);
         if (sendByEnter) {
             sendButton.setVisibility(ImageButton.GONE);
         } else {
             sendButton.setVisibility(ImageButton.VISIBLE);
             sendButton.setBackgroundColor(background);
             sendButton.setOnClickListener(new View.OnClickListener() {
                 @Override
                 public void onClick(View view) {
                     send();
                 }
             });
         }
         if (sendByEnter) {
             messageEditor.setImeOptions(EditorInfo.IME_ACTION_SEND);
             messageEditor.setOnEditorActionListener(enterListener);
         }
         messageEditor.addTextChangedListener(textWatcher);
         chatListView.setFocusable(true);
        chatListView.setClickable(true);
        chatListView.setLongClickable(true);
         chatListView.setCacheColorHint(0x00000000);
         chatListView.setOnScrollListener(this);
         chatListView.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);
         chatListView.setStackFromBottom(true);
         chatListView.setAdapter(adapter);
         chatListView.setOnCreateContextMenuListener(this);
         chatListView.setOnItemClickListener(new ChatClick());
     }
 
     public class ChatClick implements ListView.OnItemClickListener {
         @Override
         public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
             MessData msg = (MessData) adapterView.getAdapter().getItem(position);
             setText("");
             setText(onMessageSelected(msg));
         }
     }
 
 
     public Chat getCurrentChat() {
         return chat;
     }
 
     private void showKeyboard(View view) {
         Configuration conf = Resources.getSystem().getConfiguration();
         if (conf.hardKeyboardHidden != Configuration.HARDKEYBOARDHIDDEN_NO) {
             InputMethodManager keyboard = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
             keyboard.showSoftInput(view, InputMethodManager.SHOW_FORCED);
         }
     }
 
     public void hideKeyboard(View view) {
         InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
         imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
     }
 
     public void showKeyboard() {
         messageEditor.requestFocus();
         showKeyboard(messageEditor);
     }
 
     private void send() {
         hideKeyboard(messageEditor);
         chat.sendMessage(getText());
         resetText();
         updateChat();
     }
 
     public boolean canAdd(String what) {
         String text = getText();
         if (0 == text.length()) return false;
         // more then one comma
         if (text.indexOf(',') != text.lastIndexOf(',')) return true;
         // replace one post number to another
         if (what.startsWith("#") && !text.contains(" ")) return false;
         return !text.endsWith(", ");
     }
 
     public void resetText() {
         getActivity().runOnUiThread(new Runnable() {
             @Override
             public void run() {
                 messageEditor.setText("");
             }
         });
     }
 
     public String getText() {
         return messageEditor.getText().toString();
     }
 
     public void setText(final String text) {
         getActivity().runOnUiThread(new Runnable() {
             @Override
             public void run() {
                 String t = null == text ? "" : text;
                 if ((0 == t.length()) || !canAdd(t)) {
                     messageEditor.setText(t);
                     messageEditor.setSelection(t.length());
                 } else {
                     insert(t);
                 }
                 showKeyboard();
             }
         });
     }
 
     public boolean hasText() {
         return 0 < messageEditor.getText().length();
     }
 
     public void insert(String text) {
         int start = messageEditor.getSelectionStart();
         int end = messageEditor.getSelectionEnd();
         messageEditor.getText().replace(Math.min(start, end), Math.max(start, end),
                 text, 0, text.length());
     }
 
     private boolean isDone(int actionId) {
         return (EditorInfo.IME_NULL == actionId)
                 || (EditorInfo.IME_ACTION_DONE == actionId)
                 || (EditorInfo.IME_ACTION_SEND == actionId);
     }
 
     private void updateChatsIcon() {
         Icon icMess = ChatHistory.instance.getUnreadMessageIcon();
         if (icMess == null) {
             chatsImage.setVisibility(ImageView.GONE);
         } else {
             chatsImage.setVisibility(ImageView.VISIBLE);
             chatsImage.setImageBitmap(General.iconToBitmap(icMess));
         }
     }
 
     private String getBlogPostId(String text) {
         if (StringConvertor.isEmpty(text)) {
             return null;
         }
         String lastLine = text.substring(text.lastIndexOf('\n') + 1);
         if (0 == lastLine.length()) {
             return null;
         }
         if ('#' != lastLine.charAt(0)) {
             return null;
         }
         int numEnd = lastLine.indexOf(' ');
         if (-1 != numEnd) {
             lastLine = lastLine.substring(0, numEnd);
         }
         return lastLine + " ";
     }
 
     private String writeMessageTo(String nick) {
         if (null != nick) {
             if ('/' == nick.charAt(0)) {
                 nick = ' ' + nick;
             }
             nick += Chat.ADDRESS;
 
         } else {
             nick = "";
         }
         return nick;
     }
 
     private boolean isBlogBot() {
         if (currentContact instanceof JabberContact) {
             return ((Jabber) protocol).isBlogBot(currentContact.getUserId());
         }
         return false;
     }
 
     public String onMessageSelected(MessData md) {
         if (currentContact.isSingleUserContact()) {
             if (isBlogBot()) {
                 return getBlogPostId(md.getText());
             }
             return "";
         }
         String nick = ((null == md) || md.isFile()) ? null : md.getNick();
         return writeMessageTo(chat.getMyName().equals(nick) ? null : nick);
     }
 
     @Override
     public void updateChat() {
         getActivity().runOnUiThread(new Runnable() {
             @Override
             public void run() {
                 updateChatsIcon();
                 boolean scroll = chatListView.isScroll();
                 if (adapter != null) {
                     if (scroll && chatListView.getCount() >= 1) {
                         chatListView.setSelection(chatListView.getCount());
                     }
                     adapter.notifyDataSetChanged();
                 }
             }
         });
         updateMucList();
 
         RosterView rosterView = (RosterView) getActivity().getSupportFragmentManager().findFragmentById(R.id.roster_fragment);
         if (rosterView != null) {
             rosterView.updateBarProtocols();
             rosterView.updateRoster();
         }
     }
 
     @Override
     public void addMessage(final Chat chat, final MessData mess) {
         getActivity().runOnUiThread(new Runnable() {
             @Override
             public void run() {
                 if (adapter != null) {
                     chat.removeOldMessages();
                     chat.getMessData().add(mess);
                     adapter.notifyDataSetChanged();
                 }
             }
         });
     }
 
     @Override
     public void updateMucList() {
         getActivity().runOnUiThread(new Runnable() {
             @Override
             public void run() {
                 if (mucUsersView != null)
                     mucUsersView.update();
             }
         });
     }
 
     private void addLastPosition(String jid, int position) {
         positionHash.put(jid, position);
     }
 
     private int getLastPosition(String jid) {
         if (positionHash.containsKey(jid)) return positionHash.remove(jid);
         else return -1;
     }
 
     public void onScrollStateChanged(AbsListView view, int scrollState) {
     }
 
     public void onScroll(AbsListView view, final int firstVisibleItem, final int visibleItemCount, final int totalItemCount) {
         if (firstVisibleItem + visibleItemCount == totalItemCount) chatListView.setScroll(true);
         else chatListView.setScroll(false);
     }
 
     private TextWatcher textWatcher = new TextWatcher() {
         private String previousText;
         private int lineCount = 0;
 
         @Override
         public void beforeTextChanged(CharSequence s, int start, int count, int after) {
             if (sendByEnter) {
                 previousText = s.toString();
             }
         }
 
         @Override
         public void onTextChanged(CharSequence s, int start, int before, int count) {
             if (sendByEnter && (start + count <= s.length()) && (1 == count)) {
                 boolean enter = ('\n' == s.charAt(start));
                 if (enter) {
                     messageEditor.setText(previousText);
                     messageEditor.setSelection(start);
                     send();
                 }
             }
             if (lineCount != messageEditor.getLineCount()) {
                 lineCount = messageEditor.getLineCount();
                 messageEditor.requestLayout();
             }
         }
 
         @Override
         public void afterTextChanged(Editable editable) {
         }
     };
 
     private final TextView.OnEditorActionListener enterListener = new TextView.OnEditorActionListener() {
         public boolean onEditorAction(TextView exampleView, int actionId, KeyEvent event) {
             if (isDone(actionId)) {
                 if ((null == event) || (event.getAction() == KeyEvent.ACTION_DOWN)) {
                     send();
                     return true;
                 }
             }
             return false;
         }
     };
 }
