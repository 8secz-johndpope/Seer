 package com.matburt.mobileorg.Gui;
 
 import android.content.Context;
 import android.database.Cursor;
 import android.graphics.Color;
 import android.text.Spannable;
 import android.text.SpannableString;
 import android.text.SpannableStringBuilder;
 import android.text.style.ForegroundColorSpan;
 import android.view.LayoutInflater;
 import android.view.View;
 import android.view.ViewGroup;
 import android.widget.LinearLayout;
 import android.widget.SimpleCursorAdapter;
 import android.widget.TextView;
 
 import com.matburt.mobileorg.R;
 import com.matburt.mobileorg.Parsing.OrgDatabase;
 
 public class OutlineCursorAdapter extends SimpleCursorAdapter {
 
 	private OrgDatabase db;
 	private Cursor cursor;
 	
 	public OutlineCursorAdapter(Context context, Cursor cursor, OrgDatabase db) {
 		super(context, R.layout.outline_item, cursor, new String[] {}, new int[] {});
 		this.cursor = cursor;
 		this.db = db;
 	}
 	
 	@Override
 	public long getItemId(int position) {
 		cursor.moveToPosition(position);
 		return cursor.getInt(cursor.getColumnIndex("_id"));
 	}
 
 	@Override
 	public View newView(Context context, Cursor cursor, ViewGroup parent) {
 		Cursor c = getCursor();
 
 		final LayoutInflater inflater = LayoutInflater.from(context);
 		View v = inflater.inflate(R.layout.outline_item, parent, false);
 
 		bindView(v, context, c);
 
 		return v;
 	}
 
 	@Override
 	public void bindView(View v, Context context, Cursor c) {
		
 		TextView orgItem = (TextView) v.findViewById(R.id.orgItem);
//		LinearLayout tagsLayout = (LinearLayout) v
//				.findViewById(R.id.tagsLayout);
//		TextView dateInfo = (TextView) v.findViewById(R.id.dateInfo);
 
 		int nameColumn = c.getColumnIndex("name");
 		int todoColumn = c.getColumnIndex("todo");
 		int priorityColumn = c.getColumnIndex("priority");
 
 		String todo = c.getString(todoColumn);
 		String name = c.getString(nameColumn);
 		String priority = c.getString(priorityColumn);
 		
 		SpannableStringBuilder itemText = new SpannableStringBuilder(name);
 		
 		if (priority != null && !priority.isEmpty()) {
 			Spannable prioritySpan = new SpannableString(priority + " ");
 			prioritySpan.setSpan(new ForegroundColorSpan(Color.YELLOW), 0,
 					priority.length(), 0);
 			itemText.insert(0, prioritySpan);
 		}
 		
 		if(!todo.isEmpty()) {
 			Spannable todoSpan = new SpannableString(todo + " ");
 			
 			if(db.isTodoActive(todo))
 				todoSpan.setSpan(new ForegroundColorSpan(Color.RED), 0,
 						todo.length(), 0);
 			else
 				todoSpan.setSpan(new ForegroundColorSpan(Color.GREEN), 0,
 						todo.length(), 0);
 			itemText.insert(0, todoSpan);
 		}
 			
 		orgItem.setText(itemText);
 
		//
 		// // Setup date view
 		// //if (TextUtils.isEmpty(dateInfo)) {
 		// holder.dateInfo.setVisibility(View.GONE);
 		// // } else {
 		// // holder.dateInfo.setText(dateInfo);
 		// // holder.dateInfo.setVisibility(View.VISIBLE);
 		// // }
 		//
 		// // Add tag view(s)
 		// holder.tagsLayout.removeAllViews();
 		// for (String tag : node.getTags()) {
 		// TextView tagView = new TextView(this.context);
 		// tagView.setText(tag);
 		// tagView.setTextColor(Color.LTGRAY);
 		// tagView.setPadding(0, 0, 5, 0);
 		// holder.tagsLayout.addView(tagView);
 		// }
 
 	}
 
 	/**
 	 * Used as part of the holding pattern.
 	 * 
 	 * The idea is to save the findViewById()'s into this container object to
 	 * speed up the list adapter. setTag() and getTag() are used to bind and
 	 * retrieve the container.
 	 * 
 	 */
 	static class ViewHolder {
 		TextView orgItem;
 		TextView todoState;
 		TextView priorityState;
 		LinearLayout tagsLayout;
 		TextView dateInfo;
 	}
 }
