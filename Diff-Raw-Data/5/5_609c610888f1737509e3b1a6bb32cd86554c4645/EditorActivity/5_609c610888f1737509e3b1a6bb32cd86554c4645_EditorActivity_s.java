 /*
  * Copyright (C) 2012 The CyanogenMod Project
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *      http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 package com.cyanogenmod.explorer.activities;
 
 import android.app.ActionBar;
 import android.app.Activity;
 import android.app.AlertDialog;
 import android.content.DialogInterface;
 import android.content.DialogInterface.OnClickListener;
 import android.content.Intent;
 import android.content.pm.ActivityInfo;
 import android.os.AsyncTask;
 import android.os.Bundle;
 import android.text.Editable;
 import android.text.TextWatcher;
 import android.util.Log;
 import android.view.KeyEvent;
 import android.view.MenuItem;
 import android.view.View;
 import android.widget.EditText;
 import android.widget.ProgressBar;
 import android.widget.ScrollView;
 import android.widget.TextView;
 import android.widget.TextView.BufferType;
 import android.widget.Toast;
 
 import com.cyanogenmod.explorer.R;
 import com.cyanogenmod.explorer.commands.AsyncResultListener;
 import com.cyanogenmod.explorer.commands.WriteExecutable;
 import com.cyanogenmod.explorer.console.ConsoleBuilder;
 import com.cyanogenmod.explorer.model.FileSystemObject;
 import com.cyanogenmod.explorer.ui.widgets.ButtonItem;
 import com.cyanogenmod.explorer.util.CommandHelper;
 import com.cyanogenmod.explorer.util.DialogHelper;
 import com.cyanogenmod.explorer.util.ExceptionUtil;
 
 import java.io.ByteArrayInputStream;
 import java.io.File;
 import java.io.OutputStream;
 
 /**
  * An internal activity for view and edit files.
  */
 public class EditorActivity extends Activity implements TextWatcher {
 
     private static final String TAG = "EditorActivity"; //$NON-NLS-1$
 
     private static boolean DEBUG = false;
 
     /**
      * Internal interface to notify progress update
      */
     private interface OnProgressListener {
         void onProgress(int progress);
     }
 
     /**
      * An internal listener for read a file
      */
     @SuppressWarnings("hiding")
     private class AsyncReader implements AsyncResultListener {
 
         final Object mSync = new Object();
         StringBuilder mBuffer = new StringBuilder();
         Exception mCause;
         long mSize;
         FileSystemObject mFso;
         OnProgressListener mListener;
 
         /**
          * Constructor of <code>AsyncReader</code>. For enclosing access.
          */
         public AsyncReader() {
             super();
         }
 
         /**
          * {@inheritDoc}
          */
         @Override
         public void onAsyncStart() {
             this.mBuffer = new StringBuilder();
             this.mSize = 0;
         }
 
         /**
          * {@inheritDoc}
          */
         @Override
         public void onAsyncEnd(boolean cancelled) {/**NON BLOCK**/}
 
         /**
          * {@inheritDoc}
          */
         @Override
         public void onAsyncExitCode(int exitCode) {
             synchronized (this.mSync) {
                 this.mSync.notify();
             }
         }
 
         /**
          * {@inheritDoc}
          */
         @Override
         public void onPartialResult(Object result) {
             try {
                 byte[] partial = (byte[])result;
                 this.mBuffer.append(new String(partial));
                 this.mSize += this.mBuffer.length();
                 if (this.mListener != null && this.mFso != null) {
                     int progress = 0;
                     if (this.mFso.getSize() != 0) {
                         progress = (int)((this.mSize*100) / this.mFso.getSize());
                     }
                     this.mListener.onProgress(progress);
                 }
             } catch (Exception e) {
                 this.mCause = e;
             }
         }
 
         /**
          * {@inheritDoc}
          */
         @Override
         public void onException(Exception cause) {
             this.mCause = cause;
         }
     }
 
     /**
      * An internal listener for write a file
      */
     private class AsyncWriter implements AsyncResultListener {
 
         Exception mCause;
 
         /**
          * Constructor of <code>AsyncWriter</code>. For enclosing access.
          */
         public AsyncWriter() {
             super();
         }
 
         /**
          * {@inheritDoc}
          */
         @Override
         public void onAsyncStart() {/**NON BLOCK**/}
 
         /**
          * {@inheritDoc}
          */
         @Override
         public void onAsyncEnd(boolean cancelled) {/**NON BLOCK**/}
 
         /**
          * {@inheritDoc}
          */
         @Override
         public void onAsyncExitCode(int exitCode) {/**NON BLOCK**/}
 
         /**
          * {@inheritDoc}
          */
         @Override
         public void onPartialResult(Object result) {/**NON BLOCK**/}
 
         /**
          * {@inheritDoc}
          */
         @Override
         public void onException(Exception cause) {
             this.mCause = cause;
         }
     }
 
     private FileSystemObject mFso;
 
     private int mBufferSize;
     private int mMaxFileSize;
 
     /**
      * @hide
      */
     boolean mDirty;
     /**
      * @hide
      */
     boolean mReadOnly;
 
     /**
      * @hide
      */
     TextView mTitle;
     /**
      * @hide
      */
     ScrollView mScroll;
     /**
      * @hide
      */
     EditText mEditor;
     /**
      * @hide
      */
     View mProgress;
     /**
      * @hide
      */
     ProgressBar mProgressBar;
     /**
      * @hide
      */
     ButtonItem mSave;
 
     /**
      * Intent extra parameter for the path of the file to open.
      */
     public static final String EXTRA_OPEN_FILE = "extra_open_file";  //$NON-NLS-1$
 
     /**
      * {@inheritDoc}
      */
     @Override
     protected void onCreate(Bundle state) {
         if (DEBUG) {
             Log.d(TAG, "EditorActivity.onCreate"); //$NON-NLS-1$
         }
 
         //Request features
         setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
 
         //Set the main layout of the activity
         setContentView(R.layout.editor);
 
         // Get the limit vars
         this.mBufferSize =
                 getApplicationContext().getResources().getInteger(R.integer.buffer_size);
         this.mMaxFileSize =
                 getApplicationContext().getResources().getInteger(R.integer.editor_max_file_size);
 
         //Initialize
         initTitleActionBar();
         initLayout();
         initializeConsole();
         readFile();
 
         //Save state
         super.onCreate(state);
     }
 
     /**
      * Method that initializes the titlebar of the activity.
      */
     private void initTitleActionBar() {
         //Configure the action bar options
         getActionBar().setBackgroundDrawable(
                 getResources().getDrawable(R.drawable.bg_holo_titlebar));
         getActionBar().setDisplayOptions(
                 ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME);
         getActionBar().setDisplayHomeAsUpEnabled(true);
         View customTitle = getLayoutInflater().inflate(R.layout.simple_customtitle, null, false);
         this.mTitle = (TextView)customTitle.findViewById(R.id.customtitle_title);
         this.mTitle.setText(R.string.editor);
         this.mTitle.setContentDescription(getString(R.string.editor));
         this.mSave = (ButtonItem)customTitle.findViewById(R.id.ab_button1);
         this.mSave.setImageResource(R.drawable.ic_holo_light_save);
         this.mSave.setContentDescription(getString(R.string.actionbar_button_save_cd));
         this.mSave.setVisibility(View.INVISIBLE);
 
         getActionBar().setCustomView(customTitle);
     }
 
     /**
      * Method that initializes the layout and components of the activity.
      */
     private void initLayout() {
         this.mEditor = (EditText)findViewById(R.id.editor);
         this.mEditor.setText(null);
         this.mEditor.addTextChangedListener(this);
 
         this.mScroll = (ScrollView)findViewById(R.id.editor_scroller);
         this.mScroll.setEnabled(false);
 
         this.mProgress = findViewById(R.id.editor_progress);
         this.mProgressBar = (ProgressBar)findViewById(R.id.editor_progress_bar);
     }
 
     /**
      * {@inheritDoc}
      */
     @Override
     public boolean onKeyUp(int keyCode, KeyEvent event) {
         if (keyCode == KeyEvent.KEYCODE_BACK) {
             checkDirtyState();
             return false;
         }
         return super.onKeyUp(keyCode, event);
     }
 
     /**
      * {@inheritDoc}
      */
     @Override
     public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
           case android.R.id.home:
               if ((getActionBar().getDisplayOptions() & ActionBar.DISPLAY_HOME_AS_UP)
                       == ActionBar.DISPLAY_HOME_AS_UP) {
                   checkDirtyState();
               }
               return true;
           default:
              return super.onOptionsItemSelected(item);
        }
     }
 
     /**
      * Method invoked when an action item is clicked.
      *
      * @param view The button pushed
      */
     public void onActionBarItemClick(View view) {
         switch (view.getId()) {
             case R.id.ab_button1:
                 // Save the file
                 writeFile();
                 break;
 
             default:
                 break;
         }
     }
 
     /**
      * Method that initializes a console
      */
     private boolean initializeConsole() {
         try {
             // Is there a console allocate
             if (!ConsoleBuilder.isAlloc()) {
                 // Create a console
                 ConsoleBuilder.getConsole(this, true);
             }
             // There is a console allocated. Use it.
             return true;
         } catch (Throwable _throw) {
             // Capture the exception
             ExceptionUtil.translateException(this, _throw, false, true);
         }
         return false;
     }
 
     /**
      * Method that reads the requested file
      */
     private void readFile() {
         // For now editor is not dirty and editable.
         setDirty(false);
 
         // Check for a valid action
         String action = getIntent().getAction();
         if (action == null ||
                 (action.compareTo(Intent.ACTION_VIEW) != 0) &&
                 (action.compareTo(Intent.ACTION_EDIT) != 0)) {
             DialogHelper.showToast(
                     this, R.string.editor_invalid_file_msg, Toast.LENGTH_SHORT);
             return;
         }
        this.mReadOnly  = (action.compareTo(Intent.ACTION_EDIT) == 0);
 
         // Read the intent and check that is has a valid request
         String path = getIntent().getData().getPath();
         if (path == null || path.length() == 0) {
             DialogHelper.showToast(
                     this, R.string.editor_invalid_file_msg, Toast.LENGTH_SHORT);
             return;
         }
 
         // Set the title of the dialog
         File f = new File(path);
         this.mTitle.setText(f.getName());
 
         // Check that we have access to the file (the real file, not the symlink)
         try {
             this.mFso = CommandHelper.getFileInfo(this, path, true, null);
             if (this.mFso == null) {
                 DialogHelper.showToast(
                         this, R.string.editor_file_not_found_msg, Toast.LENGTH_SHORT);
                 return;
             }
         } catch (Exception e) {
             Log.e(TAG, "Failed to get file reference", e); //$NON-NLS-1$
             DialogHelper.showToast(
                     this, R.string.editor_file_not_found_msg, Toast.LENGTH_SHORT);
             return;
         }
 
         // Check that we can handle the length of the file (by device)
         if (this.mMaxFileSize < this.mFso.getSize()) {
             DialogHelper.showToast(
                     this, R.string.editor_file_exceed_size_msg, Toast.LENGTH_SHORT);
             return;
         }
 
         // Do the load of the
         AsyncTask<FileSystemObject, Integer, Boolean> mOpenTask =
                             new AsyncTask<FileSystemObject, Integer, Boolean>() {
 
             private Exception mCause;
             private AsyncReader mReader;
 
             @Override
             protected void onPreExecute() {
                 // Show the progress
                 doProgress(true, 0);
             }
 
             @Override
             protected Boolean doInBackground(FileSystemObject... params) {
                 // Only one argument (the file to open)
                 FileSystemObject fso = params[0];
                 this.mCause = null;
 
                 // Read the file in an async listener
                 try {
                     // Configure the reader
                     this.mReader = new AsyncReader();
                     this.mReader.mFso = fso;
                     this.mReader.mListener = new OnProgressListener() {
                         @Override
                         @SuppressWarnings("synthetic-access")
                         public void onProgress(int progress) {
                             publishProgress(Integer.valueOf(progress));
                         }
                     };
 
                     // Execute the command (read the file)
                     CommandHelper.read(
                             EditorActivity.this, fso.getFullPath(), this.mReader, null);
 
                     // Wait for
                     synchronized (this.mReader.mSync) {
                         this.mReader.mSync.wait();
                     }
 
                     // 100%
                     doProgress(true, 100);
 
                     // Check if the read was successfully
                     if (this.mReader.mCause != null) {
                         this.mCause = this.mReader.mCause;
                         return Boolean.FALSE;
                     }
 
                 } catch (Exception e) {
                     this.mCause = e;
                     return Boolean.FALSE;
                 }
 
                 return Boolean.TRUE;
             }
 
             @Override
             protected void onProgressUpdate(Integer... values) {
                 // Do progress
                 doProgress(true, values[0].intValue());
             }
 
             @Override
             protected void onPostExecute(Boolean result) {
                 // Hide the progress
                 doProgress(false, 0);
 
                 // Is error?
                 if (!result.booleanValue()) {
                     if (this.mCause != null) {
                         ExceptionUtil.translateException(EditorActivity.this, this.mCause);
                     }
                 } else {
                     // Now we have the buffer, set the text of the editor
                     EditorActivity.this.mEditor.setText(
                             this.mReader.mBuffer, BufferType.EDITABLE);
                     this.mReader.mBuffer = null; //Cleanup
                     setDirty(false);
                    EditorActivity.this.mScroll.setEnabled(EditorActivity.this.mReadOnly);
                 }
             }
 
             @Override
             protected void onCancelled() {
                 // Hide the progress
                 doProgress(false, 0);
             }
 
             /**
              * Method that update the progress status
              *
              * @param visible If the progress bar need to be hidden
              * @param progress The progress
              */
             private void doProgress(boolean visible, int progress) {
                 // Show the progress bar
                 EditorActivity.this.mProgressBar.setProgress(progress);
                 EditorActivity.this.mProgress.setVisibility(
                             visible ? View.VISIBLE : View.GONE);
             }
         };
         mOpenTask.execute(this.mFso);
     }
 
     /**
      * Method that reads the requested file.
      */
     private void writeFile() {
         try {
             // Configure the writer
             AsyncWriter writer = new AsyncWriter();
 
             // Create the writable command
             WriteExecutable cmd =
                     CommandHelper.write(this, this.mFso.getFullPath(), writer, null);
 
             // Obtain access to the buffer (IMP! don't close the buffer here, it's manage
             // by the command)
             OutputStream os = cmd.createOutputStream();
             try {
                 // Retrieve the text from the editor
                 String text = this.mEditor.getText().toString();
                 ByteArrayInputStream bais = new ByteArrayInputStream(text.getBytes());
                 text = null;
                 try {
                     // Buffered write
                     byte[] data = new byte[this.mBufferSize];
                     int read = 0;
                     while ((read = bais.read(data, 0, this.mBufferSize)) != -1) {
                         os.write(data, 0, read);
                     }
                 } finally {
                     try {
                         bais.close();
                     } catch (Exception e) {/**NON BLOCK**/}
                 }
 
             } finally {
                 // Ok. Data is written or ensure buffer close
                 cmd.end();
             }
 
             // Sleep a bit
             Thread.sleep(150L);
 
             // Is error?
             if (writer.mCause != null) {
                 // Something was wrong. The file probably is corrupted
                 DialogHelper.showToast(
                         this, R.string.msgs_operation_failure, Toast.LENGTH_SHORT);
             } else {
                 // Success. The file was saved
                 DialogHelper.showToast(
                         this, R.string.editor_successfully_saved, Toast.LENGTH_SHORT);
                 setDirty(false);
             }
 
         } catch (Exception e) {
             // Something was wrong, but the file was NOT written
             DialogHelper.showToast(
                     this, R.string.msgs_operation_failure, Toast.LENGTH_SHORT);
             return;
         }
     }
 
     /**
      * {@inheritDoc}
      */
     @Override
     public void beforeTextChanged(
             CharSequence s, int start, int count, int after) {/**NON BLOCK**/}
 
     /**
      * {@inheritDoc}
      */
     @Override
     public void onTextChanged(CharSequence s, int start, int before, int count) {/**NON BLOCK**/}
 
     /**
      * {@inheritDoc}
      */
     @Override
     public void afterTextChanged(Editable s) {
         setDirty(true);
     }
 
     /**
      * Method that sets if the editor is dirty (has changed)
      *
      * @param dirty If the editor is dirty
      * @hide
      */
     void setDirty(boolean dirty) {
         this.mDirty = dirty;
         this.mSave.setVisibility(dirty ? View.VISIBLE : View.GONE);
     }
 
     /**
      * Check the dirty state of the editor, and ask the user to save the changes
      * prior to exit.
      */
     public void checkDirtyState() {
         if (this.mDirty) {
             AlertDialog dlg = DialogHelper.createYesNoDialog(
                     this, R.string.editor_dirty_ask, new OnClickListener() {
                         @Override
                         public void onClick(DialogInterface dialog, int which) {
                             if (which == DialogInterface.BUTTON_POSITIVE) {
                                 dialog.dismiss();
                                 setResult(Activity.RESULT_OK);
                                 finish();
                             }
                         }
                     });
             dlg.show();
             return;
         }
         setResult(Activity.RESULT_OK);
         finish();
     }
 
 }
