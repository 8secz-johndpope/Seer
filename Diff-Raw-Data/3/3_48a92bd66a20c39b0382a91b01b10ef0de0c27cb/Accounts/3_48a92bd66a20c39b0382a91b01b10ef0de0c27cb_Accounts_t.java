 
 package com.fsck.k9.activity;
 
 import android.app.AlertDialog;
 import android.app.Dialog;
 import android.content.Context;
 import android.content.DialogInterface;
 import android.content.Intent;
 import android.content.pm.PackageInfo;
 import android.content.pm.PackageManager;
 import android.os.Bundle;
 import android.os.Handler;
 import android.util.Log;
 import android.view.*;
 import android.view.ContextMenu.ContextMenuInfo;
 import android.view.View.OnClickListener;
 import android.webkit.WebView;
 import android.widget.*;
 import android.widget.AdapterView.AdapterContextMenuInfo;
 import android.widget.AdapterView.OnItemClickListener;
 import com.fsck.k9.*;
 import com.fsck.k9.activity.setup.AccountSettings;
 import com.fsck.k9.activity.setup.AccountSetupBasics;
 import com.fsck.k9.activity.setup.Prefs;
 import com.fsck.k9.mail.Flag;
 
 import java.util.ArrayList;
 import java.util.List;
 import java.util.Map;
 import java.util.concurrent.ConcurrentHashMap;
 
 public class Accounts extends K9ListActivity implements OnItemClickListener, OnClickListener
 {
     private static final int DIALOG_REMOVE_ACCOUNT = 1;
     private ConcurrentHashMap<String, AccountStats> accountStats = new ConcurrentHashMap<String, AccountStats>();
 
     private ConcurrentHashMap<BaseAccount, String> pendingWork = new ConcurrentHashMap<BaseAccount, String>();
 
     private Account mSelectedContextAccount;
     private int mUnreadMessageCount = 0;
 
     private AccountsHandler mHandler = new AccountsHandler();
     private AccountsAdapter mAdapter;
     private SearchAccount unreadAccount = null;
     private SearchAccount flaggedAccount = null;
     private SearchAccount integratedInboxAccount = null;
     private SearchAccount integratedInboxStarredAccount = null;
 
 
     class AccountsHandler extends Handler
     {
         private void setViewTitle()
         {
             String dispString = mListener.formatHeader(Accounts.this, getString(R.string.accounts_title), mUnreadMessageCount, getTimeFormat());
 
             setTitle(dispString);
         }
         public void refreshTitle()
         {
             runOnUiThread(new Runnable()
             {
                 public void run()
                 {
                     setViewTitle();
                 }
             });
         }
 
         public void dataChanged()
         {
             runOnUiThread(new Runnable()
             {
                 public void run()
                 {
                     if (mAdapter != null)
                     {
                         mAdapter.notifyDataSetChanged();
                     }
                 }
             });
         }
 
         public void workingAccount(final Account account, final int res)
         {
             runOnUiThread(new Runnable()
             {
                 public void run()
                 {
                     String toastText = getString(res, account.getDescription());
 
                     Toast toast = Toast.makeText(getApplication(), toastText, Toast.LENGTH_SHORT);
                     toast.show();
                 }
             });
         }
 
         public void accountSizeChanged(final Account account, final long oldSize, final long newSize)
         {
             runOnUiThread(new Runnable()
             {
                 public void run()
                 {
                     AccountStats stats = accountStats.get(account.getUuid());
                     if (stats != null)
                     {
                         stats.size = newSize;
                     }
                     String toastText = getString(R.string.account_size_changed, account.getDescription(),
                                                  SizeFormatter.formatSize(getApplication(), oldSize), SizeFormatter.formatSize(getApplication(), newSize));;
 
                     Toast toast = Toast.makeText(getApplication(), toastText, Toast.LENGTH_LONG);
                     toast.show();
                     if (mAdapter != null)
                     {
                         mAdapter.notifyDataSetChanged();
                     }
                 }
             });
         }
 
         public void progress(final boolean progress)
         {
             runOnUiThread(new Runnable()
             {
                 public void run()
                 {
                     setProgressBarIndeterminateVisibility(progress);
                 }
             });
         }
         public void progress(final int progress)
         {
             runOnUiThread(new Runnable()
             {
                 public void run()
                 {
                     getWindow().setFeatureInt(Window.FEATURE_PROGRESS, progress);
                 }
             });
         }
     }
 
     ActivityListener mListener = new ActivityListener()
     {
         @Override
         public void folderStatusChanged(Account account, String folderName, int unreadMessageCount)
         {
             try
             {
                 AccountStats stats = account.getStats(Accounts.this);
                 accountStatusChanged(account, stats);
             }
             catch (Exception e)
             {
                 Log.e(K9.LOG_TAG, "Unable to get account stats", e);
             }
         }
         @Override
         public void accountStatusChanged(BaseAccount account, AccountStats stats)
         {
             AccountStats oldStats = accountStats.get(account.getUuid());
             int oldUnreadMessageCount = 0;
             if (oldStats != null)
             {
                 oldUnreadMessageCount = oldStats.unreadMessageCount;
             }
             accountStats.put(account.getUuid(), stats);
             if (account instanceof Account)
             {
                 mUnreadMessageCount += stats.unreadMessageCount - oldUnreadMessageCount;
             }
             mHandler.dataChanged();
             pendingWork.remove(account);
 
             if (pendingWork.isEmpty())
             {
                 mHandler.progress(Window.PROGRESS_END);
                 mHandler.refreshTitle();
             }
             else
             {
                 int level = (Window.PROGRESS_END / mAdapter.getCount()) * (mAdapter.getCount() - pendingWork.size()) ;
                 mHandler.progress(level);
             }
         }
 
         @Override
         public void accountSizeChanged(Account account, long oldSize, long newSize)
         {
             mHandler.accountSizeChanged(account, oldSize, newSize);
         }
 
         @Override
         public void synchronizeMailboxFinished(
             Account account,
             String folder,
             int totalMessagesInMailbox,
             int numNewMessages)
         {
             super.synchronizeMailboxFinished(account, folder, totalMessagesInMailbox, numNewMessages);
             MessagingController.getInstance(getApplication()).getAccountStats(Accounts.this, account, mListener);
 
             mHandler.progress(false);
 
             mHandler.refreshTitle();
         }
 
         @Override
         public void synchronizeMailboxStarted(Account account, String folder)
         {
             super.synchronizeMailboxStarted(account, folder);
             mHandler.progress(true);
             mHandler.refreshTitle();
         }
 
         @Override
         public void synchronizeMailboxProgress(Account account, String folder, int completed, int total)
         {
             super.synchronizeMailboxProgress(account, folder, completed, total);
             mHandler.refreshTitle();
         }
 
         @Override
         public void synchronizeMailboxFailed(Account account, String folder,
                                              String message)
         {
             super.synchronizeMailboxFailed(account, folder, message);
             mHandler.progress(false);
             mHandler.refreshTitle();
 
         }
 
         @Override
         public void sendPendingMessagesStarted(Account account)
         {
             super.sendPendingMessagesStarted(account);
             mHandler.refreshTitle();
         }
 
         @Override
         public void sendPendingMessagesCompleted(Account account)
         {
             super.sendPendingMessagesCompleted(account);
             mHandler.refreshTitle();
         }
 
 
         @Override
         public void sendPendingMessagesFailed(Account account)
         {
             super.sendPendingMessagesFailed(account);
             mHandler.refreshTitle();
         }
 
         @Override
         public void pendingCommandsProcessing(Account account)
         {
             super.pendingCommandsProcessing(account);
             mHandler.refreshTitle();
         }
 
         @Override
         public void pendingCommandsFinished(Account account)
         {
             super.pendingCommandsFinished(account);
             mHandler.refreshTitle();
         }
 
         @Override
         public void pendingCommandStarted(Account account, String commandTitle)
         {
             super.pendingCommandStarted(account, commandTitle);
             mHandler.refreshTitle();
         }
 
         @Override
         public void pendingCommandCompleted(Account account, String commandTitle)
         {
             super.pendingCommandCompleted(account, commandTitle);
             mHandler.refreshTitle();
         }
 
 
     };
 
     private static String ACCOUNT_STATS = "accountStats";
     private static String SELECTED_CONTEXT_ACCOUNT = "selectedContextAccount";
 
     public static final String EXTRA_STARTUP = "startup";
 
 
     public static void actionLaunch(Context context)
     {
         Intent intent = new Intent(context, Accounts.class);
         intent.putExtra(EXTRA_STARTUP, true);
         context.startActivity(intent);
     }
 
     public static void listAccounts(Context context)
     {
         Intent intent = new Intent(context, Accounts.class);
         intent.putExtra(EXTRA_STARTUP, false);
         context.startActivity(intent);
     }
 
 
     @Override
     public void onCreate(Bundle icicle)
     {
         unreadAccount = new SearchAccount(this, false, null, new Flag[] { Flag.SEEN } );
         unreadAccount.setDescription(getString(R.string.search_unread_messages_title));
         unreadAccount.setEmail(getString(R.string.search_unread_messages_detail));
         
         flaggedAccount = new SearchAccount(this, false, new Flag[] { Flag.FLAGGED }, null);
         flaggedAccount.setDescription(getString(R.string.search_starred_messages_title));
         flaggedAccount.setEmail(getString(R.string.search_starred_messages_detail));
         
         integratedInboxAccount = new SearchAccount(this, true, null,  new Flag[] { Flag.SEEN });
         integratedInboxAccount.setDescription(getString(R.string.integrated_inbox_title));
         integratedInboxAccount.setEmail(getString(R.string.integrated_inbox_detail));
         
         integratedInboxStarredAccount = new SearchAccount(this, true, new Flag[] { Flag.FLAGGED },  null);
         integratedInboxStarredAccount.setDescription(getString(R.string.integrated_inbox_starred_title));
         integratedInboxStarredAccount.setEmail(getString(R.string.integrated_inbox_starred_detail));
         
         super.onCreate(icicle);
 
         Account[] accounts = Preferences.getPreferences(this).getAccounts();
         Intent intent = getIntent();
         boolean startup = (boolean)intent.getBooleanExtra(EXTRA_STARTUP, true);
         if (startup && accounts.length == 1)
         {
             onOpenAccount(accounts[0]);
             finish();
         }
         else
         {
             requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
             requestWindowFeature(Window.FEATURE_PROGRESS);
 
             setContentView(R.layout.accounts);
             ListView listView = getListView();
             listView.setOnItemClickListener(this);
             listView.setItemsCanFocus(false);
             listView.setEmptyView(findViewById(R.id.empty));
             findViewById(R.id.next).setOnClickListener(this);
             registerForContextMenu(listView);
 
             if (icicle != null && icicle.containsKey(SELECTED_CONTEXT_ACCOUNT))
             {
                 String accountUuid = icicle.getString("selectedContextAccount");
                 mSelectedContextAccount = Preferences.getPreferences(this).getAccount(accountUuid);
             }
 
             if (icicle != null)
             {
                 Map<String, AccountStats> oldStats = (Map<String, AccountStats>)icicle.get(ACCOUNT_STATS);
                 if (oldStats != null)
                 {
                     accountStats.putAll(oldStats);
                 }
             }
         }
     }
 
     @Override
     public void onSaveInstanceState(Bundle outState)
     {
         super.onSaveInstanceState(outState);
         if (mSelectedContextAccount != null)
         {
             outState.putString(SELECTED_CONTEXT_ACCOUNT, mSelectedContextAccount.getUuid());
         }
         outState.putSerializable(ACCOUNT_STATS, accountStats);
     }
 
     @Override
     public void onResume()
     {
         super.onResume();
 
         refresh();
         MessagingController.getInstance(getApplication()).addListener(mListener);
     }
 
     @Override
     public void onPause()
     {
         super.onPause();
         MessagingController.getInstance(getApplication()).removeListener(mListener);
     }
 
     private void refresh()
     {
         BaseAccount[] accounts = Preferences.getPreferences(this).getAccounts();
         
         List<BaseAccount> newAccounts = new ArrayList<BaseAccount>(accounts.length + 4);
         newAccounts.add(integratedInboxAccount);
         if (K9.messageListStars())
         {
             newAccounts.add(integratedInboxStarredAccount);
         }
         newAccounts.add(unreadAccount);
         if (K9.messageListStars())
         {
             newAccounts.add(flaggedAccount);
         }
         for (BaseAccount account : accounts)
         {
             newAccounts.add(account);
         }
        
         mAdapter = new AccountsAdapter(newAccounts.toArray(new BaseAccount[0]));
         getListView().setAdapter(mAdapter);
         if (newAccounts.size() > 0)
         {
             mHandler.progress(Window.PROGRESS_START);
         }
         pendingWork.clear();
 
         for (BaseAccount account : newAccounts)
         {
             pendingWork.put(account, "true");
             if (account instanceof Account)
             {
                 Account realAccount = (Account)account;
                 MessagingController.getInstance(getApplication()).getAccountStats(Accounts.this, realAccount, mListener);
             }
             else if (account instanceof SearchAccount)
             {
                 SearchAccount searchAccount = (SearchAccount)account;
             
                 MessagingController.getInstance(getApplication()).searchLocalMessages(searchAccount, null, mListener);
             }
         }
         
     }
 
     private void onAddNewAccount()
     {
         AccountSetupBasics.actionNewAccount(this);
     }
 
     private void onEditAccount(Account account)
     {
         AccountSettings.actionSettings(this, account);
     }
 
     private void onEditPrefs()
     {
         Prefs.actionPrefs(this);
     }
 
 
     /*
      * This method is called with 'null' for the argument 'account' if
      * all accounts are to be checked. This is handled accordingly in
      * MessagingController.checkMail().
      */
     private void onCheckMail(Account account)
     {
         MessagingController.getInstance(getApplication()).checkMail(this, account, true, true, null);
     }
 
     private void onClearCommands(Account account)
     {
         MessagingController.getInstance(getApplication()).clearAllPending(account);
     }
 
     private void onEmptyTrash(Account account)
     {
         MessagingController.getInstance(getApplication()).emptyTrash(account, null);
     }
 
 
     private void onCompose()
     {
         Account defaultAccount = Preferences.getPreferences(this).getDefaultAccount();
         if (defaultAccount != null)
         {
             MessageCompose.actionCompose(this, defaultAccount);
         }
         else
         {
             onAddNewAccount();
         }
     }
 
     private void onOpenAccount(BaseAccount account)
     {
         if (account instanceof SearchAccount)
         {
             SearchAccount searchAccount = (SearchAccount)account;
             MessageList.actionHandle(this, searchAccount.getDescription(), "", searchAccount.isIntegrate(), searchAccount.getRequiredFlags(), searchAccount.getForbiddenFlags());
         }
         else
         {
             Account realAccount = (Account)account;
             if (K9.FOLDER_NONE.equals(realAccount.getAutoExpandFolderName()))
             {
                 FolderList.actionHandleAccount(this, realAccount);
             }
             else
             {
                 MessageList.actionHandleFolder(this, realAccount, realAccount.getAutoExpandFolderName());
             }
         }
     }
 
     public void onClick(View view)
     {
         if (view.getId() == R.id.next)
         {
             onAddNewAccount();
         }
     }
 
     private void onDeleteAccount(Account account)
     {
         mSelectedContextAccount = account;
         showDialog(DIALOG_REMOVE_ACCOUNT);
     }
 
     @Override
     public Dialog onCreateDialog(int id)
     {
         switch (id)
         {
             case DIALOG_REMOVE_ACCOUNT:
                 return createRemoveAccountDialog();
         }
         return super.onCreateDialog(id);
     }
 
     @Override
     public void onPrepareDialog(int id, Dialog d)
     {
         switch (id)
         {
             case DIALOG_REMOVE_ACCOUNT:
                 AlertDialog alert = (AlertDialog) d;
                 alert.setMessage(getString(R.string.account_delete_dlg_instructions_fmt,
                                            mSelectedContextAccount.getDescription()));
                 break;
         }
 
         super.onPrepareDialog(id, d);
     }
 
 
     private Dialog createRemoveAccountDialog()
     {
         return new AlertDialog.Builder(this)
                .setTitle(R.string.account_delete_dlg_title)
                .setMessage(getString(R.string.account_delete_dlg_instructions_fmt, mSelectedContextAccount.getDescription()))
                .setPositiveButton(R.string.okay_action, new DialogInterface.OnClickListener()
         {
             public void onClick(DialogInterface dialog, int whichButton)
             {
                 dismissDialog(DIALOG_REMOVE_ACCOUNT);
                 try
                 {
                     mSelectedContextAccount.getLocalStore().delete();
                 }
                 catch (Exception e)
                 {
                     // Ignore
                 }
                 MessagingController.getInstance(getApplication()).notifyAccountCancel(Accounts.this, mSelectedContextAccount);
                 Preferences.getPreferences(Accounts.this).deleteAccount(mSelectedContextAccount);
                 K9.setServicesEnabled(Accounts.this);
                 refresh();
             }
         })
                .setNegativeButton(R.string.cancel_action, new DialogInterface.OnClickListener()
         {
             public void onClick(DialogInterface dialog, int whichButton)
             {
                 dismissDialog(DIALOG_REMOVE_ACCOUNT);
             }
         })
                .create();
     }
 
     @Override
     public boolean onContextItemSelected(MenuItem item)
     {
         AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo)item.getMenuInfo();
         // submenus don't actually set the menuInfo, so the "advanced"
         // submenu wouldn't work.
         if (menuInfo != null)
         {
             mSelectedContextAccount = (Account)getListView().getItemAtPosition(menuInfo.position);
         }
         switch (item.getItemId())
         {
             case R.id.delete_account:
                 onDeleteAccount(mSelectedContextAccount);
                 break;
             case R.id.edit_account:
                 onEditAccount(mSelectedContextAccount);
                 break;
             case R.id.open:
                 onOpenAccount(mSelectedContextAccount);
                 break;
             case R.id.check_mail:
                 onCheckMail(mSelectedContextAccount);
                 break;
             case R.id.clear_pending:
                 onClearCommands(mSelectedContextAccount);
                 break;
             case R.id.empty_trash:
                 onEmptyTrash(mSelectedContextAccount);
                 break;
             case R.id.compact:
                 onCompact(mSelectedContextAccount);
                 break;
             case R.id.clear:
                 onClear(mSelectedContextAccount);
                 break;
         }
         return true;
     }
 
 
 
     private void onCompact(Account account)
     {
         mHandler.workingAccount(account, R.string.compacting_account);
         MessagingController.getInstance(getApplication()).compact(account, null);
     }
 
     private void onClear(Account account)
     {
         mHandler.workingAccount(account, R.string.clearing_account);
         MessagingController.getInstance(getApplication()).clear(account, null);
     }
 
 
     public void onItemClick(AdapterView<?> parent, View view, int position, long id)
     {
         BaseAccount account = (BaseAccount)parent.getItemAtPosition(position);
         onOpenAccount(account);
     }
 
     @Override
     public boolean onOptionsItemSelected(MenuItem item)
     {
         switch (item.getItemId())
         {
             case R.id.add_new_account:
                 onAddNewAccount();
                 break;
             case R.id.edit_prefs:
                 onEditPrefs();
                 break;
             case R.id.check_mail:
                 onCheckMail(null);
                 break;
             case R.id.compose:
                 onCompose();
                 break;
             case R.id.about:
                 onAbout();
                 break;
             case R.id.search:
                 onSearchRequested();
                 break;
             default:
                 return super.onOptionsItemSelected(item);
         }
         return true;
     }
 
     private void onAbout()
     {
         String appName = getString(R.string.app_name);
         WebView wv = new WebView(this);
        String html = "<meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\" />" + 
                      "<h1>" + String.format(getString(R.string.about_title_fmt),
                                              "<a href=\"" + getString(R.string.app_webpage_url) + "\">" + appName + "</a>") + "</h1>" +
                       "<p>" + appName + " " +
                       String.format(getString(R.string.debug_version_fmt),
                                     getVersionNumber()) + "</p>" +
                       "<p>" + String.format(getString(R.string.app_authors_fmt),
                                             getString(R.string.app_authors)) + "</p>" +
                       "<p>" + String.format(getString(R.string.app_revision_fmt),
                                             "<a href=\"" + getString(R.string.app_revision_url) + "\">" +
                                             getString(R.string.app_revision_url) + "</a></p>");
         wv.loadData(html, "text/html", "utf-8");
         new AlertDialog.Builder(this)
         .setView(wv)
         .setCancelable(true)
         .setPositiveButton(R.string.okay_action, new DialogInterface.OnClickListener()
         {
             public void onClick(DialogInterface d, int c)
             {
                 d.dismiss();
             }
         })
         .show();
     }
 
     /**
      * Get current version number.
      *
      * @return String version
      */
     private String getVersionNumber()
     {
         String version = "?";
         try
         {
             PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
             version = pi.versionName;
         }
         catch (PackageManager.NameNotFoundException e)
         {
             //Log.e(TAG, "Package name not found", e);
         };
         return version;
     }
 
     public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id)
     {
         return true;
     }
 
     @Override
     public boolean onCreateOptionsMenu(Menu menu)
     {
         super.onCreateOptionsMenu(menu);
         getMenuInflater().inflate(R.menu.accounts_option, menu);
         return true;
     }
 
     @Override
     public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
     {
         super.onCreateContextMenu(menu, v, menuInfo);
         menu.setHeaderTitle(R.string.accounts_context_menu_title);
         getMenuInflater().inflate(R.menu.accounts_context, menu);
         
         AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
         BaseAccount account =  mAdapter.getItem(info.position);
         if (account instanceof SearchAccount)
         {
             for (int i = 0; i < menu.size(); i++)
             {
                 MenuItem item = menu.getItem(i);
                 if (item.getItemId() != R.id.open)
                 {
                     item.setVisible(false);
                 }
             }
         }
     }
 
     class AccountsAdapter extends ArrayAdapter<BaseAccount>
     {
         public AccountsAdapter(BaseAccount[] accounts)
         {
             super(Accounts.this, 0, accounts);
         }
 
         @Override
         public View getView(int position, View convertView, ViewGroup parent)
         {
             BaseAccount account = getItem(position);
             View view;
             if (convertView != null)
             {
                 view = convertView;
             }
             else
             {
                 view = getLayoutInflater().inflate(R.layout.accounts_item, parent, false);
             }
             AccountViewHolder holder = (AccountViewHolder) view.getTag();
             if (holder == null)
             {
                 holder = new AccountViewHolder();
                 holder.description = (TextView) view.findViewById(R.id.description);
                 holder.email = (TextView) view.findViewById(R.id.email);
                 holder.newMessageCount = (TextView) view.findViewById(R.id.new_message_count);
                 holder.flaggedMessageCount = (TextView) view.findViewById(R.id.flagged_message_count);
 
                 holder.chip = view.findViewById(R.id.chip);
 
                 view.setTag(holder);
             }
             AccountStats stats = accountStats.get(account.getUuid());
             
             if (stats != null && account instanceof Account)
             {
                 holder.email.setText(SizeFormatter.formatSize(Accounts.this, stats.size));
             }
             else
             {
                 holder.email.setText(account.getEmail());
             }
             
             String description = account.getDescription();
             if (description == null || description.length() == 0)
             {
                 description = account.getEmail();
             }
 
             holder.description.setText(description);
             
             if (account.getEmail().equals(account.getDescription()))
             {
                 holder.email.setVisibility(View.GONE);
             }
             
             Integer unreadMessageCount = null;
             if (stats != null)
             {
                 unreadMessageCount = stats.unreadMessageCount;
                 holder.newMessageCount.setText(Integer.toString(unreadMessageCount));
                 holder.newMessageCount.setVisibility(unreadMessageCount > 0 ? View.VISIBLE : View.GONE);
                 
                 holder.flaggedMessageCount.setText(Integer.toString(stats.flaggedMessageCount));
                 holder.flaggedMessageCount.setVisibility(K9.messageListStars() && stats.flaggedMessageCount > 0 ? View.VISIBLE : View.GONE);
             }
             else
             {
                 holder.newMessageCount.setVisibility(View.GONE);
                 holder.flaggedMessageCount.setVisibility(View.GONE);
             }
             if (account instanceof Account)
             {
                 Account realAccount = (Account)account;
                 holder.chip.setBackgroundResource(K9.COLOR_CHIP_RES_IDS[realAccount.getAccountNumber() % K9.COLOR_CHIP_RES_IDS.length]);
                 if (unreadMessageCount == null)
                 {
                     holder.chip.getBackground().setAlpha(0);
                 }
                 else if (unreadMessageCount == 0)
                 {
                     holder.chip.getBackground().setAlpha(127);
                 }
                 else
                 {
                     holder.chip.getBackground().setAlpha(255);
                 }
 
             }
             else
             {
                 holder.chip.getBackground().setAlpha(0);
             }
 
             return view;
         }
 
         class AccountViewHolder
         {
             public TextView description;
             public TextView email;
             public TextView newMessageCount;
             public TextView flaggedMessageCount;
             public View chip;
         }
     }
 }
