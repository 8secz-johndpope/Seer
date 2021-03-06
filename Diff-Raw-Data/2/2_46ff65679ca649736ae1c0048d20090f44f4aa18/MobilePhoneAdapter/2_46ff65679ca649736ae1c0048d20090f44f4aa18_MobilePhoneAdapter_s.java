 package de.ub0r.android.andGMXsms;
 
 import android.content.ContentResolver;
 import android.content.Context;
 import android.database.Cursor;
 import android.database.DatabaseUtils;
 import android.provider.BaseColumns;
 import android.provider.Contacts.PeopleColumns;
 import android.provider.Contacts.Phones;
 import android.provider.Contacts.PhonesColumns;
 import android.view.View;
 import android.widget.ResourceCursorAdapter;
 import android.widget.TextView;
 
 /**
  * CursorAdapter getting Name, Phone from DB.
  * 
  * @author flx
  */
 public class MobilePhoneAdapter extends ResourceCursorAdapter {
 
 	/** INDEX: name. */
 	public static final int NAME_INDEX = 1;
 	/** INDEX: number. */
 	public static final int NUMBER_INDEX = 2;
 	/** INDEX: type. */
 	public static final int NUMBER_TYPE = 3;
 
 	/** Sort Order. */
 	private static final String SORT_ORDER = PeopleColumns.STARRED + " DESC, "
 			+ PeopleColumns.TIMES_CONTACTED + " DESC, " + PeopleColumns.NAME
 			+ " ASC, " + PhonesColumns.TYPE;
 
 	/** Global ContentResolver. */
 	private ContentResolver mContentResolver;
 
 	/** Cursor's projection. */
 	private static final String[] PROJECTION = { //  
 	BaseColumns._ID, // 0
 			PeopleColumns.NAME, // 1
 			PhonesColumns.NUMBER, // 2
 			PhonesColumns.TYPE // 3
 	};
 
 	/**
 	 * Constructor.
 	 * 
 	 * @param context
 	 *            context
 	 */
 	public MobilePhoneAdapter(final Context context) {
 		super(context, R.layout.recipient_dropdown_item, null);
 		this.mContentResolver = context.getContentResolver();
 	}
 
 	/**
 	 * Run when View is binded.
 	 * 
 	 * @param view
 	 *            view to bind to
 	 * @param context
 	 *            context
 	 * @param cursor
 	 *            cursor
 	 */
 	@Override
 	public final void bindView(final View view, final Context context,
 			final Cursor cursor) {
 		((TextView) view.findViewById(R.id.text1)).setText(cursor
 				.getString(NAME_INDEX));
 		((TextView) view.findViewById(R.id.text2)).setText(cursor
 				.getString(NUMBER_INDEX));
 		((TextView) view.findViewById(R.id.text3))
 				.setText(context.getResources().getStringArray(
						android.R.array.phoneTypes)[cursor.getInt(NUMBER_TYPE)]);
 	}
 
 	/**
 	 * Convert cursors data to String.
 	 * 
 	 * @param cursor
 	 *            cursor
 	 * @return string
 	 */
 	@Override
 	public final String convertToString(final Cursor cursor) {
 		String name = cursor.getString(NAME_INDEX);
 		String number = cursor.getString(NUMBER_INDEX);
 		if (name == null || name.length() == 0) {
 			return number;
 		}
 		return name + " (" + number + ')';
 	}
 
 	/**
 	 * Run Query to update data.
 	 * 
 	 * @param constraint
 	 *            filter string
 	 * @return cursor
 	 */
 	@Override
 	public final Cursor runQueryOnBackgroundThread(final CharSequence constraint) {
 		String where = null;
 
 		if (constraint != null) {
 			String filter = DatabaseUtils.sqlEscapeString('%' + constraint
 					.toString() + '%');
 
 			StringBuilder s = new StringBuilder();
 			s.append("(" + PeopleColumns.NAME + " LIKE ");
 			s.append(filter);
 			s.append(") OR (" + PhonesColumns.NUMBER + " LIKE ");
 			s.append(filter);
 			s.append(")");
 
 			where = s.toString();
 		}
 
 		return this.mContentResolver.query(Phones.CONTENT_URI, PROJECTION,
 				where, null, SORT_ORDER);
 	}
 }
