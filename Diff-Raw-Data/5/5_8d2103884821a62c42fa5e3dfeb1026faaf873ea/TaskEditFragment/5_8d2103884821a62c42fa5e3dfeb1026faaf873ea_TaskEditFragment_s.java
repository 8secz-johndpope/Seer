 /**
  * Copyright (c) 2012 Todoroo Inc
  *
  * See the file "LICENSE" for the full license governing this code.
  */
 package com.todoroo.astrid.activity;
 
 import java.io.File;
 import java.io.FileOutputStream;
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Locale;
 import java.util.concurrent.atomic.AtomicReference;
 
 import android.app.Activity;
 import android.app.AlertDialog;
 import android.app.Dialog;
 import android.content.ContentValues;
 import android.content.DialogInterface;
 import android.content.Intent;
 import android.content.res.Resources.Theme;
 import android.graphics.Bitmap;
 import android.os.Bundle;
 import android.os.Handler;
 import android.speech.RecognizerIntent;
 import android.support.v4.app.Fragment;
 import android.support.v4.app.SupportActivity;
 import android.support.v4.view.Menu;
 import android.support.v4.view.MenuItem;
 import android.support.v4.view.ViewPager;
 import android.text.TextUtils;
 import android.util.DisplayMetrics;
 import android.util.Log;
 import android.util.TypedValue;
 import android.view.KeyEvent;
 import android.view.LayoutInflater;
 import android.view.MenuInflater;
 import android.view.View;
 import android.view.View.MeasureSpec;
 import android.view.View.OnClickListener;
 import android.view.ViewGroup;
 import android.view.ViewGroup.LayoutParams;
 import android.view.ViewParent;
 import android.webkit.MimeTypeMap;
 import android.widget.ArrayAdapter;
 import android.widget.Button;
 import android.widget.EditText;
 import android.widget.FrameLayout;
 import android.widget.ImageButton;
 import android.widget.LinearLayout;
 import android.widget.ScrollView;
 import android.widget.TextView;
 import android.widget.Toast;
 
 import com.timsu.astrid.R;
 import com.todoroo.andlib.service.Autowired;
 import com.todoroo.andlib.service.DependencyInjectionService;
 import com.todoroo.andlib.service.ExceptionService;
 import com.todoroo.andlib.utility.AndroidUtilities;
 import com.todoroo.andlib.utility.DateUtilities;
 import com.todoroo.andlib.utility.DialogUtilities;
 import com.todoroo.andlib.utility.Preferences;
 import com.todoroo.astrid.actfm.ActFmCameraModule;
 import com.todoroo.astrid.actfm.ActFmCameraModule.CameraResultCallback;
 import com.todoroo.astrid.actfm.CommentsActivity;
 import com.todoroo.astrid.actfm.EditPeopleControlSet;
 import com.todoroo.astrid.actfm.TaskCommentsFragment;
 import com.todoroo.astrid.actfm.sync.ActFmPreferenceService;
 import com.todoroo.astrid.actfm.sync.ActFmSyncService;
 import com.todoroo.astrid.api.AstridApiConstants;
 import com.todoroo.astrid.dao.TaskAttachmentDao;
 import com.todoroo.astrid.data.RemoteModel;
 import com.todoroo.astrid.data.Task;
 import com.todoroo.astrid.data.TaskAttachment;
 import com.todoroo.astrid.files.AACRecordingActivity;
 import com.todoroo.astrid.files.FileExplore;
 import com.todoroo.astrid.files.FileUtilities;
 import com.todoroo.astrid.files.FilesControlSet;
 import com.todoroo.astrid.gcal.GCalControlSet;
 import com.todoroo.astrid.helper.TaskEditControlSet;
 import com.todoroo.astrid.notes.EditNoteActivity;
 import com.todoroo.astrid.opencrx.OpencrxControlSet;
 import com.todoroo.astrid.opencrx.OpencrxCoreUtils;
 import com.todoroo.astrid.reminders.Notifications;
 import com.todoroo.astrid.repeats.RepeatControlSet;
 import com.todoroo.astrid.service.AddOnService;
 import com.todoroo.astrid.service.MetadataService;
 import com.todoroo.astrid.service.StatisticsConstants;
 import com.todoroo.astrid.service.StatisticsService;
 import com.todoroo.astrid.service.TaskService;
 import com.todoroo.astrid.service.ThemeService;
 import com.todoroo.astrid.tags.TagsControlSet;
 import com.todoroo.astrid.taskrabbit.TaskRabbitControlSet;
 import com.todoroo.astrid.timers.TimerActionControlSet;
 import com.todoroo.astrid.timers.TimerControlSet;
 import com.todoroo.astrid.timers.TimerPlugin;
 import com.todoroo.astrid.ui.DateChangedAlerts;
 import com.todoroo.astrid.ui.DeadlineControlSet;
 import com.todoroo.astrid.ui.EditNotesControlSet;
 import com.todoroo.astrid.ui.EditTitleControlSet;
 import com.todoroo.astrid.ui.HideUntilControlSet;
 import com.todoroo.astrid.ui.ImportanceControlSet;
 import com.todoroo.astrid.ui.NestableScrollView;
 import com.todoroo.astrid.ui.NestableViewPager;
 import com.todoroo.astrid.ui.PopupControlSet;
 import com.todoroo.astrid.ui.ReminderControlSet;
 import com.todoroo.astrid.ui.TaskEditMoreControls;
 import com.todoroo.astrid.ui.WebServicesView;
 import com.todoroo.astrid.utility.Constants;
 import com.todoroo.astrid.utility.Flags;
 import com.todoroo.astrid.voice.VoiceInputAssistant;
 import com.todoroo.astrid.voice.VoiceRecognizer;
 import com.viewpagerindicator.TabPageIndicator;
 
 /**
  * This activity is responsible for creating new tasks and editing existing
  * ones. It saves a task when it is paused (screen rotated, back button pressed)
  * as long as the task has a title.
  *
  * @author timsu
  *
  */
 public final class TaskEditFragment extends Fragment implements
 ViewPager.OnPageChangeListener, EditNoteActivity.UpdatesChangedListener {
 
     public static final String TAG_TASKEDIT_FRAGMENT = "taskedit_fragment"; //$NON-NLS-1$
 
     // --- bundle tokens
 
     /**
      * Task ID
      */
     public static final String TOKEN_ID = "id"; //$NON-NLS-1$
 
     /**
      * Content Values to set
      */
     public static final String TOKEN_VALUES = "v"; //$NON-NLS-1$
 
     public static final String TOKEN_OPEN_CONTROL = "open_control"; //$NON-NLS-1$
 
     /**
      * Task in progress (during orientation change)
      */
     private static final String TASK_IN_PROGRESS = "task_in_progress"; //$NON-NLS-1$
 
     /**
      * Task remote id (during orientation change)
      */
     private static final String TASK_UUID = "task_uuid"; //$NON-NLS-1$
 
     /**
      * Token for saving a bitmap in the intent before it has been added with a comment
      */
     public static final String TOKEN_PICTURE_IN_PROGRESS = "picture_in_progress"; //$NON-NLS-1$
 
     /**
      * Tab to start on
      */
     public static final String TOKEN_TAB = "tab"; //$NON-NLS-1$
 
     // --- request codes
 
     public static final int REQUEST_LOG_IN = 0;
     private static final int REQUEST_VOICE_RECOG = 10;
     public static final int REQUEST_CODE_CONTACT = 20;
     public static final int REQUEST_CODE_RECORD = 30;
     public static final int REQUEST_CODE_ATTACH_FILE = 40;
     public static final int REQUEST_CODE_BEAST_MODE = 50;
 
     // --- menu codes
 
     private static final int MENU_SAVE_ID = R.string.TEA_menu_save;
     private static final int MENU_DISCARD_ID = R.string.TEA_menu_discard;
     private static final int MENU_COMMENTS_REFRESH_ID = R.string.TEA_menu_refresh_comments;
     private static final int MENU_SHOW_COMMENTS_ID = R.string.TEA_menu_comments;
     private static final int MENU_ATTACH_ID = R.string.premium_attach_file;
     private static final int MENU_RECORD_ID = R.string.premium_record_audio;
 
     // --- result codes
 
     public static final int RESULT_CODE_SAVED = Activity.RESULT_FIRST_USER;
     public static final int RESULT_CODE_DISCARDED = Activity.RESULT_FIRST_USER + 1;
     public static final int RESULT_CODE_DELETED = Activity.RESULT_FIRST_USER + 2;
 
     public static final String OVERRIDE_FINISH_ANIM = "finishAnim"; //$NON-NLS-1$
 
     public static final String TOKEN_TASK_WAS_ASSIGNED = "task_assigned"; //$NON-NLS-1$
 
     public static final String TOKEN_ASSIGNED_TO_DISPLAY = "task_assigned_to_display"; //$NON-NLS-1$
     public static final String TOKEN_ASSIGNED_TO_EMAIL = "task_assigned_to_email"; //$NON-NLS-1$
     public static final String TOKEN_ASSIGNED_TO_ID = "task_assigned_to_id"; //$NON-NLS-1$
     public static final String TOKEN_TAGS_CHANGED = "tags_changed";  //$NON-NLS-1$
     public static final String TOKEN_NEW_REPEATING_TASK = "new_repeating"; //$NON-NLS-1$
 
     // --- services
 
     public static final int TAB_VIEW_UPDATES = 0;
     public static final int TAB_VIEW_MORE = 1;
     public static final int TAB_VIEW_WEB_SERVICES = 2;
 
     @Autowired
     private ExceptionService exceptionService;
 
     @Autowired
     private TaskService taskService;
 
     @Autowired
     private MetadataService metadataService;
 
     @Autowired
     private TaskAttachmentDao taskAttachmentDao;
 
     @Autowired
     private AddOnService addOnService;
 
     @Autowired
     private ActFmPreferenceService actFmPreferenceService;
 
     @Autowired
     private ActFmSyncService actFmSyncService;
 
     // --- UI components
 
     private ImageButton voiceAddNoteButton;
 
     private EditPeopleControlSet peopleControlSet = null;
     private TaskRabbitControlSet taskRabbitControl = null;
     private EditNotesControlSet notesControlSet = null;
     private HideUntilControlSet hideUntilControls = null;
     private TagsControlSet tagsControlSet = null;
     private FilesControlSet filesControlSet = null;
     private TimerActionControlSet timerAction;
     private EditText title;
     private TaskEditMoreControls moreControls;
     private EditNoteActivity editNotes;
     private NestableViewPager mPager;
     private TabPageIndicator mIndicator;
     private HashMap<String, TaskEditControlSet> controlSetMap = new HashMap<String, TaskEditControlSet>();
 
     private final List<TaskEditControlSet> controls = Collections.synchronizedList(new ArrayList<TaskEditControlSet>());
 
     // --- other instance variables
 
     /** true if editing started with a new task */
     private boolean isNewTask = false;
 
     /** task model */
     Task model = null;
 
     /** whether task should be saved when this activity exits */
     private boolean shouldSaveState = true;
 
     /** voice assistant for notes-creation */
     private VoiceInputAssistant voiceNoteAssistant;
 
     private EditText notesEditText;
 
     private Dialog whenDialog;
 
     private boolean overrideFinishAnim;
 
     private String uuid = RemoteModel.NO_UUID;
 
     private boolean showEditComments;
 
     private int commentIcon = R.drawable.comment_dark_blue;
 
     private WebServicesView webServices = null;
 
     private int tabStyle = 0;
 
     private boolean moreSectionHasControls;
 
     /*
      * ======================================================================
      * ======================================================= initialization
      * ======================================================================
      */
 
     /**
      * Container Activity must implement this interface and we ensure that it
      * does during the onAttach() callback
      */
     public interface OnTaskEditDetailsClickedListener {
         public void onTaskEditDetailsClicked(int category, int position);
     }
 
     @Override
     public void onAttach(SupportActivity activity) {
         super.onAttach(activity);
     }
 
     public TaskEditFragment() {
         DependencyInjectionService.getInstance().inject(this);
     }
 
     @Override
     public void onCreate(Bundle savedInstanceState) {
         super.onCreate(savedInstanceState);
 
         // if we were editing a task already, restore it
         if (savedInstanceState != null
                 && savedInstanceState.containsKey(TASK_IN_PROGRESS)) {
             Task task = savedInstanceState.getParcelable(TASK_IN_PROGRESS);
             if (task != null) {
                 model = task;
             }
             if (savedInstanceState.containsKey(TASK_UUID)) {
                 uuid = savedInstanceState.getString(TASK_UUID);
             }
         }
 
         showEditComments = Preferences.getBoolean(R.string.p_show_task_edit_comments, true);
 
         TypedValue tv = new TypedValue();
         getActivity().getTheme().resolveAttribute(R.attr.asCommentButtonImg, tv, false);
         commentIcon = tv.data;
 
         getActivity().setResult(Activity.RESULT_OK);
     }
 
     /*
      * ======================================================================
      * ==================================================== UI initialization
      * ======================================================================
      */
 
     @Override
     public View onCreateView(LayoutInflater inflater, ViewGroup container,
             Bundle savedInstanceState) {
         super.onCreateView(inflater, container, savedInstanceState);
 
         View v = inflater.inflate(R.layout.task_edit_activity, container, false);
 
         return v;
     }
 
     @Override
     public void onActivityCreated(Bundle savedInstanceState) {
         super.onActivityCreated(savedInstanceState);
 
         // We have a menu item to show in action bar.
         setHasOptionsMenu(true);
 
         AstridActivity activity = (AstridActivity) getActivity();
 
         setUpUIComponents();
         adjustInfoPopovers();
 
         Preferences.setBoolean(R.string.p_showed_tap_task_help, true);
 
         overrideFinishAnim = false;
         if (activity != null) {
             if (activity.getIntent() != null)
                 overrideFinishAnim = activity.getIntent().getBooleanExtra(
                         OVERRIDE_FINISH_ANIM, true);
         }
 
         if (activity instanceof TaskListActivity)
             ((TaskListActivity) activity).setCommentsButtonVisibility(false);
     }
 
     private void instantiateEditNotes() {
         if (showEditComments) {
             long idParam = getActivity().getIntent().getLongExtra(TOKEN_ID, -1L);
             editNotes = new EditNoteActivity(this, getView(),
                     idParam);
             editNotes.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.FILL_PARENT,
                     LayoutParams.WRAP_CONTENT));
 
             editNotes.addListener(this);
         }
     }
 
     private void loadMoreContainer() {
         View moreTab = (View) getView().findViewById(R.id.more_container);
         View commentsBar = (View) getView().findViewById(R.id.updatesFooter);
 
         long idParam = getActivity().getIntent().getLongExtra(TOKEN_ID, -1L);
 
         boolean hasTitle = !TextUtils.isEmpty(model.getValue(Task.TITLE));
 
         if(hasTitle && Preferences.getBoolean(R.string.p_ideas_tab_enabled, false) && Constants.MARKET_STRATEGY.allowIdeasTab())
             tabStyle = (TaskEditViewPager.TAB_SHOW_ACTIVITY | TaskEditViewPager.TAB_SHOW_WEB);
         else
             tabStyle = TaskEditViewPager.TAB_SHOW_ACTIVITY;
 
         if (!showEditComments)
             tabStyle &= ~TaskEditViewPager.TAB_SHOW_ACTIVITY;
 
         if (moreSectionHasControls)
             tabStyle |= TaskEditViewPager.TAB_SHOW_MORE;
 
         if (editNotes == null) {
             instantiateEditNotes();
         } else {
             editNotes.loadViewForTaskID(idParam);
         }
 
         if (timerAction != null && editNotes != null) {
             timerAction.removeListener(editNotes);
             timerAction.addListener(editNotes);
         }
 
         if (editNotes != null)
             editNotes.addListener(this);
 
 
         if(hasTitle) {
             if(webServices == null) {
                 webServices = new WebServicesView(getActivity());
                 webServices.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.FILL_PARENT,
                         LayoutParams.WRAP_CONTENT));
                 webServices.setPadding(10, 5, 10, 10);
                 webServices.taskRabbitControl = taskRabbitControl;
                 webServices.setTask(model);
             } else {
                 webServices.refresh();
             }
         }
 
         if (tabStyle == 0) {
             return;
         }
 
         TaskEditViewPager adapter = new TaskEditViewPager(getActivity(), tabStyle);
         adapter.parent = this;
 
         mPager = (NestableViewPager) getView().findViewById(R.id.pager);
         mPager.setAdapter(adapter);
 
         mIndicator = (TabPageIndicator) getView().findViewById(
                 R.id.indicator);
         mIndicator.setViewPager(mPager);
         mIndicator.setOnPageChangeListener(this);
 
         if (moreControls.getParent() != null && moreControls.getParent() != mPager) {
             ((ViewGroup) moreControls.getParent()).removeView(moreControls);
         }
 
         if (showEditComments)
             commentsBar.setVisibility(View.VISIBLE);
         moreTab.setVisibility(View.VISIBLE);
         setCurrentTab(TAB_VIEW_UPDATES);
         setPagerHeightForPosition(TAB_VIEW_UPDATES);
         Handler handler = new Handler();
         handler.postDelayed(new Runnable() {
             @Override
             public void run() {
                 updatesChanged();
             }
         }, 500L);
     }
 
     private void setCurrentTab(int position) {
         if(mIndicator == null)
             return;
 
         mIndicator.setCurrentItem(position);
         mPager.setCurrentItem(position);
     }
 
     /** Initialize UI components */
     private void setUpUIComponents() {
 
         LinearLayout basicControls = (LinearLayout) getView().findViewById(
                 R.id.basic_controls);
         LinearLayout titleControls = (LinearLayout) getView().findViewById(
                 R.id.title_controls);
         LinearLayout whenDialogView = (LinearLayout) LayoutInflater.from(
                 getActivity()).inflate(R.layout.task_edit_when_controls, null);
         moreControls = (TaskEditMoreControls) LayoutInflater.from(getActivity()).inflate(
                 R.layout.task_edit_more_controls, null);
 
         constructWhenDialog(whenDialogView);
 
         controlSetMap = new HashMap<String, TaskEditControlSet>();
 
         // populate control set
         EditTitleControlSet editTitle = new EditTitleControlSet(getActivity(),
                 R.layout.control_set_title, R.id.title);
         title = (EditText) editTitle.getView().findViewById(R.id.title);
         controls.add(editTitle);
         titleControls.addView(editTitle.getDisplayView(), 0, new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1.0f));
 
         timerAction = new TimerActionControlSet(
                 getActivity(), getView());
         controls.add(timerAction);
 
         tagsControlSet = new TagsControlSet(getActivity(),
                 R.layout.control_set_tags,
                 R.layout.control_set_default_display, R.string.TEA_tags_label_long);
         controls.add(tagsControlSet);
         controlSetMap.put(getString(R.string.TEA_ctrl_lists_pref),
                 tagsControlSet);
 
         // EditPeopleControlSet relies on the "tags" transitory created by the
         // TagsControlSet, so we put the tags control before the people control
         // EditPeopleControlSet also relies on taskRabbitControl set being added
         // that way it can tell if it needs to show task rabbit in the spinner
 
         peopleControlSet = new EditPeopleControlSet(getActivity(), this,
                 R.layout.control_set_assigned,
                 R.layout.control_set_default_display,
                 R.string.actfm_EPA_assign_label_long, REQUEST_LOG_IN);
         if(Locale.getDefault().getCountry().equals("US")) { //$NON-NLS-1$
             taskRabbitControl = new TaskRabbitControlSet(this, R.layout.control_set_default_display);
             controls.add(taskRabbitControl);
             peopleControlSet.addListener(taskRabbitControl);
         }
         controls.add(peopleControlSet);
         controlSetMap.put(getString(R.string.TEA_ctrl_who_pref),
                 peopleControlSet);
 
         RepeatControlSet repeatControls = new RepeatControlSet(getActivity(),
                 R.layout.control_set_repeat,
                 R.layout.control_set_repeat_display, R.string.repeat_enabled);
 
         GCalControlSet gcalControl = new GCalControlSet(getActivity(),
                 R.layout.control_set_gcal, R.layout.control_set_gcal_display,
                 R.string.gcal_TEA_addToCalendar_label);
 
         // The deadline control set contains the repeat controls and the
         // calendar controls.
         // NOTE: we add the gcalControl AFTER the
         // deadline control, because
         // otherwise the correct date may not be written to the calendar event.
         // Order matters!
         DeadlineControlSet deadlineControl = new DeadlineControlSet(
                 getActivity(), R.layout.control_set_deadline,
                 R.layout.control_set_deadline_display, repeatControls,
                 repeatControls.getDisplayView(), gcalControl.getDisplayView());
         controlSetMap.put(getString(R.string.TEA_ctrl_when_pref),
                 deadlineControl);
         controls.add(repeatControls);
         repeatControls.addListener(editTitle);
         controls.add(deadlineControl);
         controls.add(gcalControl);
 
         ImportanceControlSet importanceControl = new ImportanceControlSet(
                 getActivity(), R.layout.control_set_importance);
         controls.add(importanceControl);
         importanceControl.addListener(editTitle);
         controlSetMap.put(getString(R.string.TEA_ctrl_importance_pref),
                 importanceControl);
 
         notesControlSet = new EditNotesControlSet(getActivity(),
                 R.layout.control_set_notes, R.layout.control_set_notes_display);
         notesEditText = (EditText) notesControlSet.getView().findViewById(
                 R.id.notes);
         controls.add(notesControlSet);
         controlSetMap.put(getString(R.string.TEA_ctrl_notes_pref),
                 notesControlSet);
 
         ReminderControlSet reminderControl = new ReminderControlSet(
                 getActivity(), R.layout.control_set_reminders,
                 R.layout.control_set_default_display);
         controls.add(reminderControl);
         controlSetMap.put(getString(R.string.TEA_ctrl_reminders_pref),
                 reminderControl);
 
         hideUntilControls = new HideUntilControlSet(getActivity(),
                 R.layout.control_set_hide,
                 R.layout.control_set_default_display,
                 R.string.hide_until_prompt);
         controls.add(hideUntilControls);
         reminderControl.addViewToBody(hideUntilControls.getDisplayView());
 
         // TODO: Fix the fact that hideUntil doesn't update accordingly with date changes when lazy loaded. Until then, don't lazy load.
         hideUntilControls.getView();
 
         TimerControlSet timerControl = new TimerControlSet(getActivity(),
                 R.layout.control_set_timers,
                 R.layout.control_set_default_display,
                 R.string.TEA_timer_controls);
         timerAction.addListener(timerControl);
         controls.add(timerControl);
         controlSetMap.put(getString(R.string.TEA_ctrl_timer_pref), timerControl);
 
         filesControlSet = new FilesControlSet(getActivity(),
                 R.layout.control_set_files,
                 R.layout.control_set_files_display,
                 R.string.TEA_control_files);
         controls.add(filesControlSet);
         controlSetMap.put(getString(R.string.TEA_ctrl_files_pref), filesControlSet);
 
         try {
             if (OpencrxCoreUtils.INSTANCE.isLoggedIn()) {
                 OpencrxControlSet ocrxControl = new OpencrxControlSet(
                         getActivity(), R.layout.control_set_opencrx,
                         R.layout.control_set_opencrx_display,
                         R.string.opencrx_TEA_opencrx_title);
                 controls.add(ocrxControl);
                 basicControls.addView(ocrxControl.getDisplayView());
                 notesEditText.setHint(R.string.opencrx_TEA_notes);
             }
         } catch (Exception e) {
             Log.e("astrid-error", "loading-control-set", e); //$NON-NLS-1$ //$NON-NLS-2$
         }
 
         setupBeastModeButton();
 
         getView().findViewById(R.id.delete_task).setOnClickListener(new OnClickListener() {
             @Override
             public void onClick(View v) {
                 deleteButtonClick();
             }
         });
 
         loadEditPageOrder(false);
 
         // Load task data in background
         new TaskEditBackgroundLoader().start();
     }
 
     private void setupBeastModeButton() {
         TextView beastMode = (TextView) getView().findViewById(R.id.edit_beast_mode);
         TypedValue tv = new TypedValue();
         Theme theme = getActivity().getTheme();
         theme.resolveAttribute(R.attr.asTextColor, tv, false);
 
         int color = tv.data & 0x00ffffff;
         color = color + 0x7f000000;
         beastMode.setTextColor(color);
 
         beastMode.setOnClickListener(new OnClickListener() {
             @Override
             public void onClick(View v) {
                 Intent intent = new Intent(getActivity(), BeastModePreferences.class);
                 intent.setAction(AstridApiConstants.ACTION_SETTINGS);
                 startActivityForResult(intent, REQUEST_CODE_BEAST_MODE);
             }
         });
     }
 
     private void loadEditPageOrder(boolean removeViews) {
         LinearLayout basicControls = (LinearLayout) getView().findViewById(
                 R.id.basic_controls);
         if (removeViews) {
             basicControls.removeAllViews();
             moreControls.removeAllViews();
         }
 
         ArrayList<String> controlOrder = BeastModePreferences.constructOrderedControlList(getActivity());
         String[] itemOrder = controlOrder.toArray(new String[controlOrder.size()]);
 
         String moreSectionTrigger = getString(R.string.TEA_ctrl_more_pref);
         String hideAlwaysTrigger = getString(R.string.TEA_ctrl_hide_section_pref);
         LinearLayout section = basicControls;
 
         moreSectionHasControls = false;
 
         Class<?> openControl = (Class<?>) getActivity().getIntent().getSerializableExtra(TOKEN_OPEN_CONTROL);
 
         for (int i = 0; i < itemOrder.length; i++) {
             String item = itemOrder[i];
             if (item.equals(hideAlwaysTrigger)) {
                 break; // As soon as we hit the hide section, we're done
             } else if (item.equals(moreSectionTrigger)) {
                 section = moreControls;
                 if (taskRabbitControl != null) {
                     taskRabbitControl.getDisplayView().setVisibility(View.GONE);
                     section.addView(taskRabbitControl.getDisplayView());
                 }
 
             } else {
                 View controlSet = null;
                 TaskEditControlSet curr = controlSetMap.get(item);
 
                 if (curr != null)
                     controlSet = (LinearLayout) curr.getDisplayView();
 
                 if (controlSet != null) {
                     if ((i + 1 >= itemOrder.length || itemOrder[i + 1].equals(moreSectionTrigger))) {
                         removeTeaSeparator(controlSet);
                     }
                     section.addView(controlSet);
                     if (section == moreControls)
                         moreSectionHasControls = true;
                 }
 
                 if (curr != null && curr.getClass().equals(openControl) && curr instanceof PopupControlSet) {
                     ((PopupControlSet) curr).getDisplayView().performClick();
                 }
             }
         }
 
         getActivity().getIntent().removeExtra(TOKEN_OPEN_CONTROL);
     }
 
     private void removeTeaSeparator(View view) {
 
         View teaSeparator = view.findViewById(R.id.TEA_Separator);
 
         if (teaSeparator != null) {
             teaSeparator.setVisibility(View.GONE);
         }
     }
 
     private void constructWhenDialog(View whenDialogView) {
         int theme = ThemeService.getEditDialogTheme();
         whenDialog = new Dialog(getActivity(), theme);
 
         Button dismissDialogButton = (Button) whenDialogView.findViewById(R.id.when_dismiss);
         dismissDialogButton.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View v) {
                 DialogUtilities.dismissDialog(getActivity(), whenDialog);
             }
         });
 
         DisplayMetrics metrics = new DisplayMetrics();
         getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
         whenDialog.setTitle(R.string.TEA_when_dialog_title);
         whenDialog.addContentView(whenDialogView, new LayoutParams(
                 metrics.widthPixels - (int) (30 * metrics.density),
                 LayoutParams.WRAP_CONTENT));
     }
 
     /**
      * Initialize task edit page in the background
      *
      * @author Tim Su <tim@todoroo.com>
      *
      */
     private class TaskEditBackgroundLoader extends Thread {
 
         public void onUiThread() {
             // prepare and set listener for voice-button
             if (getActivity() != null) {
                 if (VoiceRecognizer.voiceInputAvailable(getActivity())) {
                     voiceAddNoteButton = (ImageButton) notesControlSet.getView().findViewById(
                             R.id.voiceAddNoteButton);
                     voiceAddNoteButton.setVisibility(View.VISIBLE);
                     int prompt = R.string.voice_edit_note_prompt;
                     voiceNoteAssistant = new VoiceInputAssistant(voiceAddNoteButton, REQUEST_VOICE_RECOG);
                     voiceNoteAssistant.setAppend(true);
                     voiceNoteAssistant.setLanguageModel(RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                     voiceNoteAssistant.configureMicrophoneButton(TaskEditFragment.this, prompt);
                 }
                 loadMoreContainer();
             }
         }
 
         @Override
         public void run() {
             AndroidUtilities.sleepDeep(500L);
 
             Activity activity = getActivity();
             if (activity == null)
                 return;
 
             activity.runOnUiThread(new Runnable() {
                 public void run() {
                     onUiThread();
                 }
             });
 
         }
     }
 
     /*
      * ======================================================================
      * =============================================== model reading / saving
      * ======================================================================
      */
 
     /**
      * Loads action item from the given intent
      *
      * @param intent
      */
     @SuppressWarnings("nls")
     protected void loadItem(Intent intent) {
         if (model != null) {
             // came from bundle
             setIsNewTask(model.getValue(Task.TITLE).length() == 0);
             return;
         }
 
         long idParam = intent.getLongExtra(TOKEN_ID, -1L);
         if (idParam > -1L) {
             model = taskService.fetchById(idParam, Task.PROPERTIES);
 
             if (model != null && model.containsNonNullValue(Task.UUID))
                 uuid = model.getValue(Task.UUID);
         }
 
         // not found by id or was never passed an id
         if (model == null) {
             String valuesAsString = intent.getStringExtra(TOKEN_VALUES);
             ContentValues values = null;
             try {
                 if (valuesAsString != null)
                     values = AndroidUtilities.contentValuesFromSerializedString(valuesAsString);
             } catch (Exception e) {
                 // oops, can't serialize
             }
             model = TaskService.createWithValues(values, null);
             getActivity().getIntent().putExtra(TOKEN_ID, model.getId());
         }
 
         if (model.getValue(Task.TITLE).length() == 0) {
             StatisticsService.reportEvent(StatisticsConstants.CREATE_TASK);
 
             // set deletion date until task gets a title
             model.setValue(Task.DELETION_DATE, DateUtilities.now());
         } else {
             StatisticsService.reportEvent(StatisticsConstants.EDIT_TASK);
         }
         setIsNewTask(model.getValue(Task.TITLE).length() == 0);
 
         if (model == null) {
             exceptionService.reportError("task-edit-no-task",
                     new NullPointerException("model"));
             getActivity().onBackPressed();
             return;
         }
 
         // clear notification
         Notifications.cancelNotifications(model.getId());
 
     }
 
     public long getTaskIdInProgress() {
         if (model != null && model.getId() > 0)
             return model.getId();
         return getActivity().getIntent().getLongExtra(TOKEN_ID, -1);
     }
 
     private void setIsNewTask(boolean isNewTask) {
         this.isNewTask = isNewTask;
         Activity activity = getActivity();
         if (activity instanceof TaskEditActivity) {
             ((TaskEditActivity) activity).updateTitle(isNewTask);
         }
     }
 
     /** Convenience method to populate fields after setting model to null */
     public void repopulateFromScratch(Intent intent) {
         model = null;
         uuid = RemoteModel.NO_UUID;
         populateFields(intent);
         if (webServices != null) {
             webServices.setTask(model);
             webServices.reset();
         }
         loadMoreContainer();
     }
 
     /** Populate UI component values from the model */
     public void populateFields(Intent intent) {
         loadItem(intent);
 
         synchronized (controls) {
             if (!taskAttachmentDao.taskHasAttachments(model.getUuid())) {
                 filesControlSet.getDisplayView().setVisibility(View.GONE);
             }
             for (TaskEditControlSet controlSet : controls)
                 controlSet.readFromTask(model);
         }
 
     }
 
     /** Populate UI component values from the model */
     private void populateFields() {
         populateFields(getActivity().getIntent());
     }
 
     /** Save task model from values in UI components */
     public void save(boolean onPause) {
         if (title == null)
             return;
 
         if (title.getText().length() > 0)
             model.setValue(Task.DELETION_DATE, 0L);
 
         if (title.getText().length() == 0)
             return;
 
         StringBuilder toast = new StringBuilder();
         synchronized (controls) {
             for (TaskEditControlSet controlSet : controls) {
                 if (controlSet instanceof PopupControlSet) { // Save open control set
                     PopupControlSet popup = (PopupControlSet) controlSet;
                     Dialog d = popup.getDialog();
                     if (d != null && d.isShowing()) {
                         getActivity().getIntent().putExtra(TOKEN_OPEN_CONTROL, popup.getClass());
                     }
                 }
                 String toastText = controlSet.writeToModel(model);
                 if (toastText != null)
                     toast.append('\n').append(toastText);
             }
         }
 
         String processedToast = addDueTimeToToast(toast.toString());
         boolean cancelFinish = peopleControlSet != null
         && !peopleControlSet.saveSharingSettings(processedToast) && !onPause;
 
         boolean tagsChanged = Flags.check(Flags.TAGS_CHANGED);
         model.putTransitory(TaskService.TRANS_EDIT_SAVE, true);
         taskService.save(model);
 
         if (!onPause && !cancelFinish) {
             boolean taskEditActivity = (getActivity() instanceof TaskEditActivity);
             boolean isAssignedToMe = peopleControlSet.isAssignedToMe();
             boolean showRepeatAlert = model.getTransitory(TaskService.TRANS_REPEAT_CHANGED) != null
                     && !TextUtils.isEmpty(model.getValue(Task.RECURRENCE));
             String assignedTo = peopleControlSet.getAssignedToString();
             String assignedEmail = ""; //$NON-NLS-1$
             String assignedId = Task.USER_ID_IGNORE;
             if (Task.userIdIsEmail(model.getValue(Task.USER_ID))) {
                 assignedEmail = model.getValue(Task.USER_ID);
             }
 
             if (taskEditActivity) {
                 Intent data = new Intent();
                 if (!isAssignedToMe) {
                     data.putExtra(TOKEN_TASK_WAS_ASSIGNED, true);
                     data.putExtra(TOKEN_ASSIGNED_TO_DISPLAY, assignedTo);
                     if (!TextUtils.isEmpty(assignedEmail))
                         data.putExtra(TOKEN_ASSIGNED_TO_EMAIL, assignedEmail);
                     if (Task.isRealUserId(assignedId));
                         data.putExtra(TOKEN_ASSIGNED_TO_ID, assignedId);
                 }
                 if (showRepeatAlert) {
                     data.putExtra(TOKEN_NEW_REPEATING_TASK, model);
                 }
                 data.putExtra(TOKEN_TAGS_CHANGED, tagsChanged);
                 getActivity().setResult(Activity.RESULT_OK, data);
 
             } else {
                 // Notify task list fragment in multi-column case
                 // since the activity isn't actually finishing
                 TaskListActivity tla = (TaskListActivity) getActivity();
                 if (!isAssignedToMe)
                     tla.taskAssignedTo(assignedTo, assignedEmail, assignedId);
                 else if (showRepeatAlert)
                     DateChangedAlerts.showRepeatChangedDialog(tla, model);
 
                 if (tagsChanged)
                     tla.tagsChanged();
                 tla.refreshTaskList();
             }
 
             removeExtrasFromIntent(getActivity().getIntent());
             shouldSaveState = false;
             getActivity().onBackPressed();
 
         }
     }
 
     public boolean onKeyDown(int keyCode) {
         if (keyCode == KeyEvent.KEYCODE_BACK) {
             if (title.getText().length() == 0 || !peopleControlSet.hasLoadedUI())
                 discardButtonClick();
             else
                 saveButtonClick();
             return true;
         }
         return false;
     }
 
     @Override
     public void onDetach() {
         super.onDetach();
 
         // abandon editing and delete the newly created task if
         // no title was entered
         Activity activity = getActivity();
         if (overrideFinishAnim) {
             AndroidUtilities.callOverridePendingTransition(activity,
                     R.anim.slide_right_in, R.anim.slide_right_out);
         }
 
         if (activity instanceof TaskListActivity) {
             if (title.getText().length() == 0 && isNewTask
                     && model != null && model.isSaved()) {
                 taskService.delete(model);
             }
             ((TaskListActivity) activity).setCommentsButtonVisibility(true);
         }
     }
 
     /**
      * Helper to remove task edit specific info from activity intent
      * @param intent
      */
     public static void removeExtrasFromIntent(Intent intent) {
         if (intent != null) {
             intent.removeExtra(TaskListActivity.OPEN_TASK);
             intent.removeExtra(TOKEN_PICTURE_IN_PROGRESS);
         }
     }
 
     /*
      * ======================================================================
      * ======================================================= event handlers
      * ======================================================================
      */
 
     protected void saveButtonClick() {
         save(false);
     }
 
     /**
      * Displays a Toast reporting that the selected task has been saved and, if
      * it has a due date, that is due in 'x' amount of time, to 1 time-unit of
      * precision
      *
      * @param additionalMessage
      */
     private String addDueTimeToToast(String additionalMessage) {
         int stringResource;
 
         long due = model.getValue(Task.DUE_DATE);
         String toastMessage;
         if (due != 0) {
             stringResource = R.string.TEA_onTaskSave_due;
             CharSequence formattedDate = DateUtilities.getRelativeDay(
                     getActivity(), due);
             toastMessage = getString(stringResource, formattedDate);
         } else {
             toastMessage = getString(R.string.TEA_onTaskSave_notDue);
         }
 
         return toastMessage + additionalMessage;
     }
 
     protected void discardButtonClick() {
         shouldSaveState = false;
 
         // abandon editing in this case
         if (title.getText().length() == 0
                 || TextUtils.isEmpty(model.getValue(Task.TITLE))) {
             if (isNewTask) {
                 taskService.delete(model);
                 if (getActivity() instanceof TaskListActivity) {
                     TaskListActivity tla = (TaskListActivity) getActivity();
                     tla.refreshTaskList();
                 }
             }
         }
 
         removeExtrasFromIntent(getActivity().getIntent());
         getActivity().onBackPressed();
     }
 
     protected void deleteButtonClick() {
         new AlertDialog.Builder(getActivity()).setTitle(
                 R.string.DLG_confirm_title).setMessage(
                         R.string.DLG_delete_this_task_question).setIcon(
                                 android.R.drawable.ic_dialog_alert).setPositiveButton(
                                         android.R.string.ok, new DialogInterface.OnClickListener() {
                                             public void onClick(DialogInterface dialog, int which) {
                                                taskService.delete(model);
                                                 TimerPlugin.updateTimer(getActivity(), model, false);
                                                 shouldSaveState = false;
 
                                                 Activity a = getActivity();
                                                 if (a instanceof TaskEditActivity) {
                                                     getActivity().setResult(Activity.RESULT_OK);
                                                     getActivity().onBackPressed();
                                                 } else if (a instanceof TaskListActivity) {
                                                     discardButtonClick();
                                                     TaskListFragment tlf = ((TaskListActivity) a).getTaskListFragment();
                                                     if (tlf != null)
                                                         tlf.refresh();
                                                 }
                                             }
                                         }).setNegativeButton(android.R.string.cancel, null).show();
     }
 
     private void startAttachFile() {
         ArrayList<String> options = new ArrayList<String>();
         options.add(getString(R.string.file_add_picture));
         options.add(getString(R.string.file_add_sdcard));
 
         ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                 android.R.layout.simple_spinner_dropdown_item, options.toArray(new String[options.size()]));
 
         DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
             @Override
             public void onClick(DialogInterface d, int which) {
                 if(which == 0) {
                     ActFmCameraModule.showPictureLauncher(TaskEditFragment.this, null);
                 } else if (which == 1) {
                     Intent attachFile = new Intent(getActivity(), FileExplore.class);
                     startActivityForResult(attachFile, REQUEST_CODE_ATTACH_FILE);
                 }
             }
         };
 
         // show a menu of available options
         new AlertDialog.Builder(getActivity())
         .setAdapter(adapter, listener)
         .show().setOwnerActivity(getActivity());
     }
 
     private void startRecordingAudio() {
         Intent recordAudio = new Intent(getActivity(), AACRecordingActivity.class);
         recordAudio.putExtra(AACRecordingActivity.EXTRA_TEMP_FILE, getActivity().getFilesDir() + File.separator + "audio.aac"); //$NON-NLS-1$
         startActivityForResult(recordAudio, REQUEST_CODE_RECORD);
     }
 
     private void attachFile(String file) {
         File src = new File(file);
         if (!src.exists()) {
             Toast.makeText(getActivity(), R.string.file_err_copy, Toast.LENGTH_LONG).show();
             return;
         }
 
         File dst = new File(FileUtilities.getAttachmentsDirectory(getActivity()) + File.separator + src.getName());
         try {
             AndroidUtilities.copyFile(src, dst);
         } catch (Exception e) {
             Toast.makeText(getActivity(), R.string.file_err_copy, Toast.LENGTH_LONG).show();
             return;
         }
 
         String path = dst.getAbsolutePath();
         String name = dst.getName();
         String extension = AndroidUtilities.getFileExtension(name);
 
         String type = TaskAttachment.FILE_TYPE_OTHER;
         if (!TextUtils.isEmpty(extension)) {
             MimeTypeMap map = MimeTypeMap.getSingleton();
             String guessedType = map.getMimeTypeFromExtension(extension);
             if (!TextUtils.isEmpty(guessedType))
                 type = guessedType;
         }
 
         createNewFileAttachment(path, name, type);
     }
 
     @SuppressWarnings("nls")
     private void attachImage(Bitmap bitmap) {
 
         AtomicReference<String> nameRef = new AtomicReference<String>();
         String path = FileUtilities.getNewImageAttachmentPath(getActivity(), nameRef);
 
         try {
             FileOutputStream fos = new FileOutputStream(path);
             bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
             fos.flush();
             fos.close();
 
             createNewFileAttachment(path, nameRef.get(), TaskAttachment.FILE_TYPE_IMAGE + "png");
         } catch (Exception e) {
             Toast.makeText(getActivity(), R.string.file_err_copy, Toast.LENGTH_LONG).show();
         }
     }
 
     private void createNewFileAttachment(String path, String fileName, String fileType) {
         if (!ActFmPreferenceService.isPremiumUser())
             return;
 
         TaskAttachment attachment = TaskAttachment.createNewAttachment(model.getUuid(), path, fileName, fileType);
         taskAttachmentDao.createNew(attachment);
         filesControlSet.refreshMetadata();
         filesControlSet.getDisplayView().setVisibility(View.VISIBLE);
     }
 
     @Override
     public boolean onOptionsItemSelected(MenuItem item) {
         switch (item.getItemId()) {
         case MENU_SAVE_ID:
             saveButtonClick();
             return true;
         case MENU_DISCARD_ID:
             discardButtonClick();
             return true;
         case MENU_ATTACH_ID:
             startAttachFile();
             return true;
         case MENU_RECORD_ID:
             startRecordingAudio();
             return true;
         case MENU_COMMENTS_REFRESH_ID: {
             if (editNotes != null)
                 editNotes.refreshData();
             return true;
         }
         case MENU_SHOW_COMMENTS_ID: {
             Intent intent = new Intent(getActivity(), CommentsActivity.class);
             intent.putExtra(TaskCommentsFragment.EXTRA_TASK, model.getId());
             startActivity(intent);
             AndroidUtilities.callOverridePendingTransition(getActivity(), R.anim.slide_left_in, R.anim.slide_left_out);
             return true;
         }
         case android.R.id.home:
             if (title.getText().length() == 0)
                 discardButtonClick();
             else
                 saveButtonClick();
             return true;
         }
 
         return super.onOptionsItemSelected(item);
     }
 
     @Override
     public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
         super.onCreateOptionsMenu(menu, inflater);
         MenuItem item;
 
         if (ActFmPreferenceService.isPremiumUser()) {
             item = menu.add(Menu.NONE, MENU_ATTACH_ID, 0, R.string.premium_attach_file);
             item.setIcon(ThemeService.getDrawable(R.drawable.ic_menu_attach));
             item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
 
             item = menu.add(Menu.NONE, MENU_RECORD_ID, 0, R.string.premium_record_audio);
             item.setIcon(ThemeService.getDrawable(R.drawable.ic_menu_mic));
             item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
         }
 
         item = menu.add(Menu.NONE, MENU_DISCARD_ID, 0, R.string.TEA_menu_discard);
         item.setIcon(ThemeService.getDrawable(R.drawable.ic_menu_close));
         item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
 
         if (!(getActivity() instanceof TaskEditActivity)) {
             item = menu.add(Menu.NONE, MENU_SAVE_ID, 0, R.string.TEA_menu_save);
             item.setIcon(ThemeService.getDrawable(R.drawable.ic_menu_save));
             item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
         }
 
         boolean wouldShowComments = actFmPreferenceService.isLoggedIn() && menu.findItem(MENU_COMMENTS_REFRESH_ID) == null;
         if(wouldShowComments && showEditComments) {
             item = menu.add(Menu.NONE, MENU_COMMENTS_REFRESH_ID, Menu.NONE,
                     R.string.ENA_refresh_comments);
             item.setIcon(R.drawable.icn_menu_refresh_dark);
         } else if (wouldShowComments && !showEditComments) {
             item = menu.add(Menu.NONE, MENU_SHOW_COMMENTS_ID, Menu.NONE, R.string.TEA_menu_comments);
             item.setIcon(commentIcon);
             item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
         }
 
     }
 
     @Override
     public void onPause() {
         super.onPause();
         StatisticsService.sessionPause();
 
         if (shouldSaveState)
             save(true);
     }
 
     @Override
     public void onResume() {
         super.onResume();
         StatisticsService.sessionStart(getActivity());
         populateFields();
     }
 
     @Override
     public void onActivityResult(int requestCode, int resultCode, Intent data) {
         if (editNotes == null)
             instantiateEditNotes();
 
         if (taskRabbitControl != null && taskRabbitControl.activityResult(requestCode, resultCode, data)) {
             return;
         } else if (editNotes != null && editNotes.activityResult(requestCode, resultCode, data)) {
             return;
         } else if (requestCode == REQUEST_VOICE_RECOG
                 && resultCode == Activity.RESULT_OK) {
             // handle the result of voice recognition, put it into the
             // appropiate textfield
             voiceNoteAssistant.handleActivityResult(requestCode, resultCode, data, notesEditText);
 
             // write the voicenote into the model, or it will be deleted by
             // onResume.populateFields
             // (due to the activity-change)
             notesControlSet.writeToModel(model);
         } else if (requestCode == REQUEST_CODE_RECORD && resultCode == Activity.RESULT_OK) {
             String recordedAudioPath = data.getStringExtra(AACRecordingActivity.RESULT_OUTFILE);
             String recordedAudioName = data.getStringExtra(AACRecordingActivity.RESULT_FILENAME);
             createNewFileAttachment(recordedAudioPath, recordedAudioName, TaskAttachment.FILE_TYPE_AUDIO + "m4a"); //$NON-NLS-1$
         } else if (requestCode == REQUEST_CODE_ATTACH_FILE && resultCode == Activity.RESULT_OK) {
             attachFile(data.getStringExtra(FileExplore.RESULT_FILE_SELECTED));
         } else if (requestCode == REQUEST_CODE_BEAST_MODE) {
             loadEditPageOrder(true);
             new TaskEditBackgroundLoader().start();
             return;
         }
 
         ActFmCameraModule.activityResult(getActivity(), requestCode, resultCode, data, new CameraResultCallback() {
             @Override
             public void handleCameraResult(Bitmap bitmap) {
                 attachImage(bitmap);
             }
         });
 
         // respond to sharing logoin
         peopleControlSet.onActivityResult(requestCode, resultCode, data);
 
         super.onActivityResult(requestCode, resultCode, data);
     }
 
     @Override
     public void onSaveInstanceState(Bundle outState) {
         super.onSaveInstanceState(outState);
 
         // stick our task into the outState
         outState.putParcelable(TASK_IN_PROGRESS, model);
         outState.putString(TASK_UUID, uuid.toString());
     }
 
     @Override
     public void onStart() {
         super.onStart();
     }
 
     @Override
     public void onStop() {
         super.onStop();
         StatisticsService.sessionStop(getActivity());
     }
 
     private void adjustInfoPopovers() {
         Preferences.setBoolean(R.string.p_showed_tap_task_help, true);
         if (!Preferences.isSet(getString(R.string.p_showed_lists_help)))
             Preferences.setBoolean(R.string.p_showed_lists_help, false);
     }
 
     /*
      * ======================================================================
      * ========================================== UI component helper classes
      * ======================================================================
      */
 
     public int getTabForPosition(int position) {
         int tab = TaskEditViewPager.getPageForPosition(position, tabStyle);
         switch(tab) {
         case TaskEditViewPager.TAB_SHOW_ACTIVITY:
             return TAB_VIEW_UPDATES;
         case TaskEditViewPager.TAB_SHOW_MORE:
             return TAB_VIEW_MORE;
         case TaskEditViewPager.TAB_SHOW_WEB:
             return TAB_VIEW_WEB_SERVICES;
         }
 
         // error experienced
         return TAB_VIEW_MORE;
     }
 
     /**
      * Returns the correct view for TaskEditViewPager
      *
      * @param position
      *            in the horizontal scroll view
      */
 
     public View getPageView(int position) {
         switch(getTabForPosition(position)) {
         case TAB_VIEW_MORE:
             return moreControls;
         case TAB_VIEW_UPDATES:
             return editNotes;
         case TAB_VIEW_WEB_SERVICES:
             return webServices;
         }
 
         return null;
     }
 
     private void setPagerHeightForPosition(int position) {
         int height = 0;
 
         View view = null;
         switch(getTabForPosition(position)) {
         case TAB_VIEW_MORE:
             view = moreControls;
             break;
         case TAB_VIEW_UPDATES:
             view = editNotes;
             break;
         case TAB_VIEW_WEB_SERVICES:
             view = webServices;
         }
 
         if (view == null || mPager == null) return;
 
         int desiredWidth = MeasureSpec.makeMeasureSpec(view.getWidth(),
                 MeasureSpec.AT_MOST);
         view.measure(desiredWidth, MeasureSpec.UNSPECIFIED);
         height = Math.max(view.getMeasuredHeight(), height);;
         LayoutParams pagerParams = mPager.getLayoutParams();
         if (height > 0 && height != pagerParams.height) {
             pagerParams.height = height;
             mPager.setLayoutParams(pagerParams);
         }
     }
 
     public static void setViewHeightBasedOnChildren(LinearLayout view) {
 
         int totalHeight = 0;
         int desiredWidth = MeasureSpec.makeMeasureSpec(view.getWidth(),
                 MeasureSpec.AT_MOST);
         for (int i = 0; i < view.getChildCount(); i++) {
             View listItem = view.getChildAt(i);
             listItem.measure(desiredWidth, MeasureSpec.UNSPECIFIED);
             totalHeight += listItem.getMeasuredHeight();
         }
 
         ViewGroup.LayoutParams params = view.getLayoutParams();
         if(params == null)
             return;
 
         params.height = totalHeight;
         view.setLayoutParams(params);
         view.requestLayout();
     }
 
     // Tab Page listener when page/tab changes
     @Override
     public void onPageScrolled(int position, float positionOffset,
             int positionOffsetPixels) {
         return;
     }
 
     @Override
     public void onPageSelected(final int position) {
         final Runnable onPageSelected = new Runnable() {
             @Override
             public void run() {
                 setPagerHeightForPosition(position);
 
                 NestableScrollView scrollView = (NestableScrollView)getView().findViewById(R.id.edit_scroll);
                 if(getTabForPosition(position) == TAB_VIEW_WEB_SERVICES)
                     scrollView.setScrollabelViews(webServices.getScrollableViews());
                 else
                     scrollView.setScrollabelViews(null);
             }
         };
 
         if(getTabForPosition(position) == TAB_VIEW_WEB_SERVICES)
             webServices.onPageSelected(new Runnable() {
                 @Override
                 public void run() {
                     onPageSelected.run();
                 }
             });
         else
             onPageSelected.run();
     }
 
     @Override
     public void onPageScrollStateChanged(int state) {
         return;
     }
 
     // EditNoteActivity Listener when there are new updates/comments
     @Override
     public void updatesChanged()  {
         if (mPager != null && mPager.getCurrentItem() == TAB_VIEW_UPDATES)
             setPagerHeightForPosition(TAB_VIEW_UPDATES);
     }
 
     // EditNoteActivity Lisener when there are new updates/comments
     @Override
     public void commentAdded() {
         setCurrentTab(TAB_VIEW_UPDATES);
         setPagerHeightForPosition(TAB_VIEW_UPDATES);
         scrollToView(editNotes);
     }
 
     // Scroll to view in edit task
     public void scrollToView(View v) {
         View child = v;
         ScrollView scrollView = (ScrollView) getView().findViewById(R.id.edit_scroll);
         int top = v.getTop();
         while (!child.equals(scrollView) ) {
             top += child.getTop();
             ViewParent parentView = child.getParent();
             if (parentView != null && View.class.isInstance(parentView)) {
                 child = (View) parentView;
             }
             else {
                 break;
             }
         }
         scrollView.smoothScrollTo(0, top);
     }
 }
