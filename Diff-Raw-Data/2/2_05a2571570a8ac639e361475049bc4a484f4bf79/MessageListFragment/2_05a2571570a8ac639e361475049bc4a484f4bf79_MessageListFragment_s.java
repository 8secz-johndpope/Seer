 package com.fsck.k9.fragment;
 
 import java.text.DateFormat;
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.Comparator;
 import java.util.Date;
 import java.util.EnumMap;
 import java.util.HashMap;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.Map;
 import java.util.concurrent.Future;
 
 import android.app.Activity;
 import android.content.Context;
 import android.content.Intent;
 import android.content.SharedPreferences.Editor;
 import android.database.Cursor;
 import android.graphics.Color;
 import android.graphics.Typeface;
 import android.graphics.drawable.Drawable;
 import android.net.ConnectivityManager;
 import android.net.NetworkInfo;
 import android.net.Uri;
 import android.os.Bundle;
 import android.os.Handler;
 import android.support.v4.app.DialogFragment;
 import android.support.v4.app.LoaderManager;
 import android.support.v4.app.LoaderManager.LoaderCallbacks;
 import android.support.v4.content.CursorLoader;
 import android.support.v4.content.Loader;
 import android.support.v4.widget.CursorAdapter;
 import android.text.Spannable;
 import android.text.SpannableStringBuilder;
 import android.text.style.AbsoluteSizeSpan;
 import android.text.style.ForegroundColorSpan;
 import android.util.Log;
 import android.util.SparseBooleanArray;
 import android.util.TypedValue;
 import android.view.ContextMenu;
 import android.view.ContextMenu.ContextMenuInfo;
 import android.view.LayoutInflater;
 import android.view.MotionEvent;
 import android.view.View;
 import android.view.ViewGroup;
 import android.widget.AdapterView;
 import android.widget.AdapterView.AdapterContextMenuInfo;
 import android.widget.AdapterView.OnItemClickListener;
 import android.widget.ListView;
 import android.widget.ProgressBar;
 import android.widget.TextView;
 import android.widget.Toast;
 
 import com.actionbarsherlock.app.SherlockFragment;
 import com.actionbarsherlock.view.ActionMode;
 import com.actionbarsherlock.view.Menu;
 import com.actionbarsherlock.view.MenuInflater;
 import com.actionbarsherlock.view.MenuItem;
 import com.actionbarsherlock.view.Window;
 import com.fsck.k9.Account;
 import com.fsck.k9.Account.SortType;
 import com.fsck.k9.AccountStats;
 import com.fsck.k9.FontSizes;
 import com.fsck.k9.K9;
 import com.fsck.k9.Preferences;
 import com.fsck.k9.R;
 import com.fsck.k9.activity.ActivityListener;
 import com.fsck.k9.activity.ChooseFolder;
 import com.fsck.k9.activity.FolderInfoHolder;
 import com.fsck.k9.activity.MessageInfoHolder;
 import com.fsck.k9.activity.MessageReference;
 import com.fsck.k9.controller.MessagingController;
 import com.fsck.k9.fragment.ConfirmationDialogFragment;
 import com.fsck.k9.fragment.ConfirmationDialogFragment.ConfirmationDialogFragmentListener;
 import com.fsck.k9.helper.MessageHelper;
 import com.fsck.k9.helper.MergeCursorWithUniqueId;
 import com.fsck.k9.helper.StringUtils;
 import com.fsck.k9.helper.Utility;
 import com.fsck.k9.mail.Address;
 import com.fsck.k9.mail.Flag;
 import com.fsck.k9.mail.Folder;
 import com.fsck.k9.mail.Message;
 import com.fsck.k9.mail.MessagingException;
 import com.fsck.k9.mail.Folder.OpenMode;
 import com.fsck.k9.mail.store.LocalStore;
 import com.fsck.k9.mail.store.LocalStore.LocalFolder;
 import com.fsck.k9.provider.EmailProvider;
 import com.fsck.k9.provider.EmailProvider.MessageColumns;
 import com.fsck.k9.provider.EmailProvider.SpecialColumns;
 import com.fsck.k9.search.ConditionsTreeNode;
 import com.fsck.k9.search.LocalSearch;
 import com.fsck.k9.search.SearchSpecification;
 import com.fsck.k9.search.SearchSpecification.SearchCondition;
 import com.handmark.pulltorefresh.library.PullToRefreshBase;
 import com.handmark.pulltorefresh.library.PullToRefreshListView;
 
 
 public class MessageListFragment extends SherlockFragment implements OnItemClickListener,
         ConfirmationDialogFragmentListener, LoaderCallbacks<Cursor> {
 
     private static final String[] PROJECTION = {
         MessageColumns.ID,
         MessageColumns.UID,
         MessageColumns.INTERNAL_DATE,
         MessageColumns.SUBJECT,
         MessageColumns.DATE,
         MessageColumns.SENDER_LIST,
         MessageColumns.TO_LIST,
         MessageColumns.CC_LIST,
         MessageColumns.FLAGS,
         MessageColumns.ATTACHMENT_COUNT,
         MessageColumns.FOLDER_ID,
         MessageColumns.PREVIEW,
         MessageColumns.THREAD_ROOT,
         MessageColumns.THREAD_PARENT,
         SpecialColumns.ACCOUNT_UUID
     };
 
     private static final int ID_COLUMN = 0;
     private static final int UID_COLUMN = 1;
     private static final int INTERNAL_DATE_COLUMN = 2;
     private static final int SUBJECT_COLUMN = 3;
     private static final int DATE_COLUMN = 4;
     private static final int SENDER_LIST_COLUMN = 5;
     private static final int TO_LIST_COLUMN = 6;
     private static final int CC_LIST_COLUMN = 7;
     private static final int FLAGS_COLUMN = 8;
     private static final int ATTACHMENT_COUNT_COLUMN = 9;
     private static final int FOLDER_ID_COLUMN = 10;
     private static final int PREVIEW_COLUMN = 11;
     private static final int THREAD_ROOT_COLUMN = 12;
     private static final int THREAD_PARENT_COLUMN = 13;
     private static final int ACCOUNT_UUID_COLUMN = 14;
 
 
     public static MessageListFragment newInstance(LocalSearch search, boolean remoteSearch) {
         MessageListFragment fragment = new MessageListFragment();
         Bundle args = new Bundle();
         args.putParcelable(ARG_SEARCH, search);
         args.putBoolean(ARG_REMOTE_SEARCH, remoteSearch);
         fragment.setArguments(args);
         return fragment;
     }
 
     /**
      * Reverses the result of a {@link Comparator}.
      *
      * @param <T>
      */
     public static class ReverseComparator<T> implements Comparator<T> {
         private Comparator<T> mDelegate;
 
         /**
          * @param delegate
          *            Never <code>null</code>.
          */
         public ReverseComparator(final Comparator<T> delegate) {
             mDelegate = delegate;
         }
 
         @Override
         public int compare(final T object1, final T object2) {
             // arg1 & 2 are mixed up, this is done on purpose
             return mDelegate.compare(object2, object1);
         }
 
     }
 
     /**
      * Chains comparator to find a non-0 result.
      *
      * @param <T>
      */
     public static class ComparatorChain<T> implements Comparator<T> {
 
         private List<Comparator<T>> mChain;
 
         /**
          * @param chain
          *            Comparator chain. Never <code>null</code>.
          */
         public ComparatorChain(final List<Comparator<T>> chain) {
             mChain = chain;
         }
 
         @Override
         public int compare(T object1, T object2) {
             int result = 0;
             for (final Comparator<T> comparator : mChain) {
                 result = comparator.compare(object1, object2);
                 if (result != 0) {
                     break;
                 }
             }
             return result;
         }
 
     }
 
     public static class AttachmentComparator implements Comparator<MessageInfoHolder> {
 
         @Override
         public int compare(MessageInfoHolder object1, MessageInfoHolder object2) {
             return (object1.message.hasAttachments() ? 0 : 1) - (object2.message.hasAttachments() ? 0 : 1);
         }
 
     }
 
     public static class FlaggedComparator implements Comparator<MessageInfoHolder> {
 
         @Override
         public int compare(MessageInfoHolder object1, MessageInfoHolder object2) {
             return (object1.flagged ? 0 : 1) - (object2.flagged ? 0 : 1);
         }
 
     }
 
     public static class UnreadComparator implements Comparator<MessageInfoHolder> {
 
         @Override
         public int compare(MessageInfoHolder object1, MessageInfoHolder object2) {
             return (object1.read ? 1 : 0) - (object2.read ? 1 : 0);
         }
 
     }
 
     public static class SenderComparator implements Comparator<MessageInfoHolder> {
 
         @Override
         public int compare(MessageInfoHolder object1, MessageInfoHolder object2) {
             if (object1.compareCounterparty == null) {
                 return (object2.compareCounterparty == null ? 0 : 1);
             } else if (object2.compareCounterparty == null) {
                 return -1;
             } else {
                 return object1.compareCounterparty.toLowerCase().compareTo(object2.compareCounterparty.toLowerCase());
             }
         }
 
     }
 
     public static class DateComparator implements Comparator<MessageInfoHolder> {
 
         @Override
         public int compare(MessageInfoHolder object1, MessageInfoHolder object2) {
             if (object1.compareDate == null) {
                 return (object2.compareDate == null ? 0 : 1);
             } else if (object2.compareDate == null) {
                 return -1;
             } else {
                 return object1.compareDate.compareTo(object2.compareDate);
             }
         }
 
     }
 
     public static class ArrivalComparator implements Comparator<MessageInfoHolder> {
 
         @Override
         public int compare(MessageInfoHolder object1, MessageInfoHolder object2) {
             return object1.compareArrival.compareTo(object2.compareArrival);
         }
 
     }
 
     public static class SubjectComparator implements Comparator<MessageInfoHolder> {
 
         @Override
         public int compare(MessageInfoHolder arg0, MessageInfoHolder arg1) {
             // XXX doesn't respect the Comparator contract since it alters the compared object
             if (arg0.compareSubject == null) {
                 arg0.compareSubject = Utility.stripSubject(arg0.message.getSubject());
             }
             if (arg1.compareSubject == null) {
                 arg1.compareSubject = Utility.stripSubject(arg1.message.getSubject());
             }
             return arg0.compareSubject.compareToIgnoreCase(arg1.compareSubject);
         }
 
     }
 
 
     private static final int ACTIVITY_CHOOSE_FOLDER_MOVE = 1;
     private static final int ACTIVITY_CHOOSE_FOLDER_COPY = 2;
 
     private static final String ARG_SEARCH = "searchObject";
     private static final String ARG_REMOTE_SEARCH = "remoteSearch";
     private static final String STATE_LIST_POSITION = "listPosition";
 
     /**
      * Maps a {@link SortType} to a {@link Comparator} implementation.
      */
     private static final Map<SortType, Comparator<MessageInfoHolder>> SORT_COMPARATORS;
 
     static {
         // fill the mapping at class time loading
 
         final Map<SortType, Comparator<MessageInfoHolder>> map = new EnumMap<SortType, Comparator<MessageInfoHolder>>(SortType.class);
         map.put(SortType.SORT_ATTACHMENT, new AttachmentComparator());
         map.put(SortType.SORT_DATE, new DateComparator());
         map.put(SortType.SORT_ARRIVAL, new ArrivalComparator());
         map.put(SortType.SORT_FLAGGED, new FlaggedComparator());
         map.put(SortType.SORT_SENDER, new SenderComparator());
         map.put(SortType.SORT_SUBJECT, new SubjectComparator());
         map.put(SortType.SORT_UNREAD, new UnreadComparator());
 
         // make it immutable to prevent accidental alteration (content is immutable already)
         SORT_COMPARATORS = Collections.unmodifiableMap(map);
     }
 
     private ListView mListView;
     private PullToRefreshListView mPullToRefreshView;
 
     private int mPreviewLines = 0;
 
 
     private MessageListAdapter mAdapter;
     private View mFooterView;
 
     private FolderInfoHolder mCurrentFolder;
 
     private LayoutInflater mInflater;
 
     private MessagingController mController;
 
     private Account mAccount;
     private String[] mAccountUuids;
     private int mUnreadMessageCount = 0;
 
     private Map<Integer, Cursor> mCursors = new HashMap<Integer, Cursor>();
 
     /**
      * Stores the name of the folder that we want to open as soon as possible
      * after load.
      */
     private String mFolderName;
 
     /**
      * If we're doing a search, this contains the query string.
      */
     private boolean mRemoteSearch = false;
     private Future mRemoteSearchFuture = null;
 
     private String mTitle;
     private LocalSearch mSearch = null;
     private boolean mSingleAccountMode;
     private boolean mSingleFolderMode;
 
     private MessageListHandler mHandler = new MessageListHandler();
 
     private SortType mSortType = SortType.SORT_DATE;
     private boolean mSortAscending = true;
     private boolean mSortDateAscending = false;
     private boolean mSenderAboveSubject = false;
 
     private int mSelectedCount = 0;
     private SparseBooleanArray mSelected;
 
     private FontSizes mFontSizes = K9.getFontSizes();
 
     private MenuItem mRefreshMenuItem;
     private ActionMode mActionMode;
     private View mActionBarProgressView;
     private Bundle mState = null;
 
     private Boolean mHasConnectivity;
 
     /**
      * Relevant messages for the current context when we have to remember the
      * chosen messages between user interactions (eg. Selecting a folder for
      * move operation)
      */
     private Message[] mActiveMessages;
 
     /* package visibility for faster inner class access */
     MessageHelper mMessageHelper;
 
     private ActionModeCallback mActionModeCallback = new ActionModeCallback();
 
 
     private MessageListFragmentListener mFragmentListener;
 
 
     private DateFormat mTimeFormat;
 
     //TODO: make this a setting
     private boolean mThreadViewEnabled = true;
 
     private long mThreadId;
 
 
     private Context mContext;
 
     private final ActivityListener mListener = new MessageListActivityListener();
 
     private Preferences mPreferences;
 
     /**
      * This class is used to run operations that modify UI elements in the UI thread.
      *
      * <p>We are using convenience methods that add a {@link android.os.Message} instance or a
      * {@link Runnable} to the message queue.</p>
      *
      * <p><strong>Note:</strong> If you add a method to this class make sure you don't accidentally
      * perform the operation in the calling thread.</p>
      */
     class MessageListHandler extends Handler {
         private static final int ACTION_SORT_MESSAGES = 1;
         private static final int ACTION_FOLDER_LOADING = 2;
         private static final int ACTION_REFRESH_TITLE = 3;
         private static final int ACTION_PROGRESS = 4;
 
 
         public void sortMessages() {
             android.os.Message msg = android.os.Message.obtain(this, ACTION_SORT_MESSAGES);
             sendMessage(msg);
         }
 
         public void folderLoading(String folder, boolean loading) {
             android.os.Message msg = android.os.Message.obtain(this, ACTION_FOLDER_LOADING,
                     (loading) ? 1 : 0, 0, folder);
             sendMessage(msg);
         }
 
         public void refreshTitle() {
             android.os.Message msg = android.os.Message.obtain(this, ACTION_REFRESH_TITLE);
             sendMessage(msg);
         }
 
         public void progress(final boolean progress) {
             android.os.Message msg = android.os.Message.obtain(this, ACTION_PROGRESS,
                     (progress) ? 1 : 0, 0);
             sendMessage(msg);
         }
 
         public void updateFooter(final String message, final boolean showProgress) {
             //TODO: use message
             post(new Runnable() {
                 @Override
                 public void run() {
                     updateFooter(message, showProgress);
                 }
             });
         }
 
         @Override
         public void handleMessage(android.os.Message msg) {
             switch (msg.what) {
                 case ACTION_SORT_MESSAGES: {
                     mAdapter.sortMessages();
                     break;
                 }
                 case ACTION_FOLDER_LOADING: {
                     String folder = (String) msg.obj;
                     boolean loading = (msg.arg1 == 1);
                     MessageListFragment.this.folderLoading(folder, loading);
                     break;
                 }
                 case ACTION_REFRESH_TITLE: {
                     MessageListFragment.this.refreshTitle();
                     break;
                 }
                 case ACTION_PROGRESS: {
                     boolean progress = (msg.arg1 == 1);
                     MessageListFragment.this.progress(progress);
                     break;
                 }
             }
         }
     }
 
     /**
      * @return The comparator to use to display messages in an ordered
      *         fashion. Never <code>null</code>.
      */
     protected Comparator<MessageInfoHolder> getComparator() {
         final List<Comparator<MessageInfoHolder>> chain = new ArrayList<Comparator<MessageInfoHolder>>(2 /* we add 2 comparators at most */);
 
         {
             // add the specified comparator
             final Comparator<MessageInfoHolder> comparator = SORT_COMPARATORS.get(mSortType);
             if (mSortAscending) {
                 chain.add(comparator);
             } else {
                 chain.add(new ReverseComparator<MessageInfoHolder>(comparator));
             }
         }
 
         {
             // add the date comparator if not already specified
             if (mSortType != SortType.SORT_DATE && mSortType != SortType.SORT_ARRIVAL) {
                 final Comparator<MessageInfoHolder> comparator = SORT_COMPARATORS.get(SortType.SORT_DATE);
                 if (mSortDateAscending) {
                     chain.add(comparator);
                 } else {
                     chain.add(new ReverseComparator<MessageInfoHolder>(comparator));
                 }
             }
         }
 
         // build the comparator chain
         final Comparator<MessageInfoHolder> chainComparator = new ComparatorChain<MessageInfoHolder>(chain);
 
         return chainComparator;
     }
 
     private void folderLoading(String folder, boolean loading) {
         if (mCurrentFolder != null && mCurrentFolder.name.equals(folder)) {
             mCurrentFolder.loading = loading;
         }
         updateFooterView();
     }
 
     private void refreshTitle() {
         setWindowTitle();
         if (!mRemoteSearch) {
             setWindowProgress();
         }
     }
 
     private void setWindowProgress() {
         int level = Window.PROGRESS_END;
 
         if (mCurrentFolder != null && mCurrentFolder.loading && mListener.getFolderTotal() > 0) {
             int divisor = mListener.getFolderTotal();
             if (divisor != 0) {
                 level = (Window.PROGRESS_END / divisor) * (mListener.getFolderCompleted()) ;
                 if (level > Window.PROGRESS_END) {
                     level = Window.PROGRESS_END;
                 }
             }
         }
 
         mFragmentListener.setMessageListProgress(level);
     }
 
     private void setWindowTitle() {
         // regular folder content display
         if (mSingleFolderMode) {
             Activity activity = getActivity();
             String displayName = FolderInfoHolder.getDisplayName(activity, mAccount,
                 mFolderName);
 
             mFragmentListener.setMessageListTitle(displayName);
 
             String operation = mListener.getOperation(activity, getTimeFormat()).trim();
             if (operation.length() < 1) {
                 mFragmentListener.setMessageListSubTitle(mAccount.getEmail());
             } else {
                 mFragmentListener.setMessageListSubTitle(operation);
             }
         } else {
             // query result display.  This may be for a search folder as opposed to a user-initiated search.
             if (mTitle != null) {
                 // This was a search folder; the search folder has overridden our title.
                 mFragmentListener.setMessageListTitle(mTitle);
             } else {
                 // This is a search result; set it to the default search result line.
                 mFragmentListener.setMessageListTitle(getString(R.string.search_results));
             }
 
             mFragmentListener.setMessageListSubTitle(null);
         }
 
         // set unread count
         if (mUnreadMessageCount == 0) {
             mFragmentListener.setUnreadCount(0);
         } else {
             if (!mSingleFolderMode && mTitle == null) {
                 // The unread message count is easily confused
                 // with total number of messages in the search result, so let's hide it.
                 mFragmentListener.setUnreadCount(0);
             } else {
                 mFragmentListener.setUnreadCount(mUnreadMessageCount);
             }
         }
     }
 
     private void setupFormats() {
         mTimeFormat = android.text.format.DateFormat.getTimeFormat(mContext);
     }
 
     private DateFormat getTimeFormat() {
         return mTimeFormat;
     }
 
     private void progress(final boolean progress) {
         // Make sure we don't try this before the menu is initialized
         // this could happen while the activity is initialized.
         if (mRefreshMenuItem != null) {
             if (progress) {
                 mRefreshMenuItem.setActionView(mActionBarProgressView);
             } else {
                 mRefreshMenuItem.setActionView(null);
             }
         }
 
         if (mPullToRefreshView != null && !progress) {
             mPullToRefreshView.onRefreshComplete();
         }
     }
 
     @Override
     public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
         if (view == mFooterView) {
             if (mCurrentFolder != null && !mRemoteSearch) {
                 mController.loadMoreMessages(mAccount, mFolderName, null);
             } /*else if (mRemoteSearch && mAdapter.mExtraSearchResults != null && mAdapter.mExtraSearchResults.size() > 0 && mSearchAccount != null) {
                 int numResults = mAdapter.mExtraSearchResults.size();
                 Context appContext = getActivity().getApplicationContext();
                 Account account = Preferences.getPreferences(appContext).getAccount(mSearchAccount);
                 if (account == null) {
                     updateFooter("", false);
                     return;
                 }
                 int limit = mAccount.getRemoteSearchNumResults();
                 List<Message> toProcess = mAdapter.mExtraSearchResults;
                 if (limit > 0 && numResults > limit) {
                     toProcess = toProcess.subList(0, limit);
                     mAdapter.mExtraSearchResults = mAdapter.mExtraSearchResults.subList(limit, mAdapter.mExtraSearchResults.size());
                 } else {
                     mAdapter.mExtraSearchResults = null;
                     updateFooter("", false);
                 }
                 mController.loadSearchResults(mAccount, mCurrentFolder.name, toProcess, mListener);
             }*/
             return;
         }
 
         Cursor cursor = (Cursor) parent.getItemAtPosition(position);
         if (mSelectedCount > 0) {
             toggleMessageSelect(position);
 //        } else if (message.threadCount > 1) {
 //            Folder folder = message.message.getFolder();
 //            long rootId = ((LocalMessage) message.message).getRootId();
 //            mFragmentListener.showThread(folder.getAccount(), folder.getName(), rootId);
         } else {
             Account account = getAccountFromCursor(cursor);
 
             long folderId = cursor.getLong(FOLDER_ID_COLUMN);
             String folderName = getFolderNameById(account, folderId);
 
             MessageReference ref = new MessageReference();
             ref.accountUuid = account.getUuid();
             ref.folderName = folderName;
             ref.uid = cursor.getString(UID_COLUMN);
             onOpenMessage(ref);
         }
     }
 
     @Override
     public void onAttach(Activity activity) {
         super.onAttach(activity);
 
         mContext = activity.getApplicationContext();
 
         try {
             mFragmentListener = (MessageListFragmentListener) activity;
         } catch (ClassCastException e) {
             throw new ClassCastException(activity.getClass() +
                     " must implement MessageListFragmentListener");
         }
     }
 
     @Override
     public void onCreate(Bundle savedInstanceState) {
         super.onCreate(savedInstanceState);
 
         mPreferences = Preferences.getPreferences(getActivity().getApplicationContext());
         mController = MessagingController.getInstance(getActivity().getApplication());
 
         mPreviewLines = K9.messageListPreviewLines();
 
         decodeArguments();
     }
 
     @Override
     public View onCreateView(LayoutInflater inflater, ViewGroup container,
             Bundle savedInstanceState) {
 
         mInflater = inflater;
 
         View view = inflater.inflate(R.layout.message_list_fragment, container, false);
 
         mActionBarProgressView = inflater.inflate(R.layout.actionbar_indeterminate_progress_actionview, null);
 
         mPullToRefreshView = (PullToRefreshListView) view.findViewById(R.id.message_list);
 
         initializeLayout();
         mListView.setVerticalFadingEdgeEnabled(false);
 
         return view;
     }
 
     @Override
     public void onActivityCreated(Bundle savedInstanceState) {
         super.onActivityCreated(savedInstanceState);
 
         mMessageHelper = MessageHelper.getInstance(getActivity());
 
         initializeMessageList();
 
         LoaderManager loaderManager = getLoaderManager();
         for (int i = 0, len = mAccountUuids.length; i < len; i++) {
             loaderManager.initLoader(i, null, this);
         }
     }
 
     private void decodeArguments() {
         Bundle args = getArguments();
 
         mRemoteSearch = args.getBoolean(ARG_REMOTE_SEARCH, false);
         mSearch = args.getParcelable(ARG_SEARCH);
        mTitle = args.getString(mSearch.getName());
 
         String[] accountUuids = mSearch.getAccountUuids();
 
         mSingleAccountMode = false;
         if (accountUuids.length == 1 && !accountUuids[0].equals(SearchSpecification.ALL_ACCOUNTS)) {
             mSingleAccountMode = true;
             mAccount = mPreferences.getAccount(accountUuids[0]);
         }
 
         mSingleFolderMode = false;
         if (mSingleAccountMode && (mSearch.getFolderNames().size() == 1)) {
             mSingleFolderMode = true;
             mFolderName = mSearch.getFolderNames().get(0);
             mCurrentFolder = getFolder(mFolderName, mAccount);
         }
 
         if (mSingleAccountMode) {
             mAccountUuids = new String[] { mAccount.getUuid() };
         } else {
             if (accountUuids.length == 1 &&
                     accountUuids[0].equals(SearchSpecification.ALL_ACCOUNTS)) {
 
                 Account[] accounts = mPreferences.getAccounts();
 
                 mAccountUuids = new String[accounts.length];
                 for (int i = 0, len = accounts.length; i < len; i++) {
                     mAccountUuids[i] = accounts[i].getUuid();
                 }
             } else {
                 mAccountUuids = accountUuids;
             }
         }
     }
 
     private void initializeMessageList() {
         mAdapter = new MessageListAdapter();
 
         if (mFolderName != null) {
             mCurrentFolder = getFolder(mFolderName, mAccount);
         }
 
         // Hide "Load up to x more" footer for search views
         mFooterView.setVisibility((!mSingleFolderMode) ? View.GONE : View.VISIBLE);
 
         mController = MessagingController.getInstance(getActivity().getApplication());
         mListView.setAdapter(mAdapter);
     }
 
     private FolderInfoHolder getFolder(String folder, Account account) {
         LocalFolder local_folder = null;
         try {
             LocalStore localStore = account.getLocalStore();
             local_folder = localStore.getFolder(folder);
             return new FolderInfoHolder(mContext, local_folder, account);
         } catch (Exception e) {
             Log.e(K9.LOG_TAG, "getFolder(" + folder + ") goes boom: ", e);
             return null;
         } finally {
             if (local_folder != null) {
                 local_folder.close();
             }
         }
     }
 
     private String getFolderNameById(Account account, long folderId) {
         try {
             LocalStore localStore = account.getLocalStore();
             LocalFolder localFolder = localStore.getFolderById(folderId);
             localFolder.open(OpenMode.READ_ONLY);
             return localFolder.getName();
         } catch (Exception e) {
             Log.e(K9.LOG_TAG, "getFolderNameById() failed.", e);
             return null;
         }
     }
 
     @Override
     public void onPause() {
         super.onPause();
         mController.removeListener(mListener);
         saveListState();
     }
 
     public void saveListState() {
         mState = new Bundle();
         mState.putInt(STATE_LIST_POSITION, mListView.getSelectedItemPosition());
     }
 
     public void restoreListState() {
         if (mState == null) {
             return;
         }
 
         int pos = mState.getInt(STATE_LIST_POSITION, ListView.INVALID_POSITION);
 
         if (pos >= mListView.getCount()) {
             pos = mListView.getCount() - 1;
         }
 
         if (pos == ListView.INVALID_POSITION) {
             mListView.setSelected(false);
         } else {
             mListView.setSelection(pos);
         }
     }
 
     /**
      * On resume we refresh messages for the folder that is currently open.
      * This guarantees that things like unread message count and read status
      * are updated.
      */
     @Override
     public void onResume() {
         super.onResume();
 
         setupFormats();
 
         Context appContext = getActivity().getApplicationContext();
 
         mSenderAboveSubject = K9.messageListSenderAboveSubject();
 
         // Check if we have connectivity.  Cache the value.
         if (mHasConnectivity == null) {
             final ConnectivityManager connectivityManager =
                 (ConnectivityManager) getActivity().getApplication().getSystemService(
                         Context.CONNECTIVITY_SERVICE);
             final NetworkInfo netInfo = connectivityManager.getActiveNetworkInfo();
             if (netInfo != null && netInfo.getState() == NetworkInfo.State.CONNECTED) {
                 mHasConnectivity = true;
             } else {
                 mHasConnectivity = false;
             }
         }
 
         if (mSingleFolderMode) {
             if (!mAccount.allowRemoteSearch()) {
                 mPullToRefreshView.setOnRefreshListener(new PullToRefreshBase.OnRefreshListener<ListView>() {
                     @Override
                     public void onRefresh(PullToRefreshBase<ListView> refreshView) {
                         checkMail();
                     }
                 });
             // TODO this has to go! find better remote search integration
             } else {
                 mPullToRefreshView.setOnRefreshListener(new PullToRefreshBase.OnRefreshListener<ListView>() {
                     @Override
                     public void onRefresh(PullToRefreshBase<ListView> refreshView) {
                         mPullToRefreshView.onRefreshComplete();
                         onRemoteSearchRequested();
                     }
                 });
                 mPullToRefreshView.setPullLabel(getString(R.string.pull_to_refresh_remote_search_from_local_search_pull));
                 mPullToRefreshView.setReleaseLabel(getString(R.string.pull_to_refresh_remote_search_from_local_search_release));
             }
         } else {
             mPullToRefreshView.setMode(PullToRefreshBase.Mode.DISABLED);
         }
 
         mController.addListener(mListener);
 
         //Cancel pending new mail notifications when we open an account
         Account[] accountsWithNotification;
 
         Account account = mAccount;
 
         if (account != null) {
             accountsWithNotification = new Account[] { account };
             mSortType = account.getSortType();
             mSortAscending = account.isSortAscending(mSortType);
             mSortDateAscending = account.isSortAscending(SortType.SORT_DATE);
         } else {
             accountsWithNotification = mPreferences.getAccounts();
             mSortType = K9.getSortType();
             mSortAscending = K9.isSortAscending(mSortType);
             mSortDateAscending = K9.isSortAscending(SortType.SORT_DATE);
         }
 
         for (Account accountWithNotification : accountsWithNotification) {
             mController.notifyAccountCancel(appContext, accountWithNotification);
         }
 
         if (mAccount != null && mFolderName != null && !mRemoteSearch) {
             mController.getFolderUnreadMessageCount(mAccount, mFolderName, mListener);
         }
 
         refreshTitle();
     }
 
     private void initializeLayout() {
         mListView = mPullToRefreshView.getRefreshableView();
         mListView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
         mListView.setLongClickable(true);
         mListView.setFastScrollEnabled(true);
         mListView.setScrollingCacheEnabled(false);
         mListView.setOnItemClickListener(this);
         mListView.addFooterView(getFooterView(mListView));
         //mListView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
 
         registerForContextMenu(mListView);
     }
 
     private void onOpenMessage(MessageReference reference) {
         mFragmentListener.openMessage(reference);
     }
 
     public void onCompose() {
         if (!mSingleAccountMode) {
             /*
              * If we have a query string, we don't have an account to let
              * compose start the default action.
              */
             mFragmentListener.onCompose(null);
         } else {
             mFragmentListener.onCompose(mAccount);
         }
     }
 
     public void onReply(Message message) {
         mFragmentListener.onReply(message);
     }
 
     public void onReplyAll(Message message) {
         mFragmentListener.onReplyAll(message);
     }
 
     public void onForward(Message message) {
         mFragmentListener.onForward(message);
     }
 
     public void onResendMessage(Message message) {
         mFragmentListener.onResendMessage(message);
     }
 
     public void changeSort(SortType sortType) {
         Boolean sortAscending = (mSortType == sortType) ? !mSortAscending : null;
         changeSort(sortType, sortAscending);
     }
 
     /**
      * User has requested a remote search.  Setup the bundle and start the intent.
      */
     public void onRemoteSearchRequested() {
         String searchAccount;
         String searchFolder;
 
         searchAccount = mAccount.getUuid();
         searchFolder = mCurrentFolder.name;
 
         mFragmentListener.remoteSearch(searchAccount, searchFolder, mSearch.getRemoteSearchArguments());
     }
 
     /**
      * Change the sort type and sort order used for the message list.
      *
      * @param sortType
      *         Specifies which field to use for sorting the message list.
      * @param sortAscending
      *         Specifies the sort order. If this argument is {@code null} the default search order
      *         for the sort type is used.
      */
     // FIXME: Don't save the changes in the UI thread
     private void changeSort(SortType sortType, Boolean sortAscending) {
         mSortType = sortType;
 
         Account account = mAccount;
 
         if (account != null) {
             account.setSortType(mSortType);
 
             if (sortAscending == null) {
                 mSortAscending = account.isSortAscending(mSortType);
             } else {
                 mSortAscending = sortAscending;
             }
             account.setSortAscending(mSortType, mSortAscending);
             mSortDateAscending = account.isSortAscending(SortType.SORT_DATE);
 
             account.save(mPreferences);
         } else {
             K9.setSortType(mSortType);
 
             if (sortAscending == null) {
                 mSortAscending = K9.isSortAscending(mSortType);
             } else {
                 mSortAscending = sortAscending;
             }
             K9.setSortAscending(mSortType, mSortAscending);
             mSortDateAscending = K9.isSortAscending(SortType.SORT_DATE);
 
             Editor editor = mPreferences.getPreferences().edit();
             K9.save(editor);
             editor.commit();
         }
 
         reSort();
     }
 
     private void reSort() {
         int toastString = mSortType.getToast(mSortAscending);
 
         Toast toast = Toast.makeText(getActivity(), toastString, Toast.LENGTH_SHORT);
         toast.show();
 
         mAdapter.sortMessages();
     }
 
     public void onCycleSort() {
         SortType[] sorts = SortType.values();
         int curIndex = 0;
 
         for (int i = 0; i < sorts.length; i++) {
             if (sorts[i] == mSortType) {
                 curIndex = i;
                 break;
             }
         }
 
         curIndex++;
 
         if (curIndex == sorts.length) {
             curIndex = 0;
         }
 
         changeSort(sorts[curIndex]);
     }
 
     private void onDelete(Message message) {
         onDelete(new Message[] { message });
     }
 
     private void onDelete(Message[] messages) {
         mController.deleteMessages(messages, null);
     }
 
     @Override
     public void onActivityResult(int requestCode, int resultCode, Intent data) {
         if (resultCode != Activity.RESULT_OK) {
             return;
         }
 
         switch (requestCode) {
         case ACTIVITY_CHOOSE_FOLDER_MOVE:
         case ACTIVITY_CHOOSE_FOLDER_COPY: {
             if (data == null) {
                 return;
             }
 
             final String destFolderName = data.getStringExtra(ChooseFolder.EXTRA_NEW_FOLDER);
             final Message[] messages = mActiveMessages;
 
             if (destFolderName != null) {
 
                 mActiveMessages = null; // don't need it any more
 
                 final Account account = messages[0].getFolder().getAccount();
                 account.setLastSelectedFolderName(destFolderName);
 
                 switch (requestCode) {
                 case ACTIVITY_CHOOSE_FOLDER_MOVE:
                     move(messages, destFolderName);
                     break;
 
                 case ACTIVITY_CHOOSE_FOLDER_COPY:
                     copy(messages, destFolderName);
                     break;
                 }
             }
             break;
         }
         }
     }
 
     public void onExpunge() {
         if (mCurrentFolder != null) {
             onExpunge(mAccount, mCurrentFolder.name);
         }
     }
 
     private void onExpunge(final Account account, String folderName) {
         mController.expunge(account, folderName, null);
     }
 
     private void showDialog(int dialogId) {
         DialogFragment fragment;
         switch (dialogId) {
             case R.id.dialog_confirm_spam: {
                 String title = getString(R.string.dialog_confirm_spam_title);
 
                 int selectionSize = mActiveMessages.length;
                 String message = getResources().getQuantityString(
                         R.plurals.dialog_confirm_spam_message, selectionSize,
                         Integer.valueOf(selectionSize));
 
                 String confirmText = getString(R.string.dialog_confirm_spam_confirm_button);
                 String cancelText = getString(R.string.dialog_confirm_spam_cancel_button);
 
                 fragment = ConfirmationDialogFragment.newInstance(dialogId, title, message,
                         confirmText, cancelText);
                 break;
             }
             default: {
                 throw new RuntimeException("Called showDialog(int) with unknown dialog id.");
             }
         }
 
         fragment.setTargetFragment(this, dialogId);
         fragment.show(getFragmentManager(), getDialogTag(dialogId));
     }
 
     private String getDialogTag(int dialogId) {
         return String.format("dialog-%d", dialogId);
     }
 
     @Override
     public boolean onOptionsItemSelected(MenuItem item) {
         int itemId = item.getItemId();
         switch (itemId) {
         case R.id.set_sort_date: {
             changeSort(SortType.SORT_DATE);
             return true;
         }
         case R.id.set_sort_arrival: {
             changeSort(SortType.SORT_ARRIVAL);
             return true;
         }
         case R.id.set_sort_subject: {
             changeSort(SortType.SORT_SUBJECT);
             return true;
         }
         case R.id.set_sort_sender: {
             changeSort(SortType.SORT_SENDER);
             return true;
         }
         case R.id.set_sort_flag: {
             changeSort(SortType.SORT_FLAGGED);
             return true;
         }
         case R.id.set_sort_unread: {
             changeSort(SortType.SORT_UNREAD);
             return true;
         }
         case R.id.set_sort_attach: {
             changeSort(SortType.SORT_ATTACHMENT);
             return true;
         }
         case R.id.select_all: {
             selectAll();
             return true;
         }
         }
 
         if (!mSingleAccountMode) {
             // None of the options after this point are "safe" for search results
             //TODO: This is not true for "unread" and "starred" searches in regular folders
             return false;
         }
 
         switch (itemId) {
         case R.id.send_messages: {
             onSendPendingMessages();
             return true;
         }
         case R.id.expunge: {
             if (mCurrentFolder != null) {
                 onExpunge(mAccount, mCurrentFolder.name);
             }
             return true;
         }
         default: {
             return super.onOptionsItemSelected(item);
         }
         }
     }
 
     public void onSendPendingMessages() {
         mController.sendPendingMessages(mAccount, null);
     }
 
     @Override
     public boolean onContextItemSelected(android.view.MenuItem item) {
         AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
         int adapterPosition = listViewToAdapterPosition(info.position);
         Message message = getMessageAtPosition(adapterPosition);
 
         switch (item.getItemId()) {
             case R.id.reply: {
                 onReply(message);
                 break;
             }
             case R.id.reply_all: {
                 onReplyAll(message);
                 break;
             }
             case R.id.forward: {
                 onForward(message);
                 break;
             }
             case R.id.send_again: {
                 onResendMessage(message);
                 mSelectedCount = 0;
                 break;
             }
             case R.id.same_sender: {
                 Cursor cursor = (Cursor) mAdapter.getItem(adapterPosition);
                 String senderAddress = getSenderAddressFromCursor(cursor);
                 if (senderAddress != null) {
                     mFragmentListener.showMoreFromSameSender(senderAddress);
                 }
                 break;
             }
             case R.id.delete: {
                 onDelete(message);
                 break;
             }
             case R.id.mark_as_read: {
                 setFlag(message, Flag.SEEN, true);
                 break;
             }
             case R.id.mark_as_unread: {
                 setFlag(message, Flag.SEEN, false);
                 break;
             }
             case R.id.flag: {
                 setFlag(message, Flag.FLAGGED, true);
                 break;
             }
             case R.id.unflag: {
                 setFlag(message, Flag.FLAGGED, false);
                 break;
             }
 
             // only if the account supports this
             case R.id.archive: {
                 onArchive(message);
                 break;
             }
             case R.id.spam: {
                 onSpam(message);
                 break;
             }
             case R.id.move: {
                 onMove(message);
                 break;
             }
             case R.id.copy: {
                 onCopy(message);
                 break;
             }
         }
 
         return true;
     }
 
 
     private String getSenderAddressFromCursor(Cursor cursor) {
         String fromList = cursor.getString(SENDER_LIST_COLUMN);
         Address[] fromAddrs = Address.unpack(fromList);
         return (fromAddrs.length > 0) ? fromAddrs[0].getAddress() : null;
     }
 
     @Override
     public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
         super.onCreateContextMenu(menu, v, menuInfo);
 
         AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
         Cursor cursor = (Cursor) mListView.getItemAtPosition(info.position);
 
         if (cursor == null) {
             return;
         }
 
         getActivity().getMenuInflater().inflate(R.menu.message_list_item_context, menu);
 
         Account account = getAccountFromCursor(cursor);
 
         String subject = cursor.getString(SUBJECT_COLUMN);
         String flagList = cursor.getString(FLAGS_COLUMN);
         String[] flags = flagList.split(",");
         boolean read = false;
         boolean flagged = false;
         for (int i = 0, len = flags.length; i < len; i++) {
             try {
                 switch (Flag.valueOf(flags[i])) {
                     case SEEN: {
                         read = true;
                         break;
                     }
                     case FLAGGED: {
                         flagged = true;
                         break;
                     }
                     default: {
                         // We don't care about the other flags
                     }
                 }
             } catch (Exception e) { /* ignore */ }
         }
 
         menu.setHeaderTitle(subject);
 
         if (read) {
             menu.findItem(R.id.mark_as_read).setVisible(false);
         } else {
             menu.findItem(R.id.mark_as_unread).setVisible(false);
         }
 
         if (flagged) {
             menu.findItem(R.id.flag).setVisible(false);
         } else {
             menu.findItem(R.id.unflag).setVisible(false);
         }
 
         if (!mController.isCopyCapable(account)) {
             menu.findItem(R.id.copy).setVisible(false);
         }
 
         if (!mController.isMoveCapable(account)) {
             menu.findItem(R.id.move).setVisible(false);
             menu.findItem(R.id.archive).setVisible(false);
             menu.findItem(R.id.spam).setVisible(false);
         }
 
         if (!account.hasArchiveFolder()) {
             menu.findItem(R.id.archive).setVisible(false);
         }
 
         if (!account.hasSpamFolder()) {
             menu.findItem(R.id.spam).setVisible(false);
         }
 
     }
 
     public void onSwipeRightToLeft(final MotionEvent e1, final MotionEvent e2) {
         // Handle right-to-left as an un-select
         handleSwipe(e1, false);
     }
 
     public void onSwipeLeftToRight(final MotionEvent e1, final MotionEvent e2) {
         // Handle left-to-right as a select.
         handleSwipe(e1, true);
     }
 
     /**
      * Handle a select or unselect swipe event
      * @param downMotion Event that started the swipe
      * @param selected true if this was an attempt to select (i.e. left to right).
      */
     private void handleSwipe(final MotionEvent downMotion, final boolean selected) {
         int[] listPosition = new int[2];
         mListView.getLocationOnScreen(listPosition);
 
         int listX = (int) downMotion.getRawX() - listPosition[0];
         int listY = (int) downMotion.getRawY() - listPosition[1];
 
         int listViewPosition = mListView.pointToPosition(listX, listY);
 
         toggleMessageSelect(listViewPosition);
     }
 
     private int listViewToAdapterPosition(int position) {
         if (position > 0 && position <= mAdapter.getCount()) {
             return position - 1;
         }
 
         return AdapterView.INVALID_POSITION;
     }
 
     class MessageListActivityListener extends ActivityListener {
         @Override
         public void remoteSearchFailed(Account acct, String folder, final String err) {
             //TODO: Better error handling
             mHandler.post(new Runnable() {
                 @Override
                 public void run() {
                     Toast.makeText(getActivity(), err, Toast.LENGTH_LONG).show();
                 }
             });
         }
 
         @Override
         public void remoteSearchStarted(Account acct, String folder) {
             mHandler.progress(true);
             mHandler.updateFooter(mContext.getString(R.string.remote_search_sending_query), true);
         }
 
 
         @Override
         public void remoteSearchFinished(Account acct, String folder, int numResults, List<Message> extraResults) {
             mHandler.progress(false);
             if (extraResults != null && extraResults.size() > 0) {
                 mHandler.updateFooter(String.format(mContext.getString(R.string.load_more_messages_fmt), acct.getRemoteSearchNumResults()), false);
             } else {
                 mHandler.updateFooter("", false);
             }
             mFragmentListener.setMessageListProgress(Window.PROGRESS_END);
 
         }
 
         @Override
         public void remoteSearchServerQueryComplete(Account account, String folderName, int numResults) {
             mHandler.progress(true);
             if (account != null &&  account.getRemoteSearchNumResults() != 0 && numResults > account.getRemoteSearchNumResults()) {
                 mHandler.updateFooter(mContext.getString(R.string.remote_search_downloading_limited, account.getRemoteSearchNumResults(), numResults), true);
             } else {
                 mHandler.updateFooter(mContext.getString(R.string.remote_search_downloading, numResults), true);
             }
             mFragmentListener.setMessageListProgress(Window.PROGRESS_START);
         }
 
         @Override
         public void informUserOfStatus() {
             mHandler.refreshTitle();
         }
 
         @Override
         public void synchronizeMailboxStarted(Account account, String folder) {
             if (updateForMe(account, folder)) {
                 mHandler.progress(true);
                 mHandler.folderLoading(folder, true);
             }
             super.synchronizeMailboxStarted(account, folder);
         }
 
         @Override
         public void synchronizeMailboxFinished(Account account, String folder,
         int totalMessagesInMailbox, int numNewMessages) {
 
             if (updateForMe(account, folder)) {
                 mHandler.progress(false);
                 mHandler.folderLoading(folder, false);
                 mHandler.sortMessages();
             }
             super.synchronizeMailboxFinished(account, folder, totalMessagesInMailbox, numNewMessages);
         }
 
         @Override
         public void synchronizeMailboxFailed(Account account, String folder, String message) {
 
             if (updateForMe(account, folder)) {
                 mHandler.progress(false);
                 mHandler.folderLoading(folder, false);
                 mHandler.sortMessages();
             }
             super.synchronizeMailboxFailed(account, folder, message);
         }
 
         @Override
         public void searchStats(AccountStats stats) {
             mUnreadMessageCount = stats.unreadMessageCount;
             super.searchStats(stats);
         }
 
         @Override
         public void folderStatusChanged(Account account, String folder, int unreadMessageCount) {
             if (updateForMe(account, folder)) {
                 mUnreadMessageCount = unreadMessageCount;
             }
             super.folderStatusChanged(account, folder, unreadMessageCount);
         }
 
         private boolean updateForMe(Account account, String folder) {
             //FIXME
             return ((account.equals(mAccount) && folder.equals(mFolderName)));
         }
     }
 
 
     class MessageListAdapter extends CursorAdapter {
 
         private Drawable mAttachmentIcon;
         private Drawable mForwardedIcon;
         private Drawable mAnsweredIcon;
         private Drawable mForwardedAnsweredIcon;
 
         MessageListAdapter() {
             super(getActivity(), null, 0);
             mAttachmentIcon = getResources().getDrawable(R.drawable.ic_email_attachment_small);
             mAnsweredIcon = getResources().getDrawable(R.drawable.ic_email_answered_small);
             mForwardedIcon = getResources().getDrawable(R.drawable.ic_email_forwarded_small);
             mForwardedAnsweredIcon = getResources().getDrawable(R.drawable.ic_email_forwarded_answered_small);
         }
 
         /**
          * Set the selection state for all messages at once.
          * @param selected Selection state to set.
          */
         public void setSelectionForAllMesages(final boolean selected) {
             //TODO: implement
 
             //notifyDataSetChanged();
         }
 
         public void sortMessages() {
             //TODO: implement
 
             //notifyDataSetChanged();
         }
 
         private String recipientSigil(boolean toMe, boolean ccMe) {
             if (toMe) {
                 return getString(R.string.messagelist_sent_to_me_sigil);
             } else if (ccMe) {
                 return getString(R.string.messagelist_sent_cc_me_sigil);
             } else {
                 return "";
             }
         }
 
         @Override
         public View newView(Context context, Cursor cursor, ViewGroup parent) {
             View view = mInflater.inflate(R.layout.message_list_item, parent, false);
             view.setId(R.layout.message_list_item);
 
             MessageViewHolder holder = new MessageViewHolder();
             holder.date = (TextView) view.findViewById(R.id.date);
             holder.chip = view.findViewById(R.id.chip);
             holder.preview = (TextView) view.findViewById(R.id.preview);
 
             if (mSenderAboveSubject) {
                 holder.from = (TextView) view.findViewById(R.id.subject);
                 holder.from.setTextSize(TypedValue.COMPLEX_UNIT_SP, mFontSizes.getMessageListSender());
             } else {
                 holder.subject = (TextView) view.findViewById(R.id.subject);
                 holder.subject.setTextSize(TypedValue.COMPLEX_UNIT_SP, mFontSizes.getMessageListSubject());
             }
 
             holder.date.setTextSize(TypedValue.COMPLEX_UNIT_SP, mFontSizes.getMessageListDate());
 
             holder.preview.setLines(mPreviewLines);
             holder.preview.setTextSize(TypedValue.COMPLEX_UNIT_SP, mFontSizes.getMessageListPreview());
             holder.threadCount = (TextView) view.findViewById(R.id.thread_count);
 
             view.setTag(holder);
 
             return view;
         }
 
         @Override
         public void bindView(View view, Context context, Cursor cursor) {
             Account account = getAccountFromCursor(cursor);
 
             String fromList = cursor.getString(SENDER_LIST_COLUMN);
             String toList = cursor.getString(TO_LIST_COLUMN);
             String ccList = cursor.getString(CC_LIST_COLUMN);
             Address[] fromAddrs = Address.unpack(fromList);
             Address[] toAddrs = Address.unpack(toList);
             Address[] ccAddrs = Address.unpack(ccList);
 
             boolean fromMe = mMessageHelper.toMe(account, fromAddrs);
             boolean toMe = mMessageHelper.toMe(account, toAddrs);
             boolean ccMe = mMessageHelper.toMe(account, ccAddrs);
 
             CharSequence displayName = mMessageHelper.getDisplayName(account, fromAddrs, toAddrs);
 
             Date sentDate = new Date(cursor.getLong(DATE_COLUMN));
             String displayDate = mMessageHelper.formatDate(sentDate);
 
             String preview = cursor.getString(PREVIEW_COLUMN);
             if (preview == null) {
                 preview = "";
             }
 
             String subject = cursor.getString(SUBJECT_COLUMN);
             if (StringUtils.isNullOrEmpty(subject)) {
                 subject = getString(R.string.general_no_subject);
             }
 
             int threadCount = 0;    //TODO: get thread count from cursor
 
             String flagList = cursor.getString(FLAGS_COLUMN);
             String[] flags = flagList.split(",");
             boolean read = false;
             boolean flagged = false;
             boolean answered = false;
             boolean forwarded = false;
             for (int i = 0, len = flags.length; i < len; i++) {
                 try {
                     switch (Flag.valueOf(flags[i])) {
                         case SEEN: {
                             read = true;
                             break;
                         }
                         case FLAGGED: {
                             flagged = true;
                             break;
                         }
                         case ANSWERED: {
                             answered = true;
                             break;
                         }
                         case FORWARDED: {
                             forwarded = true;
                             break;
                         }
                         default: {
                             // We don't care about the other flags
                         }
                     }
                 } catch (Exception e) { /* ignore */ }
             }
 
             boolean hasAttachments = (cursor.getInt(ATTACHMENT_COUNT_COLUMN) > 0);
 
             MessageViewHolder holder = (MessageViewHolder) view.getTag();
 
             int maybeBoldTypeface = (read) ? Typeface.NORMAL : Typeface.BOLD;
 
             int adapterPosition = cursor.getPosition();
             boolean selected = mSelected.get(adapterPosition, false);
 
             if (selected) {
                 holder.chip.setBackgroundDrawable(account.getCheckmarkChip().drawable());
             } else {
                 holder.chip.setBackgroundDrawable(account.generateColorChip(read, toMe, ccMe,
                         fromMe, flagged).drawable());
             }
 
             // Background indicator
             if (K9.useBackgroundAsUnreadIndicator()) {
                 int res = (read) ? R.attr.messageListReadItemBackgroundColor :
                         R.attr.messageListUnreadItemBackgroundColor;
 
                 TypedValue outValue = new TypedValue();
                 getActivity().getTheme().resolveAttribute(res, outValue, true);
                 view.setBackgroundColor(outValue.data);
             }
 
             // Thread count
             if (mThreadId == -1 && threadCount > 1) {
                 holder.threadCount.setText(Integer.toString(threadCount));
                 holder.threadCount.setVisibility(View.VISIBLE);
             } else {
                 holder.threadCount.setVisibility(View.GONE);
             }
 
             CharSequence beforePreviewText = (mSenderAboveSubject) ? subject : displayName;
 
             String sigil = recipientSigil(toMe, ccMe);
 
             holder.preview.setText(
                     new SpannableStringBuilder(sigil)
                         .append(beforePreviewText)
                         .append(" ")
                         .append(preview), TextView.BufferType.SPANNABLE);
 
             Spannable str = (Spannable)holder.preview.getText();
 
             // Create a span section for the sender, and assign the correct font size and weight
             int fontSize = (mSenderAboveSubject) ?
                     mFontSizes.getMessageListSubject():
                     mFontSizes.getMessageListSender();
 
             AbsoluteSizeSpan span = new AbsoluteSizeSpan(fontSize, true);
             str.setSpan(span, 0, beforePreviewText.length() + 1,
                     Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
 
             //TODO: make this part of the theme
             int color = (K9.getK9Theme() == K9.THEME_LIGHT) ?
                     Color.rgb(105, 105, 105) :
                     Color.rgb(160, 160, 160);
 
             // Set span (color) for preview message
             str.setSpan(new ForegroundColorSpan(color), beforePreviewText.length() + 1,
                     str.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
 
             Drawable statusHolder = null;
             if (forwarded && answered) {
                 statusHolder = mForwardedAnsweredIcon;
             } else if (answered) {
                 statusHolder = mAnsweredIcon;
             } else if (forwarded) {
                 statusHolder = mForwardedIcon;
             }
 
             if (holder.from != null ) {
                 holder.from.setTypeface(null, maybeBoldTypeface);
                 if (mSenderAboveSubject) {
                     holder.from.setCompoundDrawablesWithIntrinsicBounds(
                             statusHolder, // left
                             null, // top
                             hasAttachments ? mAttachmentIcon : null, // right
                             null); // bottom
 
                     holder.from.setText(displayName);
                 } else {
                     holder.from.setText(new SpannableStringBuilder(sigil).append(displayName));
                 }
             }
 
             if (holder.subject != null ) {
                 if (!mSenderAboveSubject) {
                     holder.subject.setCompoundDrawablesWithIntrinsicBounds(
                             statusHolder, // left
                             null, // top
                             hasAttachments ? mAttachmentIcon : null, // right
                             null); // bottom
                 }
 
                 holder.subject.setTypeface(null, maybeBoldTypeface);
                 holder.subject.setText(subject);
             }
 
             holder.date.setText(displayDate);
         }
     }
 
     class MessageViewHolder {
         public TextView subject;
         public TextView preview;
         public TextView from;
         public TextView time;
         public TextView date;
         public View chip;
         public TextView threadCount;
     }
 
 
     private View getFooterView(ViewGroup parent) {
         if (mFooterView == null) {
             mFooterView = mInflater.inflate(R.layout.message_list_item_footer, parent, false);
             mFooterView.setId(R.layout.message_list_item_footer);
             FooterViewHolder holder = new FooterViewHolder();
             holder.progress = (ProgressBar) mFooterView.findViewById(R.id.message_list_progress);
             holder.progress.setIndeterminate(true);
             holder.main = (TextView) mFooterView.findViewById(R.id.main_text);
             mFooterView.setTag(holder);
         }
 
         return mFooterView;
     }
 
     private void updateFooterView() {
         if (mCurrentFolder != null && mAccount != null) {
             if (mCurrentFolder.loading) {
                 final boolean showProgress = true;
                 updateFooter(mContext.getString(R.string.status_loading_more), showProgress);
             } else {
                 String message;
                 if (!mCurrentFolder.lastCheckFailed) {
                     if (mAccount.getDisplayCount() == 0) {
                         message = mContext.getString(R.string.message_list_load_more_messages_action);
                     } else {
                         message = String.format(mContext.getString(R.string.load_more_messages_fmt), mAccount.getDisplayCount());
                     }
                 } else {
                     message = mContext.getString(R.string.status_loading_more_failed);
                 }
                 final boolean showProgress = false;
                 updateFooter(message, showProgress);
             }
         } else {
             final boolean showProgress = false;
             updateFooter(null, showProgress);
         }
     }
 
     public void updateFooter(final String text, final boolean progressVisible) {
         FooterViewHolder holder = (FooterViewHolder) mFooterView.getTag();
 
         holder.progress.setVisibility(progressVisible ? ProgressBar.VISIBLE : ProgressBar.INVISIBLE);
         if (text != null) {
             holder.main.setText(text);
         }
         if (progressVisible || holder.main.getText().length() > 0) {
             holder.main.setVisibility(View.VISIBLE);
         } else {
             holder.main.setVisibility(View.GONE);
         }
     }
 
     static class FooterViewHolder {
         public ProgressBar progress;
         public TextView main;
     }
 
     /**
      * Set selection state for all messages.
      *
      * @param selected
      *         If {@code true} all messages get selected. Otherwise, all messages get deselected and
      *         action mode is finished.
      */
     private void setSelectionState(boolean selected) {
         if (selected) {
             mSelectedCount = mAdapter.getCount();
             for (int i = 0, end = mSelectedCount; i < end; i++) {
                 mSelected.put(i, true);
             }
 
             if (mActionMode == null) {
                 mActionMode = getSherlockActivity().startActionMode(mActionModeCallback);
             }
             computeBatchDirection();
             updateActionModeTitle();
             computeSelectAllVisibility();
         } else {
             mSelected.clear();
             mSelectedCount = 0;
             if (mActionMode != null) {
                 mActionMode.finish();
                 mActionMode = null;
             }
         }
 
         mAdapter.notifyDataSetChanged();
     }
 
     private void toggleMessageSelect(int listViewPosition) {
         int adapterPosition = listViewToAdapterPosition(listViewPosition);
         if (adapterPosition == AdapterView.INVALID_POSITION) {
             return;
         }
 
         boolean selected = mSelected.get(adapterPosition, false);
         mSelected.put(adapterPosition, !selected);
 
         if (mActionMode != null) {
             if (mSelectedCount == 1 && selected) {
                 mActionMode.finish();
                 mActionMode = null;
                 return;
             }
         } else {
             mActionMode = getSherlockActivity().startActionMode(mActionModeCallback);
         }
 
         if (selected) {
             mSelectedCount -= 1;
         } else {
             mSelectedCount += 1;
         }
 
         computeBatchDirection();
         updateActionModeTitle();
 
         // make sure the onPrepareActionMode is called
         mActionMode.invalidate();
 
         computeSelectAllVisibility();
 
         mAdapter.notifyDataSetChanged();
     }
 
     private void updateActionModeTitle() {
         mActionMode.setTitle(String.format(getString(R.string.actionbar_selected), mSelectedCount));
     }
 
     private void computeSelectAllVisibility() {
         mActionModeCallback.showSelectAll(mSelectedCount != mAdapter.getCount());
     }
 
     private void computeBatchDirection() {
         boolean isBatchFlag = false;
         boolean isBatchRead = false;
 
         /*
         for (MessageInfoHolder holder : mAdapter.getMessages()) {
             if (holder.selected) {
                 if (!holder.flagged) {
                     isBatchFlag = true;
                 }
                 if (!holder.read) {
                     isBatchRead = true;
                 }
 
                 if (isBatchFlag && isBatchRead) {
                     break;
                 }
             }
         }
         */
         //TODO: implement
 
         mActionModeCallback.showMarkAsRead(isBatchRead);
         mActionModeCallback.showFlag(isBatchFlag);
     }
 
     private void setFlag(Message message, final Flag flag, final boolean newState) {
         setFlag(new Message[] { message }, flag, newState);
     }
 
     private void setFlag(Message[] messages, final Flag flag, final boolean newState) {
         if (messages.length == 0) {
             return;
         }
 
         mController.setFlag(messages, flag, newState);
 
         computeBatchDirection();
     }
 
     private void onMove(Message message) {
         onMove(new Message[] { message });
     }
 
     /**
      * Display the message move activity.
      *
      * @param holders
      *            Never {@code null}.
      */
     private void onMove(Message[] messages) {
         if (!checkCopyOrMovePossible(messages, FolderOperation.MOVE)) {
             return;
         }
 
         final Folder folder = messages.length == 1 ? messages[0].getFolder() : mCurrentFolder.folder;
         displayFolderChoice(ACTIVITY_CHOOSE_FOLDER_MOVE, folder, messages);
     }
 
     private void onCopy(Message message) {
         onCopy(new Message[] { message });
     }
 
     /**
      * Display the message copy activity.
      *
      * @param holders
      *            Never {@code null}.
      */
     private void onCopy(Message[] messages) {
         if (!checkCopyOrMovePossible(messages, FolderOperation.COPY)) {
             return;
         }
 
         final Folder folder = messages.length == 1 ? messages[0].getFolder() : mCurrentFolder.folder;
         displayFolderChoice(ACTIVITY_CHOOSE_FOLDER_COPY, folder, messages);
     }
 
     /**
      * Helper method to manage the invocation of
      * {@link #startActivityForResult(Intent, int)} for a folder operation
      * ({@link ChooseFolder} activity), while saving a list of associated
      * messages.
      *
      * @param requestCode
      *            If >= 0, this code will be returned in onActivityResult() when
      *            the activity exits.
      * @param folder
      *            Never {@code null}.
      * @param holders
      *            Messages to be affected by the folder operation. Never
      *            {@code null}.
      * @see #startActivityForResult(Intent, int)
      */
     private void displayFolderChoice(final int requestCode, final Folder folder, final Message[] messages) {
         final Intent intent = new Intent(getActivity(), ChooseFolder.class);
         intent.putExtra(ChooseFolder.EXTRA_ACCOUNT, folder.getAccount().getUuid());
         intent.putExtra(ChooseFolder.EXTRA_CUR_FOLDER, folder.getName());
         intent.putExtra(ChooseFolder.EXTRA_SEL_FOLDER, folder.getAccount().getLastSelectedFolderName());
         // remember the selected messages for #onActivityResult
         mActiveMessages = messages;
         startActivityForResult(intent, requestCode);
     }
 
     private void onArchive(final Message message) {
         onArchive(new Message[] { message });
     }
 
     private void onArchive(final Message[] messages) {
         final String folderName = messages[0].getFolder().getAccount().getArchiveFolderName();
         if (K9.FOLDER_NONE.equalsIgnoreCase(folderName)) {
             return;
         }
         // TODO one should separate messages by account and call move afterwards
         // (because each account might have a specific Archive folder name)
         move(messages, folderName);
     }
 
     private void onSpam(Message message) {
         onSpam(new Message[] { message });
     }
 
     /**
      * @param holders
      *            Never {@code null}.
      */
     private void onSpam(Message[] messages) {
         if (K9.confirmSpam()) {
             // remember the message selection for #onCreateDialog(int)
             mActiveMessages = messages;
             showDialog(R.id.dialog_confirm_spam);
         } else {
             onSpamConfirmed(messages);
         }
     }
 
     /**
      * @param holders
      *            Never {@code null}.
      */
     private void onSpamConfirmed(Message[] messages) {
         final String folderName = messages[0].getFolder().getAccount().getSpamFolderName();
         if (K9.FOLDER_NONE.equalsIgnoreCase(folderName)) {
             return;
         }
         // TODO one should separate messages by account and call move afterwards
         // (because each account might have a specific Spam folder name)
         move(messages, folderName);
     }
 
     private static enum FolderOperation {
         COPY, MOVE
     }
 
     /**
      * Display an Toast message if any message isn't synchronized
      *
      * @param holders
      *            Never <code>null</code>.
      * @param operation
      *            Never {@code null}.
      *
      * @return <code>true</code> if operation is possible
      */
     private boolean checkCopyOrMovePossible(final Message[] messages, final FolderOperation operation) {
         if (messages.length == 0) {
             return false;
         }
 
         boolean first = true;
         for (final Message message : messages) {
             if (first) {
                 first = false;
                 // account check
                 final Account account = message.getFolder().getAccount();
                 if ((operation == FolderOperation.MOVE && !mController.isMoveCapable(account)) ||
                         (operation == FolderOperation.COPY && !mController.isCopyCapable(account))) {
                     return false;
                 }
             }
             // message check
             if ((operation == FolderOperation.MOVE && !mController.isMoveCapable(message)) ||
                     (operation == FolderOperation.COPY && !mController.isCopyCapable(message))) {
                 final Toast toast = Toast.makeText(getActivity(), R.string.move_copy_cannot_copy_unsynced_message,
                                                    Toast.LENGTH_LONG);
                 toast.show();
                 return false;
             }
         }
         return true;
     }
 
     /**
      * Copy the specified messages to the specified folder.
      *
      * @param holders Never {@code null}.
      * @param destination Never {@code null}.
      */
     private void copy(Message[] messages, final String destination) {
         copyOrMove(messages, destination, FolderOperation.COPY);
     }
 
     /**
      * Move the specified messages to the specified folder.
      *
      * @param holders Never {@code null}.
      * @param destination Never {@code null}.
      */
     private void move(Message[] messages, final String destination) {
         copyOrMove(messages, destination, FolderOperation.MOVE);
     }
 
     /**
      * The underlying implementation for {@link #copy(List, String)} and
      * {@link #move(List, String)}. This method was added mainly because those 2
      * methods share common behavior.
      *
      * Note: Must be called from the UI thread!
      *
      * @param holders
      *            Never {@code null}.
      * @param destination
      *            Never {@code null}.
      * @param operation
      *            Never {@code null}.
      */
     private void copyOrMove(Message[] messages, final String destination, final FolderOperation operation) {
         if (K9.FOLDER_NONE.equalsIgnoreCase(destination)) {
             return;
         }
 
         boolean first = true;
         Account account = null;
         String folderName = null;
 
         List<Message> outMessages = new ArrayList<Message>();
 
         for (Message message : messages) {
             if (first) {
                 first = false;
                 folderName = message.getFolder().getName();
                 account = message.getFolder().getAccount();
                 if ((operation == FolderOperation.MOVE && !mController.isMoveCapable(account)) || (operation == FolderOperation.COPY && !mController.isCopyCapable(account))) {
                     // account is not copy/move capable
                     return;
                 }
             } else if (!account.equals(message.getFolder().getAccount())
                        || !folderName.equals(message.getFolder().getName())) {
                 // make sure all messages come from the same account/folder?
                 return;
             }
             if ((operation == FolderOperation.MOVE && !mController.isMoveCapable(message)) || (operation == FolderOperation.COPY && !mController.isCopyCapable(message))) {
                 final Toast toast = Toast.makeText(getActivity(), R.string.move_copy_cannot_copy_unsynced_message,
                                                    Toast.LENGTH_LONG);
                 toast.show();
 
                 // XXX return meaningful error value?
 
                 // message isn't synchronized
                 return;
             }
             outMessages.add(message);
         }
 
         if (operation == FolderOperation.MOVE) {
             mController.moveMessages(account, folderName, outMessages.toArray(new Message[outMessages.size()]), destination,
                                      null);
         } else {
             mController.copyMessages(account, folderName, outMessages.toArray(new Message[outMessages.size()]), destination,
                                      null);
         }
     }
 
     /**
      * Return the currently "open" account if available.
      *
      * @param prefs
      *         A {@link Preferences} instance that might be used to retrieve the current
      *         {@link Account}.
      *
      * @return The {@code Account} all displayed messages belong to.
      */
     //TODO: remove
     /*private Account getCurrentAccount(Preferences prefs) {
         Account account = null;
         if (mQueryString != null && !mIntegrate && mAccountUuids != null &&
                 mAccountUuids.length == 1) {
             String uuid = mAccountUuids[0];
             account = prefs.getAccount(uuid);
         } else if (mAccount != null) {
             account = mAccount;
         }
 
         return account;
     }*/
 
 
     class ActionModeCallback implements ActionMode.Callback {
         private MenuItem mSelectAll;
         private MenuItem mMarkAsRead;
         private MenuItem mMarkAsUnread;
         private MenuItem mFlag;
         private MenuItem mUnflag;
 
         @Override
         public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
             mSelectAll = menu.findItem(R.id.select_all);
             mMarkAsRead = menu.findItem(R.id.mark_as_read);
             mMarkAsUnread = menu.findItem(R.id.mark_as_unread);
             mFlag = menu.findItem(R.id.flag);
             mUnflag = menu.findItem(R.id.unflag);
 
             // we don't support cross account actions atm
             if (!mSingleAccountMode) {
                 // show all
                 menu.findItem(R.id.move).setVisible(true);
                 menu.findItem(R.id.archive).setVisible(true);
                 menu.findItem(R.id.spam).setVisible(true);
                 menu.findItem(R.id.copy).setVisible(true);
 
                 // hide uncapable
                 /*
                  *  TODO think of a better way then looping over all
                  *  messages.
                  */
                 Message[] messages = getCheckedMessages();
                 Account account;
 
                 for (Message message : messages) {
                     account = message.getFolder().getAccount();
                     setContextCapabilities(account, menu);
                 }
 
             }
             return true;
         }
 
         @Override
         public void onDestroyActionMode(ActionMode mode) {
             mActionMode = null;
             mSelectAll = null;
             mMarkAsRead = null;
             mMarkAsUnread = null;
             mFlag = null;
             mUnflag = null;
             setSelectionState(false);
         }
 
         @Override
         public boolean onCreateActionMode(ActionMode mode, Menu menu) {
             MenuInflater inflater = mode.getMenuInflater();
             inflater.inflate(R.menu.message_list_context, menu);
 
             // check capabilities
             setContextCapabilities(mAccount, menu);
 
             return true;
         }
 
         /**
          * Disables menu options based on if the account supports it or not.
          * It also checks the controller and for now the 'mode' the messagelist
          * is operation in ( query or not ).
          *
          * @param mAccount Account to check capabilities of.
          * @param menu Menu to adapt.
          */
         private void setContextCapabilities(Account mAccount, Menu menu) {
             /*
              * TODO get rid of this when we finally split the messagelist into
              * a folder content display and a search result display
              */
             if (!mSingleAccountMode) {
                 menu.findItem(R.id.move).setVisible(false);
                 menu.findItem(R.id.copy).setVisible(false);
 
                 menu.findItem(R.id.archive).setVisible(false);
                 menu.findItem(R.id.spam).setVisible(false);
 
             } else {
                 // hide unsupported
                 if (!mController.isCopyCapable(mAccount)) {
                     menu.findItem(R.id.copy).setVisible(false);
                 }
 
                 if (!mController.isMoveCapable(mAccount)) {
                     menu.findItem(R.id.move).setVisible(false);
                     menu.findItem(R.id.archive).setVisible(false);
                     menu.findItem(R.id.spam).setVisible(false);
                 }
 
                 if (!mAccount.hasArchiveFolder()) {
                     menu.findItem(R.id.archive).setVisible(false);
                 }
 
                 if (!mAccount.hasSpamFolder()) {
                     menu.findItem(R.id.spam).setVisible(false);
                 }
             }
         }
 
         public void showSelectAll(boolean show) {
             if (mActionMode != null) {
                 mSelectAll.setVisible(show);
             }
         }
 
         public void showMarkAsRead(boolean show) {
             if (mActionMode != null) {
                 mMarkAsRead.setVisible(show);
                 mMarkAsUnread.setVisible(!show);
             }
         }
 
         public void showFlag(boolean show) {
             if (mActionMode != null) {
                 mFlag.setVisible(show);
                 mUnflag.setVisible(!show);
             }
         }
 
         @Override
         public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
             Message[] messages = getCheckedMessages();
 
             /*
              * In the following we assume that we can't move or copy
              * mails to the same folder. Also that spam isn't available if we are
              * in the spam folder,same for archive.
              *
              * This is the case currently so safe assumption.
              */
             switch (item.getItemId()) {
             case R.id.delete: {
                 onDelete(messages);
 
                 //FIXME
                 mSelectedCount = 0;
                 break;
             }
             case R.id.mark_as_read: {
                 setFlag(messages, Flag.SEEN, true);
                 break;
             }
             case R.id.mark_as_unread: {
                 setFlag(messages, Flag.SEEN, false);
                 break;
             }
             case R.id.flag: {
                 setFlag(messages, Flag.FLAGGED, true);
                 break;
             }
             case R.id.unflag: {
                 setFlag(messages, Flag.FLAGGED, false);
                 break;
             }
             case R.id.select_all: {
                 selectAll();
                 break;
             }
 
             // only if the account supports this
             case R.id.archive: {
                 onArchive(messages);
                 mSelectedCount = 0;
                 break;
             }
             case R.id.spam: {
                 onSpam(messages);
                 mSelectedCount = 0;
                 break;
             }
             case R.id.move: {
                 onMove(messages);
                 mSelectedCount = 0;
                 break;
             }
             case R.id.copy: {
                 onCopy(messages);
                 mSelectedCount = 0;
                 break;
             }
             }
             if (mSelectedCount == 0) {
                 mActionMode.finish();
             }
 
             return true;
         }
     }
 
     @Override
     public void doPositiveClick(int dialogId) {
         switch (dialogId) {
             case R.id.dialog_confirm_spam: {
                 onSpamConfirmed(mActiveMessages);
                 // No further need for this reference
                 mActiveMessages = null;
                 break;
             }
         }
     }
 
     @Override
     public void doNegativeClick(int dialogId) {
         switch (dialogId) {
             case R.id.dialog_confirm_spam: {
                 // No further need for this reference
                 mActiveMessages = null;
                 break;
             }
         }
     }
 
     @Override
     public void dialogCancelled(int dialogId) {
         doNegativeClick(dialogId);
     }
 
     public void checkMail() {
         mController.synchronizeMailbox(mAccount, mFolderName, mListener, null);
         mController.sendPendingMessages(mAccount, mListener);
     }
 
     /**
      * We need to do some special clean up when leaving a remote search result screen.  If no remote search is
      * in progress, this method does nothing special.
      */
     @Override
     public void onStop() {
         // If we represent a remote search, then kill that before going back.
         if (isRemoteSearch() && mRemoteSearchFuture != null) {
             try {
                 Log.i(K9.LOG_TAG, "Remote search in progress, attempting to abort...");
                 // Canceling the future stops any message fetches in progress.
                 final boolean cancelSuccess = mRemoteSearchFuture.cancel(true);   // mayInterruptIfRunning = true
                 if (!cancelSuccess) {
                     Log.e(K9.LOG_TAG, "Could not cancel remote search future.");
                 }
                 // Closing the folder will kill off the connection if we're mid-search.
                 final Account searchAccount = mAccount;
                 final Folder remoteFolder = mCurrentFolder.folder;
                 remoteFolder.close();
                 // Send a remoteSearchFinished() message for good measure.
                 //mAdapter.mListener.remoteSearchFinished(searchAccount, mCurrentFolder.name, 0, null);
             } catch (Exception e) {
                 // Since the user is going back, log and squash any exceptions.
                 Log.e(K9.LOG_TAG, "Could not abort remote search before going back", e);
             }
         }
         super.onStop();
     }
 
     public ArrayList<MessageReference> getMessageReferences() {
         ArrayList<MessageReference> messageRefs = new ArrayList<MessageReference>();
 
         /*
         for (MessageInfoHolder holder : mAdapter.getMessages()) {
             MessageReference ref = holder.message.makeMessageReference();
             messageRefs.add(ref);
         }
         */
         //TODO: implement
 
         return messageRefs;
     }
 
     public void selectAll() {
         setSelectionState(true);
     }
 
     public void onMoveUp() {
         int currentPosition = mListView.getSelectedItemPosition();
         if (currentPosition == AdapterView.INVALID_POSITION || mListView.isInTouchMode()) {
             currentPosition = mListView.getFirstVisiblePosition();
         }
         if (currentPosition > 0) {
             mListView.setSelection(currentPosition - 1);
         }
     }
 
     public void onMoveDown() {
         int currentPosition = mListView.getSelectedItemPosition();
         if (currentPosition == AdapterView.INVALID_POSITION || mListView.isInTouchMode()) {
             currentPosition = mListView.getFirstVisiblePosition();
         }
 
         if (currentPosition < mListView.getCount()) {
             mListView.setSelection(currentPosition + 1);
         }
     }
 
     public interface MessageListFragmentListener {
         void setMessageListProgress(int level);
         void showThread(Account account, String folderName, long rootId);
         void remoteSearch(String searchAccount, String searchFolder, String queryString);
         void showMoreFromSameSender(String senderAddress);
         void onResendMessage(Message message);
         void onForward(Message message);
         void onReply(Message message);
         void onReplyAll(Message message);
         void openMessage(MessageReference messageReference);
         void setMessageListTitle(String title);
         void setMessageListSubTitle(String subTitle);
         void setUnreadCount(int unread);
         void onCompose(Account account);
         boolean startSearch(Account account, String folderName);
     }
 
     public void onReverseSort() {
         changeSort(mSortType);
     }
 
     private Message getSelectedMessage() {
         int listViewPosition = mListView.getSelectedItemPosition();
         int adapterPosition = listViewToAdapterPosition(listViewPosition);
 
         return getMessageAtPosition(adapterPosition);
     }
 
     private Message getMessageAtPosition(int adapterPosition) {
         if (adapterPosition == AdapterView.INVALID_POSITION) {
             return null;
         }
 
         Cursor cursor = (Cursor) mAdapter.getItem(adapterPosition);
         String uid = cursor.getString(UID_COLUMN);
 
         //TODO: get account and folder from cursor
         Folder folder = mCurrentFolder.folder;
 
         try {
             return folder.getMessage(uid);
         } catch (MessagingException e) {
             Log.e(K9.LOG_TAG, "Something went wrong while fetching a message", e);
         }
 
         return null;
     }
 
     private Message[] getCheckedMessages() {
         Message[] messages = new Message[mSelectedCount];
         int out = 0;
         for (int position = 0, end = mAdapter.getCount(); position < end; position++) {
             if (mSelected.get(position, false)) {
                 messages[out++] = getMessageAtPosition(position);
             }
         }
 
         return messages;
     }
 
     public void onDelete() {
         Message message = getSelectedMessage();
         if (message != null) {
             onDelete(new Message[] { message });
         }
     }
 
     public void toggleMessageSelect() {
         toggleMessageSelect(mListView.getSelectedItemPosition());
     }
 
     public void onToggleFlag() {
         Message message = getSelectedMessage();
         if (message != null) {
             setFlag(message, Flag.FLAGGED, !message.isSet(Flag.FLAGGED));
         }
     }
 
     public void onMove() {
         Message message = getSelectedMessage();
         if (message != null) {
             onMove(message);
         }
     }
 
     public void onArchive() {
         Message message = getSelectedMessage();
         if (message != null) {
             onArchive(message);
         }
     }
 
     public void onCopy() {
         Message message = getSelectedMessage();
         if (message != null) {
             onCopy(message);
         }
     }
 
     public void onToggleRead() {
         Message message = getSelectedMessage();
         if (message != null) {
             setFlag(message, Flag.SEEN, !message.isSet(Flag.SEEN));
         }
     }
 
     public boolean isSearchQuery() {
         return (mSearch.getRemoteSearchArguments() != null || !mSingleAccountMode);
     }
 
     public boolean isOutbox() {
         return (mFolderName != null && mFolderName.equals(mAccount.getOutboxFolderName()));
     }
 
     public boolean isErrorFolder() {
         return K9.ERROR_FOLDER_NAME.equals(mFolderName);
     }
 
     public boolean isRemoteFolder() {
         if (isSearchQuery() || isOutbox() || isErrorFolder()) {
             return false;
         }
 
         if (!mController.isMoveCapable(mAccount)) {
             // For POP3 accounts only the Inbox is a remote folder.
             return (mFolderName != null && !mFolderName.equals(mAccount.getInboxFolderName()));
         }
 
         return true;
     }
 
     public boolean isAccountExpungeCapable() {
         try {
             return (mAccount != null && mAccount.getRemoteStore().isExpungeCapable());
         } catch (Exception e) {
             return false;
         }
     }
 
     public void onRemoteSearch() {
         // Remote search is useless without the network.
         if (mHasConnectivity) {
             onRemoteSearchRequested();
         } else {
             Toast.makeText(getActivity(), getText(R.string.remote_search_unavailable_no_network),
                     Toast.LENGTH_SHORT).show();
         }
     }
 
     public boolean isRemoteSearch() {
         return mRemoteSearch;
     }
 
     public boolean isRemoteSearchAllowed() {
         if (!isSearchQuery() || mRemoteSearch || !mSingleFolderMode) {
             return false;
         }
 
         boolean allowRemoteSearch = false;
         final Account searchAccount = mAccount;
         if (searchAccount != null) {
             allowRemoteSearch = searchAccount.allowRemoteSearch();
         }
 
         return allowRemoteSearch;
     }
 
     public boolean onSearchRequested() {
         String folderName = (mCurrentFolder != null) ? mCurrentFolder.name : null;
         return mFragmentListener.startSearch(mAccount, folderName);
    }
 
     @Override
     public Loader<Cursor> onCreateLoader(int id, Bundle args) {
         String accountUuid = mAccountUuids[id];
         Account account = mPreferences.getAccount(accountUuid);
 
         Uri uri = Uri.withAppendedPath(EmailProvider.CONTENT_URI, "account/" + accountUuid + "/messages");
 
         StringBuilder query = new StringBuilder();
         List<String> queryArgs = new ArrayList<String>();
         buildQuery(account, mSearch.getConditions(), query, queryArgs);
 
         String selection = query.toString();
         String[] selectionArgs = queryArgs.toArray(new String[0]);
 
         return new CursorLoader(getActivity(), uri, PROJECTION, selection, selectionArgs,
                 MessageColumns.DATE + " DESC");
     }
 
     private void buildQuery(Account account, ConditionsTreeNode node, StringBuilder query,
             List<String> selectionArgs) {
 
         if (node == null) {
             return;
         }
 
         if (node.mLeft == null && node.mRight == null) {
             SearchCondition condition = node.mCondition;
             switch (condition.field) {
                 case FOLDER: {
                     String folderName;
                     //TODO: Fix the search condition used by the Unified Inbox
                     if (LocalSearch.GENERIC_INBOX_NAME.equals(condition.value) ||
                             "1".equals(condition.value)) {
                         folderName = account.getInboxFolderName();
                     } else {
                         folderName = condition.value;
                     }
                     long folderId = getFolderId(account, folderName);
                     query.append("folder_id = ?");
                     selectionArgs.add(Long.toString(folderId));
                     break;
                 }
                 default: {
                     query.append(condition.toString());
                 }
             }
         } else {
             query.append("(");
             buildQuery(account, node.mLeft, query, selectionArgs);
             query.append(") ");
             query.append(node.mValue.name());
             query.append(" (");
             buildQuery(account, node.mRight, query, selectionArgs);
             query.append(")");
         }
     }
 
     private long getFolderId(Account account, String folderName) {
         long folderId = 0;
         try {
             LocalFolder folder = (LocalFolder) getFolder(folderName, account).folder;
             folder.open(OpenMode.READ_ONLY);
             folderId = folder.getId();
         } catch (MessagingException e) {
             //FIXME
             e.printStackTrace();
         }
 
         return folderId;
     }
 
     @Override
     public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
         mCursors.put(loader.getId(), data);
 
         List<Integer> list = new LinkedList<Integer>(mCursors.keySet());
         Collections.sort(list);
         List<Cursor> cursors = new ArrayList<Cursor>(list.size());
         for (Integer id : list) {
             cursors.add(mCursors.get(id));
         }
 
         MergeCursorWithUniqueId cursor = new MergeCursorWithUniqueId(cursors);
 
         mSelected = new SparseBooleanArray(cursor.getCount());
         //TODO: use the (stable) IDs as index and reuse the old mSelected
         mAdapter.swapCursor(cursor);
     }
 
     @Override
     public void onLoaderReset(Loader<Cursor> loader) {
         mSelected = null;
         mAdapter.swapCursor(null);
     }
 
     private Account getAccountFromCursor(Cursor cursor) {
         String accountUuid = cursor.getString(ACCOUNT_UUID_COLUMN);
         return mPreferences.getAccount(accountUuid);
     }
 }
