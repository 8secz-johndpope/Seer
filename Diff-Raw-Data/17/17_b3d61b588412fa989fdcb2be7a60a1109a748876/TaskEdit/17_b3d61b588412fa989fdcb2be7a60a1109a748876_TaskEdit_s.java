 /*
  * ASTRID: Android's Simple Task Recording Dashboard
  *
  * Copyright (c) 2009 Tim Su
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation; either version 2 of the License, or
  * (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful, but
  * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
  * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
  * for more details.
  *
  * You should have received a copy of the GNU General Public License along
  * with this program; if not, write to the Free Software Foundation, Inc.,
  * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
  */
 package com.timsu.astrid.activities;
 
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 
 import android.app.AlertDialog;
 import android.content.Context;
 import android.content.DialogInterface;
 import android.content.res.Resources;
 import android.os.Bundle;
 import android.text.Editable;
 import android.text.TextWatcher;
 import android.util.Log;
 import android.view.LayoutInflater;
 import android.view.Menu;
 import android.view.MenuItem;
 import android.view.View;
 import android.view.ViewGroup;
 import android.widget.ArrayAdapter;
 import android.widget.AutoCompleteTextView;
 import android.widget.Button;
 import android.widget.EditText;
 import android.widget.ImageButton;
 import android.widget.LinearLayout;
 import android.widget.Spinner;
 import android.widget.TextView;
 
 import com.timsu.astrid.R;
 import com.timsu.astrid.data.enums.Importance;
 import com.timsu.astrid.data.tag.TagController;
 import com.timsu.astrid.data.tag.TagIdentifier;
 import com.timsu.astrid.data.tag.TagModelForView;
 import com.timsu.astrid.data.task.TaskIdentifier;
 import com.timsu.astrid.data.task.TaskModelForEdit;
 import com.timsu.astrid.widget.DateControlSet;
 import com.timsu.astrid.widget.TimeDurationControlSet;
 
 public class TaskEdit extends TaskModificationActivity<TaskModelForEdit> {
 
     // bundle arguments
     public static final String     TAG_NAME_TOKEN       = "tag";
 
     // menu items
     private static final int       SAVE_ID         = Menu.FIRST;
     private static final int       DISCARD_ID      = Menu.FIRST + 1;
     private static final int       DELETE_ID       = Menu.FIRST + 2;
 
     // activity results
     public static final int        RESULT_DELETE   = RESULT_FIRST_USER;
 
     // other constants
     private static final int       MAX_TAGS        = 5;
 
     // UI components
     private EditText               name;
     private Spinner                importance;
     private TimeDurationControlSet estimatedDuration;
     private TimeDurationControlSet elapsedDuration;
     private DateControlSet         definiteDueDate;
     private DateControlSet         preferredDueDate;
     private DateControlSet         hiddenUntil;
     private EditText               notes;
     private LinearLayout           tagsContainer;
 
     // other instance variables
     private boolean                shouldSaveState = true;
     private TagController          tagController;
     private List<TagModelForView> tags;
     private List<TagIdentifier>    taskTags;
 
     @Override
     protected void onCreate(Bundle savedInstanceState) {
         super.onCreate(savedInstanceState);
         tagController = new TagController(this);
         tagController.open();
         setContentView(R.layout.task_edit);
 
         setUpUIComponents();
 		setUpListeners();
 
         Bundle extras = getIntent().getExtras();
         if(extras != null && extras.containsKey(TAG_NAME_TOKEN)) {
             addTag(extras.getString(TAG_NAME_TOKEN));
         }
     }
     @Override
     protected TaskModelForEdit getModel(TaskIdentifier identifier) {
         if (identifier != null)
             return controller.fetchTaskForEdit(identifier);
         else
             return controller.createNewTaskForEdit();
     }
 
     /* ======================================================================
      * =============================================== model reading / saving
      * ====================================================================== */
 
     private void populateFields() {
         Resources r = getResources();
 
         // set UI components based on model variables
         if(model.getCursor() != null)
             startManagingCursor(model.getCursor());
         name.setText(model.getName());
         if(model.getName().length() > 0)
             setTitle(new StringBuilder().
                 append(r.getString(R.string.taskEdit_titlePrefix)).
                 append(" ").
                 append(model.getName()));
         estimatedDuration.setTimeElapsed(model.getEstimatedSeconds());
         elapsedDuration.setTimeElapsed(model.getElapsedSeconds());
         importance.setSelection(model.getImportance().ordinal());
         definiteDueDate.setDate(model.getDefiniteDueDate());
         preferredDueDate.setDate(model.getPreferredDueDate());
         hiddenUntil.setDate(model.getHiddenUntil());
         notes.setText(model.getNotes());
 
         // tags
         tags = tagController.getAllTags();
         if(model.getTaskIdentifier() != null) {
             taskTags = tagController.getTaskTags(model.getTaskIdentifier());
             if(taskTags.size() > 0) {
                 Map<TagIdentifier, TagModelForView> tagsMap =
                     new HashMap<TagIdentifier, TagModelForView>();
                 for(TagModelForView tag : tags)
                     tagsMap.put(tag.getTagIdentifier(), tag);
                 for(TagIdentifier id : taskTags) {
                     if(!tagsMap.containsKey(id))
                         continue;
 
                     TagModelForView tag = tagsMap.get(id);
                     addTag(tag.getName());
                 }
             }
         } else
             taskTags = new LinkedList<TagIdentifier>();
 
         addTag("");
     }
 
     private void save() {
         // don't save if user accidentally created a new task
         if(name.getText().length() == 0)
             return;
 
         model.setName(name.getText().toString());
         model.setEstimatedSeconds(estimatedDuration.getTimeDurationInSeconds());
         model.setElapsedSeconds(elapsedDuration.getTimeDurationInSeconds());
         model.setImportance(Importance.values()
                 [importance.getSelectedItemPosition()]);
         model.setDefiniteDueDate(definiteDueDate.getDate());
         model.setPreferredDueDate(preferredDueDate.getDate());
         model.setHiddenUntil(hiddenUntil.getDate());
         model.setNotes(notes.getText().toString());
 
         try {
             // write out to database
             controller.saveTask(model);
             saveTags();
         } catch (Exception e) {
             Log.e(getClass().getSimpleName(), "Error saving task!", e); // TODO
         }
 
     }
 
     /** Save task tags. Must be called after task already has an ID */
     private void saveTags() {
         Set<TagIdentifier> tagsToDelete;
         Set<TagIdentifier> tagsToAdd;
 
         HashSet<String> tagNames = new HashSet<String>();
         for(int i = 0; i < tagsContainer.getChildCount(); i++) {
             TextView tagName = (TextView)tagsContainer.getChildAt(i).findViewById(R.id.text1);
             if(tagName.getText().length() == 0)
                 continue;
             tagNames.add(tagName.getText().toString());
         }
 
         // map names to tag identifiers, creating them if necessary
         HashSet<TagIdentifier> tagIds = new HashSet<TagIdentifier>();
         HashMap<String, TagIdentifier> tagsByName = new HashMap<String, TagIdentifier>();
         for(TagModelForView tag : tags)
             tagsByName.put(tag.getName(), tag.getTagIdentifier());
         for(String tagName : tagNames) {
             if(tagsByName.containsKey(tagName))
                 tagIds.add(tagsByName.get(tagName));
             else {
                 TagIdentifier newTagId = tagController.createTag(tagName);
                 tagIds.add(newTagId);
             }
         }
 
         tagsToDelete = new HashSet<TagIdentifier>(taskTags);
         tagsToDelete.removeAll(tagIds);
         tagsToAdd = tagIds;
         tagsToAdd.removeAll(taskTags);
 
         for(TagIdentifier tagId : tagsToDelete)
             tagController.removeTag(model.getTaskIdentifier(), tagId);
         for(TagIdentifier tagId : tagsToAdd)
             tagController.addTag(model.getTaskIdentifier(), tagId);
     }
 
     /* ======================================================================
      * ==================================================== UI initialization
      * ====================================================================== */
 
     /** Initialize UI components */
     private void setUpUIComponents() {
         Resources r = getResources();
         setTitle(new StringBuilder()
             .append(r.getString(R.string.taskEdit_titleGeneric)));
 
         // populate instance variables
         name = (EditText)findViewById(R.id.name);
         importance = (Spinner)findViewById(R.id.importance);
         tagsContainer = (LinearLayout)findViewById(R.id.tags_container);
         estimatedDuration = new TimeDurationControlSet(this, R.id.estimatedDuration);
         elapsedDuration = new TimeDurationControlSet(this, R.id.elapsedDuration);
         definiteDueDate = new DateControlSet(this, R.id.definiteDueDate_notnull,
                 R.id.definiteDueDate_date, R.id.definiteDueDate_time);
         preferredDueDate = new DateControlSet(this, R.id.preferredDueDate_notnull,
                 R.id.preferredDueDate_date, R.id.preferredDueDate_time);
         hiddenUntil = new DateControlSet(this, R.id.hiddenUntil_notnull,
                 R.id.hiddenUntil_date, R.id.hiddenUntil_time);
         notes = (EditText)findViewById(R.id.notes);
 
         // individual ui component initialization
         ImportanceAdapter importanceAdapter = new ImportanceAdapter(this,
                     android.R.layout.simple_spinner_item,
                     R.layout.importance_spinner_dropdown,
                     Importance.values());
         importance.setAdapter(importanceAdapter);
     }
 
     /** Set up button listeners */
     private void setUpListeners() {
         Button saveButton = (Button) findViewById(R.id.save);
         saveButton.setOnClickListener(new View.OnClickListener() {
             public void onClick(View view) {
                 saveButtonClick();
             }
         });
 
         Button discardButton = (Button) findViewById(R.id.discard);
         discardButton.setOnClickListener(new View.OnClickListener() {
             public void onClick(View view) {
                 discardButtonClick();
             }
         });
 
         Button deleteButton = (Button) findViewById(R.id.delete);
         if(model.getTaskIdentifier() == null)
             deleteButton.setVisibility(View.GONE);
         else {
             deleteButton.setOnClickListener(new View.OnClickListener() {
                 public void onClick(View view) {
                     deleteButtonClick();
                 }
             });
         }
     }
 
     /** Adds a tag to the tag field */
     private boolean addTag(String tagName) {
         if (tagsContainer.getChildCount() >= MAX_TAGS) {
             return false;
         }
 
         LayoutInflater inflater = getLayoutInflater();
         final View tagItem = inflater.inflate(R.layout.edit_tag_item, null);
         tagsContainer.addView(tagItem);
 
         AutoCompleteTextView textView = (AutoCompleteTextView)tagItem.
             findViewById(R.id.text1);
         textView.setText(tagName);
         ArrayAdapter<TagModelForView> tagsAdapter =
             new ArrayAdapter<TagModelForView>(this,
                     android.R.layout.simple_dropdown_item_1line, tags);
         textView.setAdapter(tagsAdapter);
         textView.addTextChangedListener(new TextWatcher() {
             @Override
             public void onTextChanged(CharSequence s, int start, int before,
                     int count) {
                 if(start == 0 && tagsContainer.getChildAt(
                         tagsContainer.getChildCount()-1) == tagItem) {
                     addTag("");
                 }
             }
 
             @Override
             public void afterTextChanged(Editable s) {
                 //
             }
 
             @Override
             public void beforeTextChanged(CharSequence s, int start, int count,
                     int after) {
                 //
             }
         });
 
         ImageButton reminderRemoveButton;
         reminderRemoveButton = (ImageButton)tagItem.findViewById(R.id.button1);
         reminderRemoveButton.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View v) {
                 tagsContainer.removeView(tagItem);
             }
         });
 
         return true;
     }
 
     /* ======================================================================
      * ======================================================= event handlers
      * ====================================================================== */
 
     private void saveButtonClick() {
         setResult(RESULT_OK);
         finish();
     }
 
     private void discardButtonClick() {
         shouldSaveState = false;
         setResult(RESULT_CANCELED);
         finish();
     }
 
     private void deleteButtonClick() {
         new AlertDialog.Builder(this)
             .setTitle(R.string.delete_title)
             .setMessage(R.string.delete_this_task_title)
             .setIcon(android.R.drawable.ic_dialog_alert)
             .setPositiveButton(android.R.string.ok,
                     new DialogInterface.OnClickListener() {
                 @Override
                 public void onClick(DialogInterface dialog, int which) {
                     controller.deleteTask(model.getTaskIdentifier());
                     shouldSaveState = false;
                     setResult(RESULT_DELETE);
                     finish();
                 }
             })
             .setNegativeButton(android.R.string.cancel, null)
             .show();
     }
 
     @Override
     public boolean onMenuItemSelected(int featureId, MenuItem item) {
         switch(item.getItemId()) {
         case SAVE_ID:
             saveButtonClick();
             return true;
         case DISCARD_ID:
             discardButtonClick();
             return true;
         case DELETE_ID:
             deleteButtonClick();
             return true;
         }
 
         return super.onMenuItemSelected(featureId, item);
     }
 
     @Override
     public boolean onCreateOptionsMenu(Menu menu) {
         super.onCreateOptionsMenu(menu);
         MenuItem item;
 
         item = menu.add(Menu.NONE, SAVE_ID, 0, R.string.save_label);
         item.setIcon(android.R.drawable.ic_menu_save);
         item.setAlphabeticShortcut('s');
 
        if(model.getTaskIdentifier() != null) {
            item = menu.add(Menu.NONE, DISCARD_ID, 0, R.string.discard_label);
            item.setIcon(android.R.drawable.ic_menu_close_clear_cancel);
            item.setAlphabeticShortcut('c');
         }
 
        item = menu.add(Menu.NONE, DELETE_ID, 0, R.string.delete_label);
        item.setIcon(android.R.drawable.ic_menu_delete);
        item.setAlphabeticShortcut('d');

         return true;
     }
 
     @Override
     protected void onSaveInstanceState(Bundle outState) {
         save();
         super.onSaveInstanceState(outState);
     }
 
     @Override
     protected void onPause() {
         if(shouldSaveState)
             save();
         super.onPause();
     }
 
     @Override
     protected void onResume() {
         super.onResume();
         populateFields();
     }
 
     @Override
     protected void onDestroy() {
         super.onDestroy();
         tagController.close();
     }
 
     /* ======================================================================
      * ========================================== UI component helper classes
      * ====================================================================== */
 
     /** Adapter with custom view to display Importance with proper formatting */
     private class ImportanceAdapter extends ArrayAdapter<Importance> {
         private int textViewResourceId, dropDownResourceId;
         private LayoutInflater inflater;
 
         public ImportanceAdapter(Context context, int textViewResourceId,
                 int dropDownResourceId, Importance[] objects) {
             super(context, textViewResourceId, objects);
 
             inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
             this.textViewResourceId = textViewResourceId;
             this.dropDownResourceId = dropDownResourceId;
         }
 
         @Override
         public View getView(int position, View convertView, ViewGroup parent) {
             return getView(position, convertView, parent, textViewResourceId, true);
         }
 
         @Override
         public View getDropDownView(int position, View convertView, ViewGroup parent) {
             return getView(position, convertView, parent, dropDownResourceId, true);
         }
 
         public View getView(int position, View convertView, ViewGroup parent,
                 int resource, boolean setColors) {
             View view;
             TextView text;
             Resources r = getResources();
 
             if (convertView == null) {
                 view = inflater.inflate(resource, parent, false);
             } else {
                 view = convertView;
             }
 
             try {
                 text = (TextView) view;
             } catch (ClassCastException e) {
                 Log.e("ArrayAdapter", "You must supply a resource ID for a TextView");
                 throw new IllegalStateException(
                         "ArrayAdapter requires the resource ID to be a TextView", e);
             }
 
             text.setText(r.getString(getItem(position).getLabelResource()));
             if(setColors)
                 text.setBackgroundColor(r.getColor(getItem(position).getColorResource()));
 
             return view;
         }
     }
 }
