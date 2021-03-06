 /**
  *
  */
 package org.orange.familylink.fragment;
 
 import java.util.Date;
 import java.util.HashMap;
 import java.util.Map;
 
 import org.holoeverywhere.FontLoader;
 import org.holoeverywhere.LayoutInflater;
 import org.holoeverywhere.app.Activity;
 import org.holoeverywhere.app.ListFragment;
 import org.holoeverywhere.preference.SharedPreferences;
 import org.holoeverywhere.preference.SharedPreferences.Editor;
 import org.holoeverywhere.widget.AdapterView;
 import org.holoeverywhere.widget.AdapterView.OnItemSelectedListener;
 import org.holoeverywhere.widget.ArrayAdapter;
 import org.holoeverywhere.widget.CheckedTextView;
 import org.holoeverywhere.widget.LinearLayout;
 import org.holoeverywhere.widget.ListView;
 import org.holoeverywhere.widget.Spinner;
 import org.holoeverywhere.widget.TextView;
 import org.orange.familylink.R;
 import org.orange.familylink.data.Message.Code;
 import org.orange.familylink.data.MessageLogRecord.Direction;
 import org.orange.familylink.data.MessageLogRecord.Status;
 import org.orange.familylink.database.Contract;
 
 import android.annotation.SuppressLint;
 import android.content.Context;
 import android.database.Cursor;
 import android.net.Uri;
 import android.os.Build;
 import android.os.Bundle;
 import android.support.v4.app.LoaderManager;
 import android.support.v4.app.LoaderManager.LoaderCallbacks;
 import android.support.v4.content.CursorLoader;
 import android.support.v4.content.Loader;
 import android.support.v4.widget.CursorAdapter;
 import android.support.v4.widget.SimpleCursorAdapter;
 import android.text.format.DateFormat;
 import android.view.View;
 import android.view.ViewGroup;
 import android.widget.ImageView;
 import android.widget.ListAdapter;
 import android.widget.SpinnerAdapter;
 
 /**
  * 日志{@link ListFragment}
  * @author Team Orange
  */
 public class LogFragment extends ListFragment {
 	private static final String PREF_NAME = "log_fragment";
 	private static final String PREF_KEY_STATUS = "status";
 	private static final String PREF_KEY_CODE = "code";
 	private static final String PREF_KEY_CONTACT_ID = "contact_id";
 	private static final int LOADER_ID_CONTACTS = 1;
 	private static final int LOADER_ID_LOG = 2;
 
 	/** 用于把联系人ID映射为联系人名称的{@link Map} */
 	private Map<Long, String> mContactIdToNameMap;
 	/** 用于显示联系人筛选条件的{@link Spinner}的{@link SpinnerAdapter} */
 	private MySimpleCursorAdapterWithHeader mAdapterForContactsSpinner;
 	/** 用于显示消息日志的{@link ListView}的{@link ListAdapter} */
 	private CursorAdapter mAdapterForLogList;
 	private Spinner mSpinnerForStatus;
 	private Spinner mSpinnerForCode;
 	private Spinner mSpinnerForContact;
 	private String[] mSelectionForStatus;
 	private String[] mSelectionForCode;
 
 	@Override
 	public View onCreateView(LayoutInflater inflater, ViewGroup container,
 			Bundle savedInstanceState) {
 		// 因为super.onCreateView没有使用android.R.layout.list_content布局文件，
 		// 我们也无法按此方法的文档说明来include list_content，
 		// 只能在这用代码继承布局，以保留其内建indeterminant progress state
 
 		// 创建自定义布局，把原来的root作为新布局的子元素
 		View originalRoot = super.onCreateView(inflater, container, savedInstanceState);
 
 		LinearLayout root = new LinearLayout(getActivity());
 		root.setOrientation(LinearLayout.VERTICAL);
 		// ------------------------------------------------------------------
 		// 添加 原来的root
 		root.addView(originalRoot, new LinearLayout.LayoutParams(
 				LinearLayout.LayoutParams.MATCH_PARENT,
 				LinearLayout.LayoutParams.WRAP_CONTENT,
 				1));
 		// ------------------------------------------------------------------
 		// 添加 筛选条件输入部件
 		root.addView(createFilterWidget(), new LinearLayout.LayoutParams(
 				LinearLayout.LayoutParams.MATCH_PARENT,
 				LinearLayout.LayoutParams.WRAP_CONTENT,
 				0));
 		// ------------------------------------------------------------------
 		return root;
 	}
 
 	/* (non-Javadoc)
 	 * @see android.support.v4.app.Fragment#onActivityCreated(android.os.Bundle)
 	 */
 	@Override
 	public void onActivityCreated(Bundle savedInstanceState) {
 		super.onActivityCreated(savedInstanceState);
 		// 恢复上次的筛选选项
 		SharedPreferences pref = getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE);
 		mSpinnerForStatus.setSelection(pref.getInt(PREF_KEY_STATUS, 0));
 		mSpinnerForCode.setSelection(pref.getInt(PREF_KEY_CODE, 0));
 
 		// Give some text to display if there is no data.
 		setEmptyText(getResources().getText(R.string.no_message_record));
 
 		// We have a menu item to show in action bar.
 		setHasOptionsMenu(true);
 
 		// Create an empty adapter we will use to display the loaded data.
 		mAdapterForLogList = new LogAdapter(getActivity(), null, 0);
 		setListAdapter(mAdapterForLogList);
 
 		// Start out with a progress indicator.
 		setListShown(false);
 
 		// Prepare the loader.  Either re-connect with an existing one,
 		// or start a new one.
 		LoaderManager loaderManager = getLoaderManager();
 		loaderManager.initLoader(LOADER_ID_CONTACTS, null, mLoaderCallbacksForContacts);
 		loaderManager.initLoader(LOADER_ID_LOG, null, mLoaderCallbacks);
 	}
 
 	@SuppressLint("NewApi")
 	@Override
 	public void onStop() {
 		super.onStop();
 		// 保存筛选条件的当前选择
 		Editor editor = getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE).edit();
 		editor.putInt(PREF_KEY_STATUS, mSpinnerForStatus.getSelectedItemPosition());
 		editor.putInt(PREF_KEY_CODE, mSpinnerForCode.getSelectedItemPosition());
 		editor.putLong(PREF_KEY_CONTACT_ID, mSpinnerForContact.getSelectedItemId());
 		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD)
 			editor.apply();
 		else
 			editor.commit();
 	}
 
 	/**
 	 * 创建筛选条件输入部件
 	 * @return 新创建的筛选条件输入部件
 	 */
 	protected View createFilterWidget() {
 		LinearLayout spinnersContainer = new LinearLayout(getActivity());
 		spinnersContainer.setOrientation(LinearLayout.HORIZONTAL);
 
 		mSpinnerForStatus = new Spinner(getActivity());
 		// Create an ArrayAdapter using the string array and a default spinner layout
 		String[] status = getResources().getStringArray(R.array.message_status);
 		if(status.length != 9)
 			throw new IllegalStateException("Unexpected number of status. " +
 					"Maybe because you only update on one place");
 		String columnStatus = Contract.Messages.COLUMN_NAME_STATUS;
 		mSelectionForStatus = new String[status.length];
 		mSelectionForStatus[0] = "1";
 		mSelectionForStatus[2] = columnStatus + " = '" + Status.UNREAD.name() + "'";
 		mSelectionForStatus[3] = columnStatus + " = '" + Status.HAVE_READ.name() + "'";
 		mSelectionForStatus[1] = mSelectionForStatus[2] + " OR " + mSelectionForStatus[3];
 		mSelectionForStatus[5] = columnStatus + " = '" + Status.SENDING.name() + "'";
 		mSelectionForStatus[6] = columnStatus + " = '" + Status.SENT.name() + "'";
 		mSelectionForStatus[7] = columnStatus + " = '" + Status.DELIVERED.name() + "'";
 		mSelectionForStatus[8] = columnStatus + " = '" + Status.FAILED_TO_SEND.name() + "'";
 		mSelectionForStatus[4] = mSelectionForStatus[5] + " OR " + mSelectionForStatus[6]
 				+ " OR " + mSelectionForStatus[7] + " OR " + mSelectionForStatus[8];
 		ArrayAdapter<String> adapter = new MyHierarchicalArrayAdapter<String>(
 				getActivity(), R.layout.simple_spinner_item, status) {
 			@Override
 			protected int getLevel(int position) {
 				if(position != 0 && position != 1 && position != 4)
 					return 2;
 				else
 					return 1;
 			}
 		};
 		adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
 		mSpinnerForStatus.setAdapter(adapter);
 		mSpinnerForStatus.setOnItemSelectedListener(mOnSpinnerItemSelectedListener);
 		spinnersContainer.addView(mSpinnerForStatus, new LinearLayout.LayoutParams(
 				LinearLayout.LayoutParams.WRAP_CONTENT,
 				LinearLayout.LayoutParams.MATCH_PARENT,
 				1));
 
 		mSpinnerForCode = new Spinner(getActivity());
 		String[] code = getResources().getStringArray(R.array.code);
 		if(code.length != 5)
 			throw new IllegalStateException("Unexpected number of code. " +
 					"Maybe because you only update on one place");
 		String columnCode = Contract.Messages.COLUMN_NAME_CODE;
 		mSelectionForCode = new String[code.length];
 		mSelectionForCode[0] = "1";
 		// 通告
 		mSelectionForCode[1] = columnCode + " BETWEEN " + Code.INFORM + " AND " +
 								(Code.INFORM | Code.EXTRA_BITS);
 		// 通告 && 定时消息
 		mSelectionForCode[2] = mSelectionForCode[1] + " AND " +
 			columnCode + " & " + Code.Extra.Inform.PULSE + " = " + Code.Extra.Inform.PULSE;
 		// 通告 &&　紧急消息
 		mSelectionForCode[3] = mSelectionForCode[1] + " AND " +
 			columnCode + " & " + Code.Extra.Inform.URGENT + " = " + Code.Extra.Inform.URGENT;
 		// 命令 || ( 通告 && 命令响应 )
 		mSelectionForCode[4] = columnCode + " BETWEEN " + Code.COMMAND + " AND " +
 				(Code.COMMAND | Code.EXTRA_BITS) + " OR ( " +
 				mSelectionForCode[1] + " AND " +
 				columnCode + " & " + Code.Extra.Inform.RESPOND + " = " + Code.Extra.Inform.RESPOND + " )";
 		adapter = new MyHierarchicalArrayAdapter<String>(
 				getActivity(), R.layout.simple_spinner_item, code) {
 			@Override
 			protected int getLevel(int position) {
 				if(position == 2 || position == 3)
 					return 2;
 				else
 					return 1;
 			}
 		};
 		adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
 		mSpinnerForCode.setAdapter(adapter);
 		mSpinnerForCode.setOnItemSelectedListener(mOnSpinnerItemSelectedListener);
 		spinnersContainer.addView(mSpinnerForCode, new LinearLayout.LayoutParams(
 				LinearLayout.LayoutParams.WRAP_CONTENT,
 				LinearLayout.LayoutParams.MATCH_PARENT,
 				1));
 
 		mSpinnerForContact = new Spinner(getActivity());
 		mAdapterForContactsSpinner = new MySimpleCursorAdapterWithHeader(
 				getActivity(),
 				null,
 				new String[]{Contract.Contacts.COLUMN_NAME_NAME},
 				0,
 				new String[]{getString(R.string.all)});
 		mSpinnerForContact.setAdapter(mAdapterForContactsSpinner);
 		mSpinnerForContact.setOnItemSelectedListener(mOnSpinnerItemSelectedListener);
 		spinnersContainer.addView(mSpinnerForContact, new LinearLayout.LayoutParams(
 				LinearLayout.LayoutParams.WRAP_CONTENT,
 				LinearLayout.LayoutParams.MATCH_PARENT,
 				1));
 		return spinnersContainer;
 	}
 
 	private int getImportantCode(Integer code) {
 		if(code == null) {
 			return R.string.undefined;
 		} else if(Code.isInform(code)){
 			if(Code.Extra.Inform.hasSetUrgent(code))
 				return R.string.urgent;
 			else if(Code.Extra.Inform.hasSetRespond(code))
 				return R.string.respond;
 			else if(Code.Extra.Inform.hasSetPulse(code))
 				return R.string.pulse;
 			else
 				return R.string.inform;
 		} else if(Code.isCommand(code)){
 			if(Code.Extra.Command.hasSetLocateNow(code))
 				return R.string.locate_now;
 			else
 				return R.string.command;
 		} else if(!Code.isLegalCode(code)) {
 				return R.string.illegal_code;
 		} else {
 			throw new IllegalArgumentException("May be method Code.isLegalCode() not correct");
 		}
 	}
 	private String valueOfCode(Integer code) {
 		if(code == null)
 			return getString(R.string.undefined);
 		else if(Code.isInform(code))
 			return getString(R.string.inform);
 		else if(Code.isCommand(code))
 			return getString(R.string.command);
 		else if(!Code.isLegalCode(code))
 			return getString(R.string.illegal_code, code.intValue());
 		else
 			throw new IllegalArgumentException("May be method Code.isLegalCode() not correct");
 	}
 	private String valueOfCodeExtra(Integer code, String delimiter) {
 		if(delimiter.length() == 0)
 			throw new IllegalArgumentException("you should set a delimiter");
 		if(code == null || !Code.isLegalCode(code))
 			return "";
 		StringBuilder sb = new StringBuilder();
 		if(Code.isInform(code)){
 			if(Code.Extra.Inform.hasSetUrgent(code))
 				sb.append(getString(R.string.urgent) + delimiter);
 			if(Code.Extra.Inform.hasSetRespond(code))
 				sb.append(getString(R.string.respond) + delimiter);
 			if(Code.Extra.Inform.hasSetPulse(code))
 				sb.append(getString(R.string.pulse) + delimiter);
 		} else if(Code.isCommand(code)) {
 			if(Code.Extra.Command.hasSetLocateNow(code))
 				sb.append(getString(R.string.locate_now) + delimiter);
 		} else {
 			throw new IllegalArgumentException("May be method Code.isLegalCode() not correct");
 		}
 		int last = sb.lastIndexOf(delimiter);
 		if(last > 0)
 			return sb.substring(0, last);
 		else
 			return "";
 	}
 	private String valueOfDirection(Status status) {
 		if(status == null)
 			return "";
 		Direction direction = status.getDirection();
 		if(direction == Direction.SEND)
 			return getString(R.string.send);
 		else if(direction == Direction.RECEIVE)
 			return getString(R.string.receive);
 		else
 			throw new UnsupportedOperationException("unsupport "+direction+" now.");
 	}
 	private String valueOfHasRead(Status status) {
 		if(status == Status.HAVE_READ)
 			return getString(R.string.has_read);
 		else if(status == Status.UNREAD)
 			return getString(R.string.unread);
 		else
 			return "";
 	}
 	private String valueOfSendStatus(Status status) {
 		if(status == null || status.getDirection() != Direction.SEND)
 			return "";
 		switch(status) {
 		case SENDING:
 			return getString(R.string.sending);
 		case SENT:
 			return getString(R.string.sent);
 		case DELIVERED:
 			return getString(R.string.delivered);
 		case FAILED_TO_SEND:
 			return getString(R.string.failed_to_send);
 		default:
 			throw new UnsupportedOperationException("unsupport "+status+" now.");
 		}
 	}
 
 	protected final LoaderCallbacks<Cursor> mLoaderCallbacksForContacts =
 			new LoaderCallbacks<Cursor>(){
 		@Override
 		public Loader<Cursor> onCreateLoader(int id, Bundle args) {
 			// One callback only to one loader, so we don't care about the ID.
 			Uri baseUri = Contract.Contacts.CONTACTS_URI;
 			String[] projection = {Contract.Contacts._ID, Contract.Contacts.COLUMN_NAME_NAME};
 			return new CursorLoader(getActivity(), baseUri, projection, null, null, null);
 		}
 
 		@Override
 		public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
 			// 更新mAdapterForContactsSpinner的数据。如果是第一次更新，恢复上次的选择
 			if(mAdapterForContactsSpinner.swapCursor(data) == null) {
 				SharedPreferences pref = getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE);
 				if(pref.contains(PREF_KEY_CONTACT_ID)) {
 					long selectedContactId = pref.getLong(PREF_KEY_CONTACT_ID, -9999999L);
 					for(int i = 0 ; i < mSpinnerForContact.getCount() ; i++) {
 						if(mSpinnerForContact.getItemIdAtPosition(i) == selectedContactId) {
 							mSpinnerForContact.setSelection(i);
 							break;
 						}
 					}
 				}
 			}
 			// setup Map
 			Map<Long, String> id2nameNew = new HashMap<Long, String>(data.getCount());
 			int indexId = data.getColumnIndex(Contract.Contacts._ID);
 			int indexName = data.getColumnIndex(Contract.Contacts.COLUMN_NAME_NAME);
 			data.moveToPosition(-1);
 			while(data.moveToNext()) {
 				id2nameNew.put(data.getLong(indexId), data.getString(indexName));
 			}
 			mContactIdToNameMap = id2nameNew;
 
 			mAdapterForLogList.notifyDataSetChanged();
 		}
 
 		@Override
 		public void onLoaderReset(Loader<Cursor> loader) {
 			mAdapterForContactsSpinner.swapCursor(null);
 			mContactIdToNameMap = null;
 		}
 	};
 	protected final LoaderCallbacks<Cursor> mLoaderCallbacks = new LoaderCallbacks<Cursor>() {
 		@Override
 		public Loader<Cursor> onCreateLoader(int id, Bundle args) {
 			// This class only has one Loader, so we don't care about the ID.
 			// First, pick the base URI to use depending on whether we are
 			// currently filtering.
 			Uri baseUri = null;
 //			if (mFilter != null) {
 //				baseUri = Uri.withAppendedPath(People.CONTENT_FILTER_URI, Uri.encode(mFilter));
 //			} else {
 			baseUri = Contract.Messages.MESSAGES_URI;
 //			}
 
 			String selection =
 					"( " + mSelectionForStatus[mSpinnerForStatus.getSelectedItemPosition()] +
 					" ) AND ( " + mSelectionForCode[mSpinnerForCode.getSelectedItemPosition()]
 					+ " )";
 			if(mAdapterForContactsSpinner.getPositionWithoutHeader(
 					mSpinnerForContact.getSelectedItemPosition()) >= 0) {
 				selection += " AND ( " + Contract.Messages.COLUMN_NAME_CONTACT_ID + " = " +
 					mSpinnerForContact.getSelectedItemId() + " )";
 			}
 
 			String sortOrder = Contract.Messages.COLUMN_NAME_TIME + " DESC";
 			// Now create and return a CursorLoader that will take care of
 			// creating a Cursor for the data being displayed.
 			return new CursorLoader(getActivity(), baseUri, null, selection, null, sortOrder);
 		}
 
 		@Override
 		public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
 			// Swap the new cursor in.  (The framework will take care of closing the
 			// old cursor once we return.)
 			mAdapterForLogList.swapCursor(data);
 
 			// The list should now be shown.
 			if (isResumed()) {
 				setListShown(true);
 			} else {
 				setListShownNoAnimation(true);
 			}
 		}
 
 		@Override
 		public void onLoaderReset(Loader<Cursor> loader) {
 			// This is called when the last Cursor provided to onLoadFinished()
 			// above is about to be closed.  We need to make sure we are no
 			// longer using it.
 			mAdapterForLogList.swapCursor(null);
 		}
 	};
 
 	/** 用于接收 已修改筛选条件 的事件监听器*/
 	protected final OnItemSelectedListener mOnSpinnerItemSelectedListener =
 			new OnItemSelectedListener() {
 		@Override
 		public void onItemSelected(AdapterView<?> parent, View view,
 				int position, long id) {
 			// 重新加载日志
 			getLoaderManager().restartLoader(LOADER_ID_LOG, null, mLoaderCallbacks);
 		}
 		@Override
 		public void onNothingSelected(AdapterView<?> parent) {}
 	};
 
 	protected class LogAdapter extends CursorAdapter {
 		private final Context mContext;
 		private final LayoutInflater mInflater;
 
 		public LogAdapter(Context context, Cursor c, int flags) {
 			super(context, c, flags);
 			mContext = context;
 			mInflater = LayoutInflater.from(context);
 		}
 
 		@Override
 		public View newView(Context context, Cursor cursor, ViewGroup parent) {
 			View rootView = mInflater.inflate(R.layout.fragment_log_list_item, parent, false);
 			// Creates a ViewHolder and store references to the two children views
 			// we want to bind data to.
 			ViewHolder holder = new ViewHolder();
 			holder.code = (TextView) rootView.findViewById(R.id.code);
 			holder.code_extra = (TextView) rootView.findViewById(R.id.code_extra);
 			holder.body = (TextView) rootView.findViewById(R.id.body);
 			holder.contact_name = (TextView) rootView.findViewById(R.id.contact_name);
 			holder.address = (TextView) rootView.findViewById(R.id.address);
 			holder.send_status = (TextView) rootView.findViewById(R.id.send_status);
 			holder.date = (TextView) rootView.findViewById(R.id.date);
 			holder.directon_icon = (ImageView) rootView.findViewById(R.id.direction_icon);
 			holder.unread_icon = (ImageView) rootView.findViewById(R.id.unread_icon);
 			rootView.setTag(holder);
 			return rootView;
 		}
 
 		@Override
 		public void bindView(View view, Context context, Cursor cursor) {
 			long contactId = cursor.getLong(cursor.getColumnIndex(Contract.Messages.COLUMN_NAME_CONTACT_ID));
 			String address = cursor.getString(cursor.getColumnIndex(Contract.Messages.COLUMN_NAME_ADDRESS));
 			Date date = null;
 			if(!cursor.isNull(cursor.getColumnIndex(Contract.Messages.COLUMN_NAME_TIME))) {
 				long time = cursor.getLong(cursor.getColumnIndex(Contract.Messages.COLUMN_NAME_TIME));
 				date = new Date(time);
 			}
 			String statusString = cursor.getString(cursor.getColumnIndex(Contract.Messages.COLUMN_NAME_STATUS));
 			Status status = statusString != null ? Status.valueOf(statusString) : null;
 			String body = cursor.getString(cursor.getColumnIndex(Contract.Messages.COLUMN_NAME_BODY));
 			Integer code = null;
 			if(!cursor.isNull(cursor.getColumnIndex(Contract.Messages.COLUMN_NAME_CODE))) {
 				code = cursor.getInt(cursor.getColumnIndex(Contract.Messages.COLUMN_NAME_CODE));
 			}
 
 			ViewHolder holder = (ViewHolder) view.getTag();
 			// message code
 			holder.code.setText(valueOfCode(code));
 			holder.code_extra.setText(valueOfCodeExtra(code, " | "));
 			setViewColor(code, view);
 			// message body
 			if(body != null)
 				holder.body.setText(body);
 			else
 				holder.body.setText("");
 			// 联系人
 			if(mContactIdToNameMap != null) {
 				String contactName = mContactIdToNameMap.get(contactId);
 				if(contactName == null)
 					contactName = "null";
 				holder.contact_name.setText(contactName);
 			}
 			// address
 			if(address != null)
 				holder.address.setText(getString(R.string.address_formatter, address));
 			else
 				holder.address.setText(R.string.unknown);
 			// date
 			if(date != null) {
 				holder.date.setVisibility(View.VISIBLE);
 				holder.date.setText(DateFormat.getDateFormat(mContext).format(date)
 						+ " " + DateFormat.getTimeFormat(mContext).format(date));
 			} else {
 				holder.date.setVisibility(View.INVISIBLE);
 			}
 			// direction (in status)
 			if(status != null) {
 				//direction
 				holder.directon_icon.setVisibility(View.VISIBLE);
 				Direction dirct = status.getDirection();
 				if(dirct == Direction.SEND)
 					holder.directon_icon.setImageResource(R.drawable.left);
 				else if(dirct == Direction.RECEIVE)
 					holder.directon_icon.setImageResource(R.drawable.right);
 				else
 					throw new IllegalStateException("unknown Direction: " + dirct.name());
 			} else {
 				holder.directon_icon.setVisibility(View.INVISIBLE);
 			}
 			holder.directon_icon.setContentDescription(valueOfDirection(status));
 			// is it unread
 			if((status == Status.UNREAD)) {
 				holder.unread_icon.setVisibility(View.VISIBLE);
 				setTextAppearance(holder, R.style.TextAppearance_AppTheme_ListItem_Strong);
 			} else {	 // 包括 就收 或 status==null 的情况
 				holder.unread_icon.setVisibility(View.INVISIBLE);
 				setTextAppearance(holder, R.style.TextAppearance_AppTheme_ListItem_Weak);
 			}
 			holder.unread_icon.setContentDescription(valueOfHasRead(status));
 			// is it sent
 			if(status == null || status.getDirection() != Direction.SEND)
 				holder.send_status.setVisibility(View.GONE);
 			else {
 				holder.send_status.setText(
 					getString(R.string.send_status_formatter, valueOfSendStatus(status)));
 				holder.send_status.setVisibility(View.VISIBLE);
 			}
 		}
 		/**
 		 * 设置每项记录视图的颜色
 		 */
 		private void setViewColor(Integer code, View rootView) {
 			Integer colorResId = null;
 			switch(getImportantCode(code)) {
 				case R.string.urgent:
 					colorResId = R.color.urgent;
 					break;
 				case R.string.respond:
 					colorResId = R.color.respond;
 					break;
 				case R.string.locate_now: case R.string.command:
 					colorResId = R.color.command;
 					break;
 				default:
 					colorResId = R.color.other_code;
 			}
 			rootView.setBackgroundColor(getResources().getColor(colorResId));
 		}
 		/**
 		 * 设置文字显示效果（颜色、大小等）
 		 * @param holder 要设置的数据项视图的{@link ViewHolder}
 		 * @param resid TextAppearance资源ID
 		 * @see TextView#setTextAppearance(Context, int)
 		 */
 		private void setTextAppearance(ViewHolder holder, int resid) {
 			resetFontStyle(holder);
 			holder.code.setTextAppearance(mContext, resid);
 			holder.code_extra.setTextAppearance(mContext, resid);
 			holder.body.setTextAppearance(mContext, resid);
 			holder.contact_name.setTextAppearance(mContext, resid);
 			holder.address.setTextAppearance(mContext, resid);
 			holder.send_status.setTextAppearance(mContext, resid);
 			holder.date.setTextAppearance(mContext, resid);
 		}
 		/**
 		 * a hack for {@link TextView#setTextAppearance(Context, int)}
 		 * can't cancel bold, when using
 		 * <a href="https://github.com/Prototik/HoloEverywhere">HoloEverywhere</a>
 		 * @see #setTextAppearance(ViewHolder, int)
 		 */
 		private void resetFontStyle(ViewHolder holder) {
 			holder.code.setFontStyle(null, FontLoader.TEXT_STYLE_NORMAL);
 			holder.code_extra.setFontStyle(null, FontLoader.TEXT_STYLE_NORMAL);
 			holder.body.setFontStyle(null, FontLoader.TEXT_STYLE_NORMAL);
 			holder.contact_name.setFontStyle(null, FontLoader.TEXT_STYLE_NORMAL);
 			holder.address.setFontStyle(null, FontLoader.TEXT_STYLE_NORMAL);
 			holder.send_status.setFontStyle(null, FontLoader.TEXT_STYLE_NORMAL);
 			holder.date.setFontStyle(null, FontLoader.TEXT_STYLE_NORMAL);
 		}
 
 		/**
 		 * 保存对 每项记录的视图中各元素 的引用，避免每次重复执行<code>findViewById()</code>，也方便使用
 		 * @author Team Orange
 		 */
 		private class ViewHolder {
 			TextView code;
 			TextView code_extra;
 			TextView body;
 			TextView contact_name;
 			TextView address;
 			TextView send_status;
 			TextView date;
 			ImageView directon_icon;
 			ImageView unread_icon;
 		}
 	}
 
 	/**
 	 * 带有层次结构的{@link ArrayAdapter}。低层次的item会被缩进。
 	 * @author Team Orange
 	 * @see MyHierarchicalArrayAdapter#getLevel(int)
 	 */
 	protected abstract class MyHierarchicalArrayAdapter<T> extends ArrayAdapter<T> {
 		private Integer DefaultPaddingLeft, DefaultPaddingRight, DefaultPaddingTop, DefaultPaddingBottom;
 
 		public MyHierarchicalArrayAdapter(Context context, int resource, T[] objects) {
 			super(context, resource, objects);
 		}
 
 		@Override
 		public View getDropDownView(int position, View convertView,
 				ViewGroup parent) {
 			View view = super.getDropDownView(position, convertView, parent);
 			if(DefaultPaddingLeft == null) {
 				DefaultPaddingLeft = view.getPaddingLeft();
 				DefaultPaddingRight = view.getPaddingRight();
 				DefaultPaddingTop = view.getPaddingTop();
 				DefaultPaddingBottom = view.getPaddingBottom();
 			}
 			view.setPadding(DefaultPaddingLeft * getLevel(position),
 					DefaultPaddingTop, DefaultPaddingRight, DefaultPaddingBottom);
 			return view;
 		}
 
 		/**
 		 * 取得位置为position的item的层级
 		 * @param position 待判定层次的item的position
 		 * @return 如果此item是最高层（类似&lt;h1&gt;）返回1；第二层返回2。以此类推
 		 */
 		protected abstract int getLevel(int position);
 	}
 	/**
 	 * 带有标题的{@link SimpleCursorAdapter}
 	 * <p>
 	 * <strong>Note</strong>：这是一个特化的{@link SimpleCursorAdapter}，此类是用于{@link Spinner}的{@link SpinnerAdapter}，
 	 * 其layout已经设置为了{@link R.layout#simple_spinner_item}，
 	 * 其DropDownViewResource已设置为{@link R.layout#simple_spinner_dropdown_item}。
 	 * @author Team Orange
 	 * @see SimpleCursorAdapter#SimpleCursorAdapter(Context, int, Cursor, String[], int[], int)
 	 * @see SimpleCursorAdapter#setDropDownViewResource(int)
 	 */
 	protected class MySimpleCursorAdapterWithHeader extends SimpleCursorAdapter {
 		private final String[] mHeader;
 		private final LayoutInflater mInflater;
 
 		public MySimpleCursorAdapterWithHeader(Context context,
 				Cursor c, String[] from, int flags, String[] header) {
 			super(context, R.layout.simple_spinner_item, c, from,
 					new int[]{android.R.id.text1}, flags);
 			setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
 			mHeader = header;
 			mInflater = LayoutInflater.from(context);
 		}
 
 		@Override
 		public int getCount() {
 			return super.getCount() + mHeader.length;
 		}
 
 		@Override
 		public Object getItem(int position) {
 			if(!isHeader(position))
 				return super.getItem(getPositionWithoutHeader(position));
 			else
 				return mHeader[position];
 		}
 
 		@Override
 		public long getItemId(int position) {
 			if(!isHeader(position))
 				return super.getItemId(getPositionWithoutHeader(position));
 			else
 				return position - mHeader.length;
 		}
 
 		@Override
 		public View getView(int position, View convertView, ViewGroup parent) {
 			if(!isHeader(position))
 				return super.getView(getPositionWithoutHeader(position), convertView, parent);
 			else {
 				if(convertView == null)
 					convertView = mInflater.inflate(R.layout.simple_spinner_item, parent, false);
 				((TextView)convertView.findViewById(android.R.id.text1)).setText(mHeader[position]);
 				return convertView;
 			}
 		}
 
 		@Override
 		public View getDropDownView(int position, View convertView,
 				ViewGroup parent) {
 			if(!isHeader(position))
 				return super.getDropDownView(getPositionWithoutHeader(position), convertView, parent);
 			else {
 				if(convertView == null)
 					convertView = mInflater.inflate(R.layout.simple_spinner_dropdown_item, parent, false);
 				((CheckedTextView)convertView.findViewById(android.R.id.text1)).setText(mHeader[position]);
 				return convertView;
 			}
 		}
 
 		@Override
 		public int getViewTypeCount() {
 			return 1;
 		}
 		@Override
 		public int getItemViewType(int position) {
 			return 0;
 		}
 
 		public boolean isHeader(int position) {
 			return position < mHeader.length;
 		}
 		public int getPositionWithoutHeader(int rawPosition) {
 			return rawPosition - mHeader.length;
 		}
 	}
 }
