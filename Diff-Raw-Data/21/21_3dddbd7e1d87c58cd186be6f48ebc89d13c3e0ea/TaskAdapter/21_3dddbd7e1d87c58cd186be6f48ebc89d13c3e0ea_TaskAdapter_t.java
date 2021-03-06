 package com.todoroo.astrid.adapter;
 
 import java.util.Date;
 import java.util.HashMap;
 import java.util.LinkedHashSet;
 
 import android.app.Activity;
 import android.content.Context;
 import android.content.Intent;
 import android.content.res.Resources;
 import android.database.Cursor;
 import android.graphics.Paint;
 import android.text.Html;
 import android.view.ContextMenu;
 import android.view.ContextMenu.ContextMenuInfo;
 import android.view.LayoutInflater;
 import android.view.View;
 import android.view.View.OnClickListener;
 import android.view.View.OnCreateContextMenuListener;
 import android.view.ViewGroup;
 import android.widget.Button;
 import android.widget.CheckBox;
 import android.widget.CursorAdapter;
 import android.widget.LinearLayout;
 import android.widget.LinearLayout.LayoutParams;
 import android.widget.ListView;
 import android.widget.TextView;
 
 import com.timsu.astrid.R;
 import com.todoroo.andlib.data.Property;
 import com.todoroo.andlib.data.TodorooCursor;
 import com.todoroo.andlib.service.Autowired;
 import com.todoroo.andlib.service.DependencyInjectionService;
 import com.todoroo.andlib.service.ExceptionService;
 import com.todoroo.andlib.utility.DateUtilities;
 import com.todoroo.andlib.utility.DialogUtilities;
 import com.todoroo.astrid.activity.TaskEditActivity;
 import com.todoroo.astrid.activity.TaskListActivity;
 import com.todoroo.astrid.api.AstridApiConstants;
 import com.todoroo.astrid.api.DetailExposer;
 import com.todoroo.astrid.api.TaskDetail;
 import com.todoroo.astrid.model.Task;
 import com.todoroo.astrid.notes.NoteDetailExposer;
 import com.todoroo.astrid.repeats.RepeatDetailExposer;
 import com.todoroo.astrid.rmilk.MilkDetailExposer;
 import com.todoroo.astrid.service.TaskService;
 import com.todoroo.astrid.tags.TagDetailExposer;
 import com.todoroo.astrid.utility.Preferences;
 
 /**
  * Adapter for displaying a user's tasks as a list
  *
  * @author Tim Su <tim@todoroo.com>
  *
  */
 public class TaskAdapter extends CursorAdapter {
 
     public interface OnCompletedTaskListener {
         public void onCompletedTask(Task item, boolean newState);
     }
 
     // --- other constants
 
     /** Properties that need to be read from the action item */
     public static final Property<?>[] PROPERTIES = new Property<?>[] {
         Task.ID,
         Task.TITLE,
         Task.IMPORTANCE,
         Task.DUE_DATE,
         Task.COMPLETION_DATE,
         Task.HIDE_UNTIL,
         Task.DELETION_DATE,
     };
 
     /** Internal Task Detail exposers */
     public static final DetailExposer[] EXPOSERS = new DetailExposer[] {
         new TagDetailExposer(),
         new RepeatDetailExposer(),
         new NoteDetailExposer(),
         new MilkDetailExposer(),
     };
 
     private static int[] IMPORTANCE_COLORS = null;
 
     // --- instance variables
 
     @Autowired
     ExceptionService exceptionService;
 
     @Autowired
     TaskService taskService;
 
     @Autowired
     DialogUtilities dialogUtilities;
 
     @Autowired
     Boolean debug;
 
     protected final Activity activity;
     protected final HashMap<Long, Boolean> completedItems;
     protected final HashMap<Long, LinkedHashSet<TaskDetail>> detailCache;
     public boolean isFling = false;
     private final int resource;
     private final LayoutInflater inflater;
     protected OnCompletedTaskListener onCompletedTaskListener = null;
     private final int fontSize;
 
     /**
      * Constructor
      *
      * @param activity
      * @param resource
      *            layout resource to inflate
      * @param c
      *            database cursor
      * @param autoRequery
      *            whether cursor is automatically re-queried on changes
      * @param onCompletedTaskListener
      *            task listener. can be null
      */
     public TaskAdapter(Activity activity, int resource,
             TodorooCursor<Task> c, boolean autoRequery,
             OnCompletedTaskListener onCompletedTaskListener) {
         super(activity, c, autoRequery);
         DependencyInjectionService.getInstance().inject(this);
 
         inflater = (LayoutInflater) activity.getSystemService(
                 Context.LAYOUT_INFLATER_SERVICE);
 
         this.resource = resource;
         this.activity = activity;
         this.onCompletedTaskListener = onCompletedTaskListener;
 
         completedItems = new HashMap<Long, Boolean>();
         detailCache = new HashMap<Long, LinkedHashSet<TaskDetail>>();
         fontSize = Preferences.getIntegerFromString(R.string.p_fontSize);
 
         IMPORTANCE_COLORS = Task.getImportanceColors(activity.getResources());
     }
 
     /* ======================================================================
      * =========================================================== view setup
      * ====================================================================== */
 
     /** Creates a new view for use in the list view */
     @Override
     public View newView(Context context, Cursor cursor, ViewGroup parent) {
         View view = inflater.inflate(resource, parent, false);
 
         // create view holder
         ViewHolder viewHolder = new ViewHolder();
         viewHolder.task = new Task();
         viewHolder.nameView = (TextView)view.findViewById(R.id.title);
         viewHolder.completeBox = (CheckBox)view.findViewById(R.id.completeBox);
         viewHolder.dueDate = (TextView)view.findViewById(R.id.dueDate);
         viewHolder.details = (LinearLayout)view.findViewById(R.id.details);
         viewHolder.actions = (LinearLayout)view.findViewById(R.id.actions);
         viewHolder.importance = (View)view.findViewById(R.id.importance);
         view.setTag(viewHolder);
 
         // add UI component listeners
         addListeners(view);
 
         // populate view content
         bindView(view, context, cursor);
 
         return view;
     }
 
     /** Populates a view with content */
     @Override
     public void bindView(View view, Context context, Cursor c) {
         TodorooCursor<Task> cursor = (TodorooCursor<Task>)c;
         Task actionItem = ((ViewHolder)view.getTag()).task;
         actionItem.readFromCursor(cursor);
 
         setFieldContentsAndVisibility(view);
         setTaskAppearance(view, actionItem.isCompleted());
     }
 
     /** Helper method to set the visibility based on if there's stuff inside */
     private static void setVisibility(TextView v) {
         if(v.getText().length() > 0)
             v.setVisibility(View.VISIBLE);
         else
             v.setVisibility(View.GONE);
     }
 
     /**
      * View Holder saves a lot of findViewById lookups.
      *
      * @author Tim Su <tim@todoroo.com>
      *
      */
     public static class ViewHolder {
         public Task task;
         public TextView nameView;
         public CheckBox completeBox;
         public TextView dueDate;
         public LinearLayout details;
         public View importance;
         public LinearLayout actions;
         public boolean expanded;
     }
 
     /** Helper method to set the contents and visibility of each field */
     public synchronized void setFieldContentsAndVisibility(View view) {
         Resources r = activity.getResources();
         ViewHolder viewHolder = (ViewHolder)view.getTag();
         Task task = viewHolder.task;
 
         // name
         final TextView nameView = viewHolder.nameView; {
             String nameValue = task.getValue(Task.TITLE);
             long hiddenUntil = task.getValue(Task.HIDE_UNTIL);
             if(task.getValue(Task.DELETION_DATE) > 0)
                 nameValue = r.getString(R.string.TAd_deletedFormat, nameValue);
             if(hiddenUntil > DateUtilities.now())
                 nameValue = r.getString(R.string.TAd_hiddenFormat, nameValue);
             nameView.setText(nameValue);
         }
 
         // complete box
         final CheckBox completeBox = viewHolder.completeBox; {
             // show item as completed if it was recently checked
             if(completedItems.containsKey(task.getId()))
                 task.setValue(Task.COMPLETION_DATE, DateUtilities.now());
             completeBox.setChecked(task.isCompleted());
         }
 
         // due date / completion date
         final TextView dueDateView = viewHolder.dueDate; {
             if(!task.isCompleted() && task.hasDueDate()) {
                 long dueDate = task.getValue(Task.DUE_DATE);
                 long secondsLeft = dueDate - DateUtilities.now();
                 if(secondsLeft > 0) {
                     dueDateView.setTextAppearance(activity, R.style.TextAppearance_TAd_ItemDueDate);
                 } else {
                     dueDateView.setTextAppearance(activity, R.style.TextAppearance_TAd_ItemDueDate_Overdue);
                 }
 
                 String dateValue;
                 Date dueDateAsDate = DateUtilities.unixtimeToDate(dueDate);
                 if (task.hasDueTime()) {
                     dateValue = DateUtilities.getDateWithTimeFormat(activity).format(dueDateAsDate);
                 } else {
                     dateValue = DateUtilities.getDateFormat(activity).format(dueDateAsDate);
                 }
                 dueDateView.setText(dateValue);
                 setVisibility(dueDateView);
             } else if(task.isCompleted()) {
                 String dateValue = DateUtilities.getDateFormat(activity).format(task.getValue(Task.COMPLETION_DATE));
                 dueDateView.setText(r.getString(R.string.TAd_completed, dateValue));
                 dueDateView.setTextAppearance(activity, R.style.TextAppearance_TAd_ItemDetails);
                 setVisibility(dueDateView);
 
             } else {
                 dueDateView.setVisibility(View.GONE);
             }
         }
 
         // other information - send out a request for it (only if not fling)
         final LinearLayout detailsView = viewHolder.details;
         if(!isFling) {
             detailsView.removeViews(2, detailsView.getChildCount() - 2);
             if(detailCache.containsKey(task.getId())) {
                 LinkedHashSet<TaskDetail> details = detailCache.get(task.getId());
                 for(TaskDetail detail : details)
                     detailsView.addView(detailToView(detail));
             } else {
                 retrieveDetails(detailsView, task.getId());
             }
         }
 
         // importance bar - must be set at end when view height is determined
         final View importanceView = viewHolder.importance; {
             int value = task.getValue(Task.IMPORTANCE);
             importanceView.setBackgroundColor(IMPORTANCE_COLORS[value]);
         }
     }
 
     /**
      * Retrieve task details
      */
     private void retrieveDetails(final LinearLayout view, final long taskId) {
         final LinkedHashSet<TaskDetail> details = new LinkedHashSet<TaskDetail>();
         detailCache.put(taskId, details);
 
         // read internal details directly
         new Thread() {
             @Override
             public void run() {
                 for(DetailExposer exposer : EXPOSERS) {
                    final TaskDetail detail = exposer.getTaskDetails(activity, taskId);
                     if(detail == null || details.contains(detail))
                         continue;
                     details.add(detail);
                    activity.runOnUiThread(new Runnable() {
                        public void run() {
                            ViewHolder holder = (ViewHolder)view.getTag();
                            if(holder != null && holder.task.getId() != taskId)
                                return;
                            view.addView(detailToView(detail));
                        };
                    });
                 }
             }
         }.start();
 
         Intent broadcastIntent = new Intent(AstridApiConstants.BROADCAST_REQUEST_DETAILS);
         broadcastIntent.putExtra(AstridApiConstants.EXTRAS_TASK_ID, taskId);
         activity.sendOrderedBroadcast(broadcastIntent, AstridApiConstants.PERMISSION_READ);
     }
 
     /**
      * Called to tell the cache to be cleared
      */
     public void flushDetailCache() {
         detailCache.clear();
     }
 
     /**
      * Respond to a request to add details for a task
      *
      * @param taskId
      */
     public synchronized void addDetails(ListView list, long taskId, TaskDetail detail) {
         if(detail == null)
             return;
 
         LinkedHashSet<TaskDetail> details = detailCache.get(taskId);
         if(details == null || details.contains(detail))
             return;
 
         details.add(detail);
 
         // update view if it is visible
         int length = list.getChildCount();
         for(int i = 0; i < length; i++) {
             ViewHolder viewHolder = (ViewHolder) list.getChildAt(i).getTag();
             if(viewHolder == null || viewHolder.task.getId() != taskId)
                 continue;
 
             TextView newView = detailToView(detail);
             viewHolder.details.addView(newView);
             break;
         }
     }
 
     /**
      * Create a new view for the given detail
      *
      * @param detail
      */
     @SuppressWarnings("nls")
     private TextView detailToView(TaskDetail detail) {
         TextView textView = new TextView(activity);
         textView.setTextAppearance(activity, R.style.TextAppearance_TAd_ItemDetails);
         textView.setText(Html.fromHtml(detail.text.replace("\n", "<br>")));
         if(detail.color != 0)
             textView.setTextColor(detail.color);
         return textView;
     }
 
     private final View.OnClickListener completeBoxListener = new View.OnClickListener() {
         public void onClick(View v) {
             View container = (View) v.getParent();
             Task task = ((ViewHolder)container.getTag()).task;
 
             completeTask(task, ((CheckBox)v).isChecked());
             // set check box to actual action item state
             setTaskAppearance(container, task.isCompleted());
         }
     };
 
     protected ContextMenuListener listener = new ContextMenuListener();
     /**
      * Set listeners for this view. This is called once per view when it is
      * created.
      */
     private void addListeners(final View container) {
         // check box listener
         final CheckBox completeBox = ((CheckBox)container.findViewById(R.id.completeBox));
         completeBox.setOnClickListener(completeBoxListener);
 
         // context menu listener
         container.setOnCreateContextMenuListener(listener);
 
         // tap listener
         container.setOnClickListener(listener);
     }
 
     /* ======================================================================
      * ======================================================= event handlers
      * ====================================================================== */
 
 
     class ContextMenuListener implements OnCreateContextMenuListener, OnClickListener {
 
         public void onCreateContextMenu(ContextMenu menu, View v,
                 ContextMenuInfo menuInfo) {
             // this is all a big sham. it's actually handled in Task List Activity
         }
 
         @Override
         public void onClick(View v) {
             final ViewHolder viewHolder = (ViewHolder)v.getTag();
             viewHolder.expanded = !viewHolder.expanded;
             LinearLayout actions = viewHolder.actions;
             actions.setVisibility(viewHolder.expanded ? View.VISIBLE : View.GONE);
             if(viewHolder.expanded && actions.getChildCount() == 0) {
                 LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                         LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1f);
                 Button edit = new Button(activity);
                 edit.setText(R.string.TAd_actionEditTask);
                 edit.setLayoutParams(params);
                 edit.setOnClickListener(new OnClickListener() {
                     @Override
                     public void onClick(View view) {
                         Intent intent = new Intent(activity, TaskEditActivity.class);
                         intent.putExtra(TaskEditActivity.ID_TOKEN, viewHolder.task.getId());
                         activity.startActivityForResult(intent, TaskListActivity.ACTIVITY_EDIT_TASK);
                     }
                 });
                 actions.addView(edit);
             }
         }
     }
 
     /**
      * Call me when the parent presses trackpad
      */
     public void onTrackpadPressed(View container) {
         if(container == null)
             return;
 
         final CheckBox completeBox = ((CheckBox)container.findViewById(R.id.completeBox));
         completeBox.performClick();
     }
 
     /** Helper method to adjust a tasks' appearance if the task is completed or
      * uncompleted.
      *
      * @param actionItem
      * @param name
      * @param progress
      */
     void setTaskAppearance(View container, boolean state) {
         CheckBox completed = (CheckBox)container.findViewById(R.id.completeBox);
         TextView name = (TextView)container.findViewById(R.id.title);
 
         completed.setChecked(state);
 
         if(state) {
             name.setPaintFlags(name.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
             name.setTextAppearance(activity, R.style.TextAppearance_TAd_ItemTitle_Completed);
         } else {
             name.setPaintFlags(name.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
             name.setTextAppearance(activity, R.style.TextAppearance_TAd_ItemTitle);
         }
         name.setTextSize(fontSize);
     }
 
     /**
      * This method is called when user completes a task via check box or other
      * means
      *
      * @param container
      *            container for the action item
      * @param newState
      *            state that this task should be set to
      * @param completeBox
      *            the box that was clicked. can be null
      */
     protected void completeTask(final Task actionItem, final boolean newState) {
         if(actionItem == null)
             return;
 
         if (newState != actionItem.isCompleted()) {
             completedItems.put(actionItem.getId(), newState);
             taskService.setComplete(actionItem, newState);
 
             if(onCompletedTaskListener != null)
                 onCompletedTaskListener.onCompletedTask(actionItem, newState);
         }
     }
 
 }
