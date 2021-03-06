 /**
  * Copyright (c) 2012 Todoroo Inc
  *
  * See the file "LICENSE" for the full license governing this code.
  */
 package com.todoroo.astrid.ui;
 
 import android.app.Activity;
 import android.text.TextUtils;
 import android.view.View;
 import android.view.View.OnClickListener;
 import android.view.animation.Animation;
 import android.view.animation.ScaleAnimation;
 import android.widget.EditText;
 
 import com.timsu.astrid.R;
 import com.todoroo.andlib.service.Autowired;
 import com.todoroo.andlib.service.DependencyInjectionService;
 import com.todoroo.astrid.adapter.TaskAdapter;
 import com.todoroo.astrid.data.Task;
 import com.todoroo.astrid.helper.TaskEditControlSet;
 import com.todoroo.astrid.repeats.RepeatControlSet.RepeatChangedListener;
 import com.todoroo.astrid.service.TaskService;
 import com.todoroo.astrid.ui.ImportanceControlSet.ImportanceChangedListener;
 
 /**
  * Control set for mapping a Property to an EditText
  * @author Tim Su <tim@todoroo.com>
  *
  */
 public class EditTitleControlSet extends TaskEditControlSet implements ImportanceChangedListener, RepeatChangedListener {
     private EditText editText;
     protected CheckableImageView completeBox;
     private final int editTextId;
 
     private boolean isRepeating;
     private int importanceValue;
 
     @Autowired
     private TaskService taskService;
 
 
     public EditTitleControlSet(Activity activity, int layout, int editText) {
         super(activity, layout);
         this.editTextId = editText;
         DependencyInjectionService.getInstance().inject(this);
     }
 
     @Override
     protected void afterInflate() {
         this.editText = (EditText) getView().findViewById(editTextId);
         this.completeBox = (CheckableImageView) getView().findViewById(R.id.completeBox);
     }
 
     @Override
     protected void readFromTaskOnInitialize() {
         editText.setTextKeepState(model.getValue(Task.TITLE));
         completeBox.setChecked(model.isCompleted());
         completeBox.setOnClickListener(new OnClickListener() {
             @Override
             public void onClick(View v) {
                 ScaleAnimation scaleAnimation = new ScaleAnimation(1.5f, 1.0f, 1.5f, 1.0f,
                         Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                 scaleAnimation.setDuration(100);
                 // set check box to actual action item state
                 completeBox.startAnimation(scaleAnimation);
             }
         });
     }
 
     @Override
     protected String writeToModelAfterInitialized(Task task) {
         task.setValue(Task.TITLE, editText.getText().toString());
         boolean newState = completeBox.isChecked();
         if (newState != task.isCompleted()) {
             taskService.setComplete(task, newState);
         }
         return null;
     }
 
     @Override
     public void importanceChanged(int i, int color) {
         importanceValue = i;
         updateCompleteBox();
     }
 
 
     @Override
     public void repeatChanged(boolean repeat) {
         isRepeating = repeat;
         updateCompleteBox();
 
     }
 
     @Override
     public void readFromTask(Task task) {
         super.readFromTask(task);
         isRepeating = !TextUtils.isEmpty(task.getValue(Task.RECURRENCE));
         importanceValue = model.getValue(Task.IMPORTANCE);
     }
 
 
     private void updateCompleteBox() {
         int valueToUse = importanceValue;
        if (valueToUse >= TaskAdapter.IMPORTANCE_RESOURCES.length)
            valueToUse = TaskAdapter.IMPORTANCE_RESOURCES.length - 1;
        if(valueToUse < TaskAdapter.IMPORTANCE_RESOURCES.length) {
             if (isRepeating) {
                completeBox.setImageResource(TaskAdapter.IMPORTANCE_REPEAT_RESOURCES[valueToUse]);
             } else {
                completeBox.setImageResource(TaskAdapter.IMPORTANCE_RESOURCES[valueToUse]);
             }
         }
     }
 
 }
