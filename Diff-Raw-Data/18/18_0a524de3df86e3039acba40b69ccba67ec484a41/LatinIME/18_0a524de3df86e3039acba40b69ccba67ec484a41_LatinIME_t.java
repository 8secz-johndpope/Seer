 /*
  * Copyright (C) 2008 The Android Open Source Project
  *
  * Licensed under the Apache License, Version 2.0 (the "License"); you may not
  * use this file except in compliance with the License. You may obtain a copy of
  * the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
  * License for the specific language governing permissions and limitations under
  * the License.
  */
 
 package com.android.inputmethod.latin;
 
 import static com.android.inputmethod.latin.Constants.ImeOption.FORCE_ASCII;
 import static com.android.inputmethod.latin.Constants.ImeOption.NO_MICROPHONE;
 import static com.android.inputmethod.latin.Constants.ImeOption.NO_MICROPHONE_COMPAT;
 
 import android.app.Activity;
 import android.app.AlertDialog;
 import android.content.BroadcastReceiver;
 import android.content.Context;
 import android.content.DialogInterface;
 import android.content.Intent;
 import android.content.IntentFilter;
 import android.content.SharedPreferences;
 import android.content.pm.ApplicationInfo;
 import android.content.res.Configuration;
 import android.content.res.Resources;
 import android.graphics.Rect;
 import android.inputmethodservice.InputMethodService;
 import android.media.AudioManager;
 import android.net.ConnectivityManager;
 import android.os.Debug;
 import android.os.IBinder;
 import android.os.Message;
 import android.os.SystemClock;
 import android.preference.PreferenceManager;
 import android.text.InputType;
 import android.text.TextUtils;
 import android.util.Log;
 import android.util.PrintWriterPrinter;
 import android.util.Printer;
 import android.view.KeyCharacterMap;
 import android.view.KeyEvent;
 import android.view.View;
 import android.view.ViewGroup.LayoutParams;
 import android.view.Window;
 import android.view.WindowManager;
 import android.view.inputmethod.CompletionInfo;
 import android.view.inputmethod.CorrectionInfo;
 import android.view.inputmethod.EditorInfo;
 import android.view.inputmethod.InputMethodSubtype;
 
 import com.android.inputmethod.accessibility.AccessibilityUtils;
 import com.android.inputmethod.accessibility.AccessibleKeyboardViewProxy;
 import com.android.inputmethod.compat.CompatUtils;
 import com.android.inputmethod.compat.InputMethodManagerCompatWrapper;
 import com.android.inputmethod.compat.InputMethodServiceCompatUtils;
 import com.android.inputmethod.compat.SuggestionSpanUtils;
 import com.android.inputmethod.keyboard.KeyDetector;
 import com.android.inputmethod.keyboard.Keyboard;
 import com.android.inputmethod.keyboard.KeyboardActionListener;
 import com.android.inputmethod.keyboard.KeyboardId;
 import com.android.inputmethod.keyboard.KeyboardSwitcher;
 import com.android.inputmethod.keyboard.KeyboardView;
 import com.android.inputmethod.keyboard.MainKeyboardView;
 import com.android.inputmethod.latin.LocaleUtils.RunInLocale;
 import com.android.inputmethod.latin.define.ProductionFlag;
 import com.android.inputmethod.latin.suggestions.SuggestionStripView;
 import com.android.inputmethod.research.ResearchLogger;
 
 import java.io.FileDescriptor;
 import java.io.PrintWriter;
 import java.util.ArrayList;
 import java.util.Locale;
 
 /**
  * Input method implementation for Qwerty'ish keyboard.
  */
 public class LatinIME extends InputMethodService implements KeyboardActionListener,
         SuggestionStripView.Listener, TargetApplicationGetter.OnTargetApplicationKnownListener,
         Suggest.SuggestInitializationListener {
     private static final String TAG = LatinIME.class.getSimpleName();
     private static final boolean TRACE = false;
     private static boolean DEBUG;
 
     private static final int EXTENDED_TOUCHABLE_REGION_HEIGHT = 100;
 
     // How many continuous deletes at which to start deleting at a higher speed.
     private static final int DELETE_ACCELERATE_AT = 20;
     // Key events coming any faster than this are long-presses.
     private static final int QUICK_PRESS = 200;
 
     private static final int PENDING_IMS_CALLBACK_DURATION = 800;
 
     /**
      * The name of the scheme used by the Package Manager to warn of a new package installation,
      * replacement or removal.
      */
     private static final String SCHEME_PACKAGE = "package";
 
     private static final int SPACE_STATE_NONE = 0;
     // Double space: the state where the user pressed space twice quickly, which LatinIME
     // resolved as period-space. Undoing this converts the period to a space.
     private static final int SPACE_STATE_DOUBLE = 1;
     // Swap punctuation: the state where a weak space and a punctuation from the suggestion strip
     // have just been swapped. Undoing this swaps them back; the space is still considered weak.
     private static final int SPACE_STATE_SWAP_PUNCTUATION = 2;
     // Weak space: a space that should be swapped only by suggestion strip punctuation. Weak
     // spaces happen when the user presses space, accepting the current suggestion (whether
     // it's an auto-correction or not).
     private static final int SPACE_STATE_WEAK = 3;
     // Phantom space: a not-yet-inserted space that should get inserted on the next input,
     // character provided it's not a separator. If it's a separator, the phantom space is dropped.
     // Phantom spaces happen when a user chooses a word from the suggestion strip.
     private static final int SPACE_STATE_PHANTOM = 4;
 
     // Current space state of the input method. This can be any of the above constants.
     private int mSpaceState;
 
     private SettingsValues mCurrentSettings;
 
     private View mExtractArea;
     private View mKeyPreviewBackingView;
     private View mSuggestionsContainer;
     private SuggestionStripView mSuggestionStripView;
     /* package for tests */ Suggest mSuggest;
     private CompletionInfo[] mApplicationSpecifiedCompletions;
     private ApplicationInfo mTargetApplicationInfo;
 
     private InputMethodManagerCompatWrapper mImm;
     private Resources mResources;
     private SharedPreferences mPrefs;
     /* package for tests */ final KeyboardSwitcher mKeyboardSwitcher;
     private final SubtypeSwitcher mSubtypeSwitcher;
     private boolean mShouldSwitchToLastSubtype = true;
 
     private boolean mIsMainDictionaryAvailable;
     private UserBinaryDictionary mUserDictionary;
     private UserHistoryDictionary mUserHistoryDictionary;
     private boolean mIsUserDictionaryAvailable;
 
     private LastComposedWord mLastComposedWord = LastComposedWord.NOT_A_COMPOSED_WORD;
     private WordComposer mWordComposer = new WordComposer();
     private RichInputConnection mConnection = new RichInputConnection(this);
 
     // Keep track of the last selection range to decide if we need to show word alternatives
     private static final int NOT_A_CURSOR_POSITION = -1;
     private int mLastSelectionStart = NOT_A_CURSOR_POSITION;
     private int mLastSelectionEnd = NOT_A_CURSOR_POSITION;
 
     // Whether we are expecting an onUpdateSelection event to fire. If it does when we don't
     // "expect" it, it means the user actually moved the cursor.
     private boolean mExpectingUpdateSelection;
     private int mDeleteCount;
     private long mLastKeyTime;
 
     private AudioAndHapticFeedbackManager mFeedbackManager;
 
     // Member variables for remembering the current device orientation.
     private int mDisplayOrientation;
 
     // Object for reacting to adding/removing a dictionary pack.
     private BroadcastReceiver mDictionaryPackInstallReceiver =
             new DictionaryPackInstallBroadcastReceiver(this);
 
     // Keeps track of most recently inserted text (multi-character key) for reverting
     private CharSequence mEnteredText;
 
     private boolean mIsAutoCorrectionIndicatorOn;
 
     private AlertDialog mOptionsDialog;
 
     private final boolean mIsHardwareAcceleratedDrawingEnabled;
 
     public final UIHandler mHandler = new UIHandler(this);
 
     public static class UIHandler extends StaticInnerHandlerWrapper<LatinIME> {
         private static final int MSG_UPDATE_SHIFT_STATE = 1;
         private static final int MSG_PENDING_IMS_CALLBACK = 6;
         private static final int MSG_UPDATE_SUGGESTION_STRIP = 7;
 
         private int mDelayUpdateSuggestions;
         private int mDelayUpdateShiftState;
         private long mDoubleSpacesTurnIntoPeriodTimeout;
         private long mDoubleSpaceTimerStart;
 
         public UIHandler(LatinIME outerInstance) {
             super(outerInstance);
         }
 
         public void onCreate() {
             final Resources res = getOuterInstance().getResources();
             mDelayUpdateSuggestions =
                     res.getInteger(R.integer.config_delay_update_suggestions);
             mDelayUpdateShiftState =
                     res.getInteger(R.integer.config_delay_update_shift_state);
             mDoubleSpacesTurnIntoPeriodTimeout = res.getInteger(
                     R.integer.config_double_spaces_turn_into_period_timeout);
         }
 
         @Override
         public void handleMessage(Message msg) {
             final LatinIME latinIme = getOuterInstance();
             final KeyboardSwitcher switcher = latinIme.mKeyboardSwitcher;
             switch (msg.what) {
             case MSG_UPDATE_SUGGESTION_STRIP:
                 latinIme.updateSuggestionStrip();
                 break;
             case MSG_UPDATE_SHIFT_STATE:
                 switcher.updateShiftState();
                 break;
             }
         }
 
         public void postUpdateSuggestionStrip() {
             sendMessageDelayed(obtainMessage(MSG_UPDATE_SUGGESTION_STRIP), mDelayUpdateSuggestions);
         }
 
         public void cancelUpdateSuggestionStrip() {
             removeMessages(MSG_UPDATE_SUGGESTION_STRIP);
         }
 
         public boolean hasPendingUpdateSuggestions() {
             return hasMessages(MSG_UPDATE_SUGGESTION_STRIP);
         }
 
         public void postUpdateShiftState() {
             removeMessages(MSG_UPDATE_SHIFT_STATE);
             sendMessageDelayed(obtainMessage(MSG_UPDATE_SHIFT_STATE), mDelayUpdateShiftState);
         }
 
         public void cancelUpdateShiftState() {
             removeMessages(MSG_UPDATE_SHIFT_STATE);
         }
 
         public void startDoubleSpacesTimer() {
             mDoubleSpaceTimerStart = SystemClock.uptimeMillis();
         }
 
         public void cancelDoubleSpacesTimer() {
             mDoubleSpaceTimerStart = 0;
         }
 
         public boolean isAcceptingDoubleSpaces() {
             return SystemClock.uptimeMillis() - mDoubleSpaceTimerStart
                     < mDoubleSpacesTurnIntoPeriodTimeout;
         }
 
         // Working variables for the following methods.
         private boolean mIsOrientationChanging;
         private boolean mPendingSuccessiveImsCallback;
         private boolean mHasPendingStartInput;
         private boolean mHasPendingFinishInputView;
         private boolean mHasPendingFinishInput;
         private EditorInfo mAppliedEditorInfo;
 
         public void startOrientationChanging() {
             removeMessages(MSG_PENDING_IMS_CALLBACK);
             resetPendingImsCallback();
             mIsOrientationChanging = true;
             final LatinIME latinIme = getOuterInstance();
             if (latinIme.isInputViewShown()) {
                 latinIme.mKeyboardSwitcher.saveKeyboardState();
             }
         }
 
         private void resetPendingImsCallback() {
             mHasPendingFinishInputView = false;
             mHasPendingFinishInput = false;
             mHasPendingStartInput = false;
         }
 
         private void executePendingImsCallback(LatinIME latinIme, EditorInfo editorInfo,
                 boolean restarting) {
             if (mHasPendingFinishInputView)
                 latinIme.onFinishInputViewInternal(mHasPendingFinishInput);
             if (mHasPendingFinishInput)
                 latinIme.onFinishInputInternal();
             if (mHasPendingStartInput)
                 latinIme.onStartInputInternal(editorInfo, restarting);
             resetPendingImsCallback();
         }
 
         public void onStartInput(EditorInfo editorInfo, boolean restarting) {
             if (hasMessages(MSG_PENDING_IMS_CALLBACK)) {
                 // Typically this is the second onStartInput after orientation changed.
                 mHasPendingStartInput = true;
             } else {
                 if (mIsOrientationChanging && restarting) {
                     // This is the first onStartInput after orientation changed.
                     mIsOrientationChanging = false;
                     mPendingSuccessiveImsCallback = true;
                 }
                 final LatinIME latinIme = getOuterInstance();
                 executePendingImsCallback(latinIme, editorInfo, restarting);
                 latinIme.onStartInputInternal(editorInfo, restarting);
             }
         }
 
         public void onStartInputView(EditorInfo editorInfo, boolean restarting) {
             if (hasMessages(MSG_PENDING_IMS_CALLBACK)
                     && KeyboardId.equivalentEditorInfoForKeyboard(editorInfo, mAppliedEditorInfo)) {
                 // Typically this is the second onStartInputView after orientation changed.
                 resetPendingImsCallback();
             } else {
                 if (mPendingSuccessiveImsCallback) {
                     // This is the first onStartInputView after orientation changed.
                     mPendingSuccessiveImsCallback = false;
                     resetPendingImsCallback();
                     sendMessageDelayed(obtainMessage(MSG_PENDING_IMS_CALLBACK),
                             PENDING_IMS_CALLBACK_DURATION);
                 }
                 final LatinIME latinIme = getOuterInstance();
                 executePendingImsCallback(latinIme, editorInfo, restarting);
                 latinIme.onStartInputViewInternal(editorInfo, restarting);
                 mAppliedEditorInfo = editorInfo;
             }
         }
 
         public void onFinishInputView(boolean finishingInput) {
             if (hasMessages(MSG_PENDING_IMS_CALLBACK)) {
                 // Typically this is the first onFinishInputView after orientation changed.
                 mHasPendingFinishInputView = true;
             } else {
                 final LatinIME latinIme = getOuterInstance();
                 latinIme.onFinishInputViewInternal(finishingInput);
                 mAppliedEditorInfo = null;
             }
         }
 
         public void onFinishInput() {
             if (hasMessages(MSG_PENDING_IMS_CALLBACK)) {
                 // Typically this is the first onFinishInput after orientation changed.
                 mHasPendingFinishInput = true;
             } else {
                 final LatinIME latinIme = getOuterInstance();
                 executePendingImsCallback(latinIme, null, false);
                 latinIme.onFinishInputInternal();
             }
         }
     }
 
     public LatinIME() {
         super();
         mSubtypeSwitcher = SubtypeSwitcher.getInstance();
         mKeyboardSwitcher = KeyboardSwitcher.getInstance();
         mIsHardwareAcceleratedDrawingEnabled =
                 InputMethodServiceCompatUtils.enableHardwareAcceleration(this);
         Log.i(TAG, "Hardware accelerated drawing: " + mIsHardwareAcceleratedDrawingEnabled);
     }
 
     @Override
     public void onCreate() {
         final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
         mPrefs = prefs;
         LatinImeLogger.init(this, prefs);
         if (ProductionFlag.IS_EXPERIMENTAL) {
             ResearchLogger.getInstance().init(this, prefs, mKeyboardSwitcher);
         }
         InputMethodManagerCompatWrapper.init(this);
         SubtypeSwitcher.init(this);
         KeyboardSwitcher.init(this, prefs);
         AccessibilityUtils.init(this);
 
         super.onCreate();
 
         mImm = InputMethodManagerCompatWrapper.getInstance();
         mHandler.onCreate();
         DEBUG = LatinImeLogger.sDBG;
 
         final Resources res = getResources();
         mResources = res;
 
         loadSettings();
 
         ImfUtils.setAdditionalInputMethodSubtypes(this, mCurrentSettings.getAdditionalSubtypes());
 
         Utils.GCUtils.getInstance().reset();
         boolean tryGC = true;
         // Shouldn't this be removed? I think that from Honeycomb on, the GC is now actually working
         // as expected and this code is useless.
         for (int i = 0; i < Utils.GCUtils.GC_TRY_LOOP_MAX && tryGC; ++i) {
             try {
                 initSuggest();
                 tryGC = false;
             } catch (OutOfMemoryError e) {
                 tryGC = Utils.GCUtils.getInstance().tryGCOrWait("InitSuggest", e);
             }
         }
 
         mDisplayOrientation = res.getConfiguration().orientation;
 
         // Register to receive ringer mode change and network state change.
         // Also receive installation and removal of a dictionary pack.
         final IntentFilter filter = new IntentFilter();
         filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
         filter.addAction(AudioManager.RINGER_MODE_CHANGED_ACTION);
         registerReceiver(mReceiver, filter);
 
         final IntentFilter packageFilter = new IntentFilter();
         packageFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
         packageFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
         packageFilter.addDataScheme(SCHEME_PACKAGE);
         registerReceiver(mDictionaryPackInstallReceiver, packageFilter);
 
         final IntentFilter newDictFilter = new IntentFilter();
         newDictFilter.addAction(
                 DictionaryPackInstallBroadcastReceiver.NEW_DICTIONARY_INTENT_ACTION);
         registerReceiver(mDictionaryPackInstallReceiver, newDictFilter);
     }
 
     // Has to be package-visible for unit tests
     /* package */ void loadSettings() {
         // Note that the calling sequence of onCreate() and onCurrentInputMethodSubtypeChanged()
         // is not guaranteed. It may even be called at the same time on a different thread.
         if (null == mPrefs) mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
         final InputAttributes inputAttributes =
                 new InputAttributes(getCurrentInputEditorInfo(), isFullscreenMode());
         final RunInLocale<SettingsValues> job = new RunInLocale<SettingsValues>() {
             @Override
             protected SettingsValues job(Resources res) {
                 return new SettingsValues(mPrefs, inputAttributes, LatinIME.this);
             }
         };
         mCurrentSettings = job.runInLocale(mResources, mSubtypeSwitcher.getCurrentSubtypeLocale());
         mFeedbackManager = new AudioAndHapticFeedbackManager(this, mCurrentSettings);
         resetContactsDictionary(null == mSuggest ? null : mSuggest.getContactsDictionary());
     }
 
     @Override
     public void onUpdateMainDictionaryAvailability(boolean isMainDictionaryAvailable) {
         mIsMainDictionaryAvailable = isMainDictionaryAvailable;
         updateKeyboardViewGestureHandlingModeByMainDictionaryAvailability();
     }
 
     private void initSuggest() {
         final Locale subtypeLocale = mSubtypeSwitcher.getCurrentSubtypeLocale();
         final String localeStr = subtypeLocale.toString();
 
         final ContactsBinaryDictionary oldContactsDictionary;
         if (mSuggest != null) {
             oldContactsDictionary = mSuggest.getContactsDictionary();
             mSuggest.close();
         } else {
             oldContactsDictionary = null;
         }
         mSuggest = new Suggest(this /* Context */, subtypeLocale,
                 this /* SuggestInitializationListener */);
         if (mCurrentSettings.mCorrectionEnabled) {
             mSuggest.setAutoCorrectionThreshold(mCurrentSettings.mAutoCorrectionThreshold);
         }
 
         mIsMainDictionaryAvailable = DictionaryFactory.isDictionaryAvailable(this, subtypeLocale);
         if (ProductionFlag.IS_EXPERIMENTAL) {
             ResearchLogger.getInstance().initSuggest(mSuggest);
         }
 
         mUserDictionary = new UserBinaryDictionary(this, localeStr);
         mIsUserDictionaryAvailable = mUserDictionary.isEnabled();
         mSuggest.setUserDictionary(mUserDictionary);
 
         resetContactsDictionary(oldContactsDictionary);
 
         // Note that the calling sequence of onCreate() and onCurrentInputMethodSubtypeChanged()
         // is not guaranteed. It may even be called at the same time on a different thread.
         if (null == mPrefs) mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
         mUserHistoryDictionary = UserHistoryDictionary.getInstance(this, localeStr, mPrefs);
         mSuggest.setUserHistoryDictionary(mUserHistoryDictionary);
     }
 
     /**
      * Resets the contacts dictionary in mSuggest according to the user settings.
      *
      * This method takes an optional contacts dictionary to use when the locale hasn't changed
      * since the contacts dictionary can be opened or closed as necessary depending on the settings.
      *
      * @param oldContactsDictionary an optional dictionary to use, or null
      */
     private void resetContactsDictionary(final ContactsBinaryDictionary oldContactsDictionary) {
         final boolean shouldSetDictionary = (null != mSuggest && mCurrentSettings.mUseContactsDict);
 
         final ContactsBinaryDictionary dictionaryToUse;
         if (!shouldSetDictionary) {
             // Make sure the dictionary is closed. If it is already closed, this is a no-op,
             // so it's safe to call it anyways.
             if (null != oldContactsDictionary) oldContactsDictionary.close();
             dictionaryToUse = null;
         } else {
             final Locale locale = mSubtypeSwitcher.getCurrentSubtypeLocale();
             if (null != oldContactsDictionary) {
                 if (!oldContactsDictionary.mLocale.equals(locale)) {
                     // If the locale has changed then recreate the contacts dictionary. This
                     // allows locale dependent rules for handling bigram name predictions.
                     oldContactsDictionary.close();
                     dictionaryToUse = new ContactsBinaryDictionary(this, locale);
                 } else {
                     // Make sure the old contacts dictionary is opened. If it is already open,
                     // this is a no-op, so it's safe to call it anyways.
                     oldContactsDictionary.reopen(this);
                     dictionaryToUse = oldContactsDictionary;
                 }
             } else {
                 dictionaryToUse = new ContactsBinaryDictionary(this, locale);
             }
         }
 
         if (null != mSuggest) {
             mSuggest.setContactsDictionary(dictionaryToUse);
         }
     }
 
     /* package private */ void resetSuggestMainDict() {
         final Locale subtypeLocale = mSubtypeSwitcher.getCurrentSubtypeLocale();
         mSuggest.resetMainDict(this, subtypeLocale);
         mIsMainDictionaryAvailable = DictionaryFactory.isDictionaryAvailable(this, subtypeLocale);
     }
 
     @Override
     public void onDestroy() {
         if (mSuggest != null) {
             mSuggest.close();
             mSuggest = null;
         }
         unregisterReceiver(mReceiver);
         unregisterReceiver(mDictionaryPackInstallReceiver);
         LatinImeLogger.commit();
         LatinImeLogger.onDestroy();
         super.onDestroy();
     }
 
     @Override
     public void onConfigurationChanged(Configuration conf) {
         mSubtypeSwitcher.onConfigurationChanged(conf);
         // If orientation changed while predicting, commit the change
         if (mDisplayOrientation != conf.orientation) {
             mDisplayOrientation = conf.orientation;
             mHandler.startOrientationChanging();
             mConnection.beginBatchEdit();
             commitTyped(LastComposedWord.NOT_A_SEPARATOR);
             mConnection.finishComposingText();
             mConnection.endBatchEdit();
            if (isShowingOptionDialog()) {
                 mOptionsDialog.dismiss();
            }
         }
         super.onConfigurationChanged(conf);
     }
 
     @Override
     public View onCreateInputView() {
         return mKeyboardSwitcher.onCreateInputView(mIsHardwareAcceleratedDrawingEnabled);
     }
 
     @Override
     public void setInputView(View view) {
         super.setInputView(view);
         mExtractArea = getWindow().getWindow().getDecorView()
                 .findViewById(android.R.id.extractArea);
         mKeyPreviewBackingView = view.findViewById(R.id.key_preview_backing);
         mSuggestionsContainer = view.findViewById(R.id.suggestions_container);
         mSuggestionStripView = (SuggestionStripView)view.findViewById(R.id.suggestion_strip_view);
         if (mSuggestionStripView != null)
             mSuggestionStripView.setListener(this, view);
         if (LatinImeLogger.sVISUALDEBUG) {
             mKeyPreviewBackingView.setBackgroundColor(0x10FF0000);
         }
     }
 
     @Override
     public void setCandidatesView(View view) {
         // To ensure that CandidatesView will never be set.
         return;
     }
 
     @Override
     public void onStartInput(EditorInfo editorInfo, boolean restarting) {
         mHandler.onStartInput(editorInfo, restarting);
     }
 
     @Override
     public void onStartInputView(EditorInfo editorInfo, boolean restarting) {
         mHandler.onStartInputView(editorInfo, restarting);
     }
 
     @Override
     public void onFinishInputView(boolean finishingInput) {
         mHandler.onFinishInputView(finishingInput);
     }
 
     @Override
     public void onFinishInput() {
         mHandler.onFinishInput();
     }
 
     @Override
     public void onCurrentInputMethodSubtypeChanged(InputMethodSubtype subtype) {
         // Note that the calling sequence of onCreate() and onCurrentInputMethodSubtypeChanged()
         // is not guaranteed. It may even be called at the same time on a different thread.
         mSubtypeSwitcher.updateSubtype(subtype);
     }
 
     private void onStartInputInternal(EditorInfo editorInfo, boolean restarting) {
         super.onStartInput(editorInfo, restarting);
     }
 
     @SuppressWarnings("deprecation")
     private void onStartInputViewInternal(EditorInfo editorInfo, boolean restarting) {
         super.onStartInputView(editorInfo, restarting);
         final KeyboardSwitcher switcher = mKeyboardSwitcher;
         MainKeyboardView inputView = switcher.getKeyboardView();
 
         if (editorInfo == null) {
             Log.e(TAG, "Null EditorInfo in onStartInputView()");
             if (LatinImeLogger.sDBG) {
                 throw new NullPointerException("Null EditorInfo in onStartInputView()");
             }
             return;
         }
         if (DEBUG) {
             Log.d(TAG, "onStartInputView: editorInfo:"
                     + String.format("inputType=0x%08x imeOptions=0x%08x",
                             editorInfo.inputType, editorInfo.imeOptions));
             Log.d(TAG, "All caps = "
                     + ((editorInfo.inputType & InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS) != 0)
                     + ", sentence caps = "
                     + ((editorInfo.inputType & InputType.TYPE_TEXT_FLAG_CAP_SENTENCES) != 0)
                     + ", word caps = "
                     + ((editorInfo.inputType & InputType.TYPE_TEXT_FLAG_CAP_WORDS) != 0));
         }
         if (ProductionFlag.IS_EXPERIMENTAL) {
             ResearchLogger.latinIME_onStartInputViewInternal(editorInfo, mPrefs);
         }
         if (InputAttributes.inPrivateImeOptions(null, NO_MICROPHONE_COMPAT, editorInfo)) {
             Log.w(TAG, "Deprecated private IME option specified: "
                     + editorInfo.privateImeOptions);
             Log.w(TAG, "Use " + getPackageName() + "." + NO_MICROPHONE + " instead");
         }
         if (InputAttributes.inPrivateImeOptions(getPackageName(), FORCE_ASCII, editorInfo)) {
             Log.w(TAG, "Deprecated private IME option specified: "
                     + editorInfo.privateImeOptions);
             Log.w(TAG, "Use EditorInfo.IME_FLAG_FORCE_ASCII flag instead");
         }
 
         mTargetApplicationInfo =
                 TargetApplicationGetter.getCachedApplicationInfo(editorInfo.packageName);
         if (null == mTargetApplicationInfo) {
             new TargetApplicationGetter(this /* context */, this /* listener */)
                     .execute(editorInfo.packageName);
         }
 
         LatinImeLogger.onStartInputView(editorInfo);
         // In landscape mode, this method gets called without the input view being created.
         if (inputView == null) {
             return;
         }
 
         // Forward this event to the accessibility utilities, if enabled.
         final AccessibilityUtils accessUtils = AccessibilityUtils.getInstance();
         if (accessUtils.isTouchExplorationEnabled()) {
             accessUtils.onStartInputViewInternal(editorInfo, restarting);
         }
 
         if (!restarting) {
             mSubtypeSwitcher.updateParametersOnStartInputView();
         }
 
         // The EditorInfo might have a flag that affects fullscreen mode.
         // Note: This call should be done by InputMethodService?
         updateFullscreenMode();
         mApplicationSpecifiedCompletions = null;
 
         final boolean selectionChanged = mLastSelectionStart != editorInfo.initialSelStart
                 || mLastSelectionEnd != editorInfo.initialSelEnd;
         if (!restarting || selectionChanged) {
             // If the selection changed, we reset the input state. Essentially, we come here with
             // restarting == true when the app called setText() or similar. We should reset the
             // state if the app set the text to something else, but keep it if it set a suggestion
             // or something.
             mEnteredText = null;
             resetComposingState(true /* alsoResetLastComposedWord */);
             mDeleteCount = 0;
             mSpaceState = SPACE_STATE_NONE;
 
             if (mSuggestionStripView != null) {
                 mSuggestionStripView.clear();
             }
         }
 
         if (!restarting) {
             inputView.closing();
             loadSettings();
 
             if (mSuggest != null && mCurrentSettings.mCorrectionEnabled) {
                 mSuggest.setAutoCorrectionThreshold(mCurrentSettings.mAutoCorrectionThreshold);
             }
 
             switcher.loadKeyboard(editorInfo, mCurrentSettings);
             updateKeyboardViewGestureHandlingModeByMainDictionaryAvailability();
         }
         setSuggestionStripShownInternal(
                 isSuggestionsStripVisible(), /* needsInputViewShown */ false);
 
         mLastSelectionStart = editorInfo.initialSelStart;
         mLastSelectionEnd = editorInfo.initialSelEnd;
         // If we come here something in the text state is very likely to have changed.
         // We should update the shift state regardless of whether we are restarting or not, because
         // this is not perceived as a layout change that may be disruptive like we may have with
         // switcher.loadKeyboard; in apps like Talk, we come here when the text is sent and the
         // field gets emptied and we need to re-evaluate the shift state, but not the whole layout
         // which would be disruptive.
         mKeyboardSwitcher.updateShiftState();
 
         mHandler.cancelUpdateSuggestionStrip();
         mHandler.cancelDoubleSpacesTimer();
 
         inputView.setKeyPreviewPopupEnabled(mCurrentSettings.mKeyPreviewPopupOn,
                 mCurrentSettings.mKeyPreviewPopupDismissDelay);
 
         if (TRACE) Debug.startMethodTracing("/data/trace/latinime");
     }
 
     // Callback for the TargetApplicationGetter
     @Override
     public void onTargetApplicationKnown(final ApplicationInfo info) {
         mTargetApplicationInfo = info;
     }
 
     @Override
     public void onWindowHidden() {
         if (ProductionFlag.IS_EXPERIMENTAL) {
             ResearchLogger.latinIME_onWindowHidden(mLastSelectionStart, mLastSelectionEnd,
                     getCurrentInputConnection());
         }
         super.onWindowHidden();
         KeyboardView inputView = mKeyboardSwitcher.getKeyboardView();
         if (inputView != null) inputView.closing();
     }
 
     private void onFinishInputInternal() {
         super.onFinishInput();
 
         LatinImeLogger.commit();
         if (ProductionFlag.IS_EXPERIMENTAL) {
             ResearchLogger.getInstance().latinIME_onFinishInputInternal();
         }
 
         KeyboardView inputView = mKeyboardSwitcher.getKeyboardView();
         if (inputView != null) inputView.closing();
     }
 
     private void onFinishInputViewInternal(boolean finishingInput) {
         super.onFinishInputView(finishingInput);
         mKeyboardSwitcher.onFinishInputView();
         KeyboardView inputView = mKeyboardSwitcher.getKeyboardView();
         if (inputView != null) inputView.cancelAllMessages();
         // Remove pending messages related to update suggestions
         mHandler.cancelUpdateSuggestionStrip();
     }
 
     @Override
     public void onUpdateSelection(int oldSelStart, int oldSelEnd,
             int newSelStart, int newSelEnd,
             int composingSpanStart, int composingSpanEnd) {
         super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd,
                 composingSpanStart, composingSpanEnd);
         if (DEBUG) {
             Log.i(TAG, "onUpdateSelection: oss=" + oldSelStart
                     + ", ose=" + oldSelEnd
                     + ", lss=" + mLastSelectionStart
                     + ", lse=" + mLastSelectionEnd
                     + ", nss=" + newSelStart
                     + ", nse=" + newSelEnd
                     + ", cs=" + composingSpanStart
                     + ", ce=" + composingSpanEnd);
         }
         if (ProductionFlag.IS_EXPERIMENTAL) {
             final boolean expectingUpdateSelectionFromLogger =
                     ResearchLogger.getAndClearLatinIMEExpectingUpdateSelection();
             ResearchLogger.latinIME_onUpdateSelection(mLastSelectionStart, mLastSelectionEnd,
                     oldSelStart, oldSelEnd, newSelStart, newSelEnd, composingSpanStart,
                     composingSpanEnd, mExpectingUpdateSelection,
                     expectingUpdateSelectionFromLogger, mConnection);
             if (expectingUpdateSelectionFromLogger) {
                 // TODO: Investigate. Quitting now sounds wrong - we won't do the resetting work
                 return;
             }
         }
 
         // TODO: refactor the following code to be less contrived.
         // "newSelStart != composingSpanEnd" || "newSelEnd != composingSpanEnd" means
         // that the cursor is not at the end of the composing span, or there is a selection.
         // "mLastSelectionStart != newSelStart" means that the cursor is not in the same place
         // as last time we were called (if there is a selection, it means the start hasn't
         // changed, so it's the end that did).
         final boolean selectionChanged = (newSelStart != composingSpanEnd
                 || newSelEnd != composingSpanEnd) && mLastSelectionStart != newSelStart;
         // if composingSpanStart and composingSpanEnd are -1, it means there is no composing
         // span in the view - we can use that to narrow down whether the cursor was moved
         // by us or not. If we are composing a word but there is no composing span, then
         // we know for sure the cursor moved while we were composing and we should reset
         // the state.
         final boolean noComposingSpan = composingSpanStart == -1 && composingSpanEnd == -1;
         if (!mExpectingUpdateSelection) {
             // TAKE CARE: there is a race condition when we enter this test even when the user
             // did not explicitly move the cursor. This happens when typing fast, where two keys
             // turn this flag on in succession and both onUpdateSelection() calls arrive after
             // the second one - the first call successfully avoids this test, but the second one
             // enters. For the moment we rely on noComposingSpan to further reduce the impact.
 
             // TODO: the following is probably better done in resetEntireInputState().
             // it should only happen when the cursor moved, and the very purpose of the
             // test below is to narrow down whether this happened or not. Likewise with
             // the call to postUpdateShiftState.
             // We set this to NONE because after a cursor move, we don't want the space
             // state-related special processing to kick in.
             mSpaceState = SPACE_STATE_NONE;
 
             if ((!mWordComposer.isComposingWord()) || selectionChanged || noComposingSpan) {
                 resetEntireInputState();
             }
 
             mHandler.postUpdateShiftState();
         }
         mExpectingUpdateSelection = false;
         // TODO: Decide to call restartSuggestionsOnWordBeforeCursorIfAtEndOfWord() or not
         // here. It would probably be too expensive to call directly here but we may want to post a
         // message to delay it. The point would be to unify behavior between backspace to the
         // end of a word and manually put the pointer at the end of the word.
 
         // Make a note of the cursor position
         mLastSelectionStart = newSelStart;
         mLastSelectionEnd = newSelEnd;
     }
 
     /**
      * This is called when the user has clicked on the extracted text view,
      * when running in fullscreen mode.  The default implementation hides
      * the suggestions view when this happens, but only if the extracted text
      * editor has a vertical scroll bar because its text doesn't fit.
      * Here we override the behavior due to the possibility that a re-correction could
      * cause the suggestions strip to disappear and re-appear.
      */
     @Override
     public void onExtractedTextClicked() {
         if (mCurrentSettings.isSuggestionsRequested(mDisplayOrientation)) return;
 
         super.onExtractedTextClicked();
     }
 
     /**
      * This is called when the user has performed a cursor movement in the
      * extracted text view, when it is running in fullscreen mode.  The default
      * implementation hides the suggestions view when a vertical movement
      * happens, but only if the extracted text editor has a vertical scroll bar
      * because its text doesn't fit.
      * Here we override the behavior due to the possibility that a re-correction could
      * cause the suggestions strip to disappear and re-appear.
      */
     @Override
     public void onExtractedCursorMovement(int dx, int dy) {
         if (mCurrentSettings.isSuggestionsRequested(mDisplayOrientation)) return;
 
         super.onExtractedCursorMovement(dx, dy);
     }
 
     @Override
     public void hideWindow() {
         LatinImeLogger.commit();
         mKeyboardSwitcher.onHideWindow();
 
         if (TRACE) Debug.stopMethodTracing();
         if (mOptionsDialog != null && mOptionsDialog.isShowing()) {
             mOptionsDialog.dismiss();
             mOptionsDialog = null;
         }
         super.hideWindow();
     }
 
     @Override
     public void onDisplayCompletions(CompletionInfo[] applicationSpecifiedCompletions) {
         if (DEBUG) {
             Log.i(TAG, "Received completions:");
             if (applicationSpecifiedCompletions != null) {
                 for (int i = 0; i < applicationSpecifiedCompletions.length; i++) {
                     Log.i(TAG, "  #" + i + ": " + applicationSpecifiedCompletions[i]);
                 }
             }
         }
         if (ProductionFlag.IS_EXPERIMENTAL) {
             ResearchLogger.latinIME_onDisplayCompletions(applicationSpecifiedCompletions);
         }
         if (!mCurrentSettings.isApplicationSpecifiedCompletionsOn()) return;
         mApplicationSpecifiedCompletions = applicationSpecifiedCompletions;
         if (applicationSpecifiedCompletions == null) {
             clearSuggestionStrip();
             return;
         }
 
         final ArrayList<SuggestedWords.SuggestedWordInfo> applicationSuggestedWords =
                 SuggestedWords.getFromApplicationSpecifiedCompletions(
                         applicationSpecifiedCompletions);
         final SuggestedWords suggestedWords = new SuggestedWords(
                 applicationSuggestedWords,
                 false /* typedWordValid */,
                 false /* hasAutoCorrectionCandidate */,
                 false /* isPunctuationSuggestions */,
                 false /* isObsoleteSuggestions */,
                 false /* isPrediction */);
         // When in fullscreen mode, show completions generated by the application
         final boolean isAutoCorrection = false;
         setSuggestionStrip(suggestedWords, isAutoCorrection);
         setAutoCorrectionIndicator(isAutoCorrection);
         // TODO: is this the right thing to do? What should we auto-correct to in
         // this case? This says to keep whatever the user typed.
         mWordComposer.setAutoCorrection(mWordComposer.getTypedWord());
         setSuggestionStripShown(true);
     }
 
     private void setSuggestionStripShownInternal(boolean shown, boolean needsInputViewShown) {
         // TODO: Modify this if we support suggestions with hard keyboard
         if (onEvaluateInputViewShown() && mSuggestionsContainer != null) {
             final MainKeyboardView keyboardView = mKeyboardSwitcher.getKeyboardView();
             final boolean inputViewShown = (keyboardView != null) ? keyboardView.isShown() : false;
             final boolean shouldShowSuggestions = shown
                     && (needsInputViewShown ? inputViewShown : true);
             if (isFullscreenMode()) {
                 mSuggestionsContainer.setVisibility(
                         shouldShowSuggestions ? View.VISIBLE : View.GONE);
             } else {
                 mSuggestionsContainer.setVisibility(
                         shouldShowSuggestions ? View.VISIBLE : View.INVISIBLE);
             }
         }
     }
 
     private void setSuggestionStripShown(boolean shown) {
         setSuggestionStripShownInternal(shown, /* needsInputViewShown */true);
     }
 
     private int getAdjustedBackingViewHeight() {
         final int currentHeight = mKeyPreviewBackingView.getHeight();
         if (currentHeight > 0) {
             return currentHeight;
         }
 
         final KeyboardView keyboardView = mKeyboardSwitcher.getKeyboardView();
         if (keyboardView == null) {
             return 0;
         }
         final int keyboardHeight = keyboardView.getHeight();
         final int suggestionsHeight = mSuggestionsContainer.getHeight();
         final int displayHeight = mResources.getDisplayMetrics().heightPixels;
         final Rect rect = new Rect();
         mKeyPreviewBackingView.getWindowVisibleDisplayFrame(rect);
         final int notificationBarHeight = rect.top;
         final int remainingHeight = displayHeight - notificationBarHeight - suggestionsHeight
                 - keyboardHeight;
 
         final LayoutParams params = mKeyPreviewBackingView.getLayoutParams();
         params.height = mSuggestionStripView.setMoreSuggestionsHeight(remainingHeight);
         mKeyPreviewBackingView.setLayoutParams(params);
         return params.height;
     }
 
     @Override
     public void onComputeInsets(InputMethodService.Insets outInsets) {
         super.onComputeInsets(outInsets);
         final KeyboardView inputView = mKeyboardSwitcher.getKeyboardView();
         if (inputView == null || mSuggestionsContainer == null)
             return;
         final int adjustedBackingHeight = getAdjustedBackingViewHeight();
         final boolean backingGone = (mKeyPreviewBackingView.getVisibility() == View.GONE);
         final int backingHeight = backingGone ? 0 : adjustedBackingHeight;
         // In fullscreen mode, the height of the extract area managed by InputMethodService should
         // be considered.
         // See {@link android.inputmethodservice.InputMethodService#onComputeInsets}.
         final int extractHeight = isFullscreenMode() ? mExtractArea.getHeight() : 0;
         final int suggestionsHeight = (mSuggestionsContainer.getVisibility() == View.GONE) ? 0
                 : mSuggestionsContainer.getHeight();
         final int extraHeight = extractHeight + backingHeight + suggestionsHeight;
         int touchY = extraHeight;
         // Need to set touchable region only if input view is being shown
         final MainKeyboardView keyboardView = mKeyboardSwitcher.getKeyboardView();
         if (keyboardView != null && keyboardView.isShown()) {
             if (mSuggestionsContainer.getVisibility() == View.VISIBLE) {
                 touchY -= suggestionsHeight;
             }
             final int touchWidth = inputView.getWidth();
             final int touchHeight = inputView.getHeight() + extraHeight
                     // Extend touchable region below the keyboard.
                     + EXTENDED_TOUCHABLE_REGION_HEIGHT;
             outInsets.touchableInsets = InputMethodService.Insets.TOUCHABLE_INSETS_REGION;
             outInsets.touchableRegion.set(0, touchY, touchWidth, touchHeight);
         }
         outInsets.contentTopInsets = touchY;
         outInsets.visibleTopInsets = touchY;
     }
 
     @Override
     public boolean onEvaluateFullscreenMode() {
         // Reread resource value here, because this method is called by framework anytime as needed.
         final boolean isFullscreenModeAllowed =
                 mCurrentSettings.isFullscreenModeAllowed(getResources());
         return super.onEvaluateFullscreenMode() && isFullscreenModeAllowed;
     }
 
     @Override
     public void updateFullscreenMode() {
         super.updateFullscreenMode();
 
         if (mKeyPreviewBackingView == null) return;
         // In fullscreen mode, no need to have extra space to show the key preview.
         // If not, we should have extra space above the keyboard to show the key preview.
         mKeyPreviewBackingView.setVisibility(isFullscreenMode() ? View.GONE : View.VISIBLE);
     }
 
     // This will reset the whole input state to the starting state. It will clear
     // the composing word, reset the last composed word, tell the inputconnection about it.
     private void resetEntireInputState() {
         resetComposingState(true /* alsoResetLastComposedWord */);
         clearSuggestionStrip();
         mConnection.finishComposingText();
     }
 
     private void resetComposingState(final boolean alsoResetLastComposedWord) {
         mWordComposer.reset();
         if (alsoResetLastComposedWord)
             mLastComposedWord = LastComposedWord.NOT_A_COMPOSED_WORD;
     }
 
     private void commitTyped(final int separatorCode) {
         if (!mWordComposer.isComposingWord()) return;
         final CharSequence typedWord = mWordComposer.getTypedWord();
         if (typedWord.length() > 0) {
             mConnection.commitText(typedWord, 1);
             if (ProductionFlag.IS_EXPERIMENTAL) {
                 ResearchLogger.latinIME_commitText(typedWord);
             }
             final CharSequence prevWord = addToUserHistoryDictionary(typedWord);
             mLastComposedWord = mWordComposer.commitWord(
                     LastComposedWord.COMMIT_TYPE_USER_TYPED_WORD, typedWord.toString(),
                     separatorCode, prevWord);
         }
     }
 
     // Called from the KeyboardSwitcher which needs to know auto caps state to display
     // the right layout.
     public int getCurrentAutoCapsState() {
         if (!mCurrentSettings.mAutoCap) return Constants.TextUtils.CAP_MODE_OFF;
 
         final EditorInfo ei = getCurrentInputEditorInfo();
         if (ei == null) return Constants.TextUtils.CAP_MODE_OFF;
 
         final int inputType = ei.inputType;
         if ((inputType & InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS) != 0) {
             return TextUtils.CAP_MODE_CHARACTERS;
         }
 
         final boolean noNeedToCheckCapsMode = (inputType & (InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                 | InputType.TYPE_TEXT_FLAG_CAP_WORDS)) == 0;
         if (noNeedToCheckCapsMode) return Constants.TextUtils.CAP_MODE_OFF;
 
         // Avoid making heavy round-trip IPC calls of {@link InputConnection#getCursorCapsMode}
         // unless needed.
         if (mWordComposer.isComposingWord()) return Constants.TextUtils.CAP_MODE_OFF;
 
         // TODO: This blocking IPC call is heavy. Consider doing this without using IPC calls.
         // Note: getCursorCapsMode() returns the current capitalization mode that is any
         // combination of CAP_MODE_CHARACTERS, CAP_MODE_WORDS, and CAP_MODE_SENTENCES. 0 means none
         // of them.
         return mConnection.getCursorCapsMode(inputType);
     }
 
     private void swapSwapperAndSpace() {
         CharSequence lastTwo = mConnection.getTextBeforeCursor(2, 0);
         // It is guaranteed lastTwo.charAt(1) is a swapper - else this method is not called.
         if (lastTwo != null && lastTwo.length() == 2
                 && lastTwo.charAt(0) == Keyboard.CODE_SPACE) {
             mConnection.deleteSurroundingText(2, 0);
             if (ProductionFlag.IS_EXPERIMENTAL) {
                 ResearchLogger.latinIME_deleteSurroundingText(2);
             }
             mConnection.commitText(lastTwo.charAt(1) + " ", 1);
             if (ProductionFlag.IS_EXPERIMENTAL) {
                 ResearchLogger.latinIME_swapSwapperAndSpaceWhileInBatchEdit();
             }
             mKeyboardSwitcher.updateShiftState();
         }
     }
 
     private boolean maybeDoubleSpace() {
         if (!mCurrentSettings.mCorrectionEnabled) return false;
         if (!mHandler.isAcceptingDoubleSpaces()) return false;
         final CharSequence lastThree = mConnection.getTextBeforeCursor(3, 0);
         if (lastThree != null && lastThree.length() == 3
                 && canBeFollowedByPeriod(lastThree.charAt(0))
                 && lastThree.charAt(1) == Keyboard.CODE_SPACE
                 && lastThree.charAt(2) == Keyboard.CODE_SPACE) {
             mHandler.cancelDoubleSpacesTimer();
             mConnection.deleteSurroundingText(2, 0);
             mConnection.commitText(". ", 1);
             if (ProductionFlag.IS_EXPERIMENTAL) {
                 ResearchLogger.latinIME_doubleSpaceAutoPeriod();
             }
             mKeyboardSwitcher.updateShiftState();
             return true;
         }
         return false;
     }
 
     private static boolean canBeFollowedByPeriod(final int codePoint) {
         // TODO: Check again whether there really ain't a better way to check this.
         // TODO: This should probably be language-dependant...
         return Character.isLetterOrDigit(codePoint)
                 || codePoint == Keyboard.CODE_SINGLE_QUOTE
                 || codePoint == Keyboard.CODE_DOUBLE_QUOTE
                 || codePoint == Keyboard.CODE_CLOSING_PARENTHESIS
                 || codePoint == Keyboard.CODE_CLOSING_SQUARE_BRACKET
                 || codePoint == Keyboard.CODE_CLOSING_CURLY_BRACKET
                 || codePoint == Keyboard.CODE_CLOSING_ANGLE_BRACKET;
     }
 
     // Callback for the {@link SuggestionStripView}, to call when the "add to dictionary" hint is
     // pressed.
     @Override
     public boolean addWordToUserDictionary(String word) {
         mUserDictionary.addWordToUserDictionary(word, 128);
         return true;
     }
 
     private static boolean isAlphabet(int code) {
         return Character.isLetter(code);
     }
 
     private void onSettingsKeyPressed() {
         if (isShowingOptionDialog()) return;
         showSubtypeSelectorAndSettings();
     }
 
     // Virtual codes representing custom requests.  These are used in onCustomRequest() below.
     public static final int CODE_SHOW_INPUT_METHOD_PICKER = 1;
 
     @Override
     public boolean onCustomRequest(int requestCode) {
         if (isShowingOptionDialog()) return false;
         switch (requestCode) {
         case CODE_SHOW_INPUT_METHOD_PICKER:
             if (ImfUtils.hasMultipleEnabledIMEsOrSubtypes(
                     this, true /* include aux subtypes */)) {
                 mImm.showInputMethodPicker();
                 return true;
             }
             return false;
         }
         return false;
     }
 
     private boolean isShowingOptionDialog() {
         return mOptionsDialog != null && mOptionsDialog.isShowing();
     }
 
     private static int getActionId(Keyboard keyboard) {
         return keyboard != null ? keyboard.mId.imeActionId() : EditorInfo.IME_ACTION_NONE;
     }
 
     private void performEditorAction(int actionId) {
         mConnection.performEditorAction(actionId);
         if (ProductionFlag.IS_EXPERIMENTAL) {
             ResearchLogger.latinIME_performEditorAction(actionId);
         }
     }
 
     private void handleLanguageSwitchKey() {
         final boolean includesOtherImes = mCurrentSettings.mIncludesOtherImesInLanguageSwitchList;
         final IBinder token = getWindow().getWindow().getAttributes().token;
         if (mShouldSwitchToLastSubtype) {
             final InputMethodSubtype lastSubtype = mImm.getLastInputMethodSubtype();
             final boolean lastSubtypeBelongsToThisIme =
                     ImfUtils.checkIfSubtypeBelongsToThisImeAndEnabled(this, lastSubtype);
             if ((includesOtherImes || lastSubtypeBelongsToThisIme)
                     && mImm.switchToLastInputMethod(token)) {
                 mShouldSwitchToLastSubtype = false;
             } else {
                 mImm.switchToNextInputMethod(token, !includesOtherImes);
                 mShouldSwitchToLastSubtype = true;
             }
         } else {
             mImm.switchToNextInputMethod(token, !includesOtherImes);
         }
     }
 
     private void sendUpDownEnterOrBackspace(final int code) {
         final long eventTime = SystemClock.uptimeMillis();
         mConnection.sendKeyEvent(new KeyEvent(eventTime, eventTime,
                 KeyEvent.ACTION_DOWN, code, 0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
                 KeyEvent.FLAG_SOFT_KEYBOARD | KeyEvent.FLAG_KEEP_TOUCH_MODE));
         mConnection.sendKeyEvent(new KeyEvent(SystemClock.uptimeMillis(), eventTime,
                 KeyEvent.ACTION_UP, code, 0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
                 KeyEvent.FLAG_SOFT_KEYBOARD | KeyEvent.FLAG_KEEP_TOUCH_MODE));
     }
 
     private void sendKeyCodePoint(int code) {
         // TODO: Remove this special handling of digit letters.
         // For backward compatibility. See {@link InputMethodService#sendKeyChar(char)}.
         if (code >= '0' && code <= '9') {
             super.sendKeyChar((char)code);
             return;
         }
 
         // 16 is android.os.Build.VERSION_CODES.JELLY_BEAN but we can't write it because
         // we want to be able to compile against the Ice Cream Sandwich SDK.
         if (Keyboard.CODE_ENTER == code && mTargetApplicationInfo != null
                 && mTargetApplicationInfo.targetSdkVersion < 16) {
             // Backward compatibility mode. Before Jelly bean, the keyboard would simulate
             // a hardware keyboard event on pressing enter or delete. This is bad for many
             // reasons (there are race conditions with commits) but some applications are
             // relying on this behavior so we continue to support it for older apps.
             sendUpDownEnterOrBackspace(KeyEvent.KEYCODE_ENTER);
         } else {
             final String text = new String(new int[] { code }, 0, 1);
             mConnection.commitText(text, text.length());
         }
         if (ProductionFlag.IS_EXPERIMENTAL) {
             ResearchLogger.latinIME_sendKeyCodePoint(code);
         }
     }
 
     // Implementation of {@link KeyboardActionListener}.
     @Override
     public void onCodeInput(int primaryCode, int x, int y) {
         final long when = SystemClock.uptimeMillis();
         if (primaryCode != Keyboard.CODE_DELETE || when > mLastKeyTime + QUICK_PRESS) {
             mDeleteCount = 0;
         }
         mLastKeyTime = when;
         mConnection.beginBatchEdit();
 
         if (ProductionFlag.IS_EXPERIMENTAL) {
             ResearchLogger.latinIME_onCodeInput(primaryCode, x, y);
         }
 
         final KeyboardSwitcher switcher = mKeyboardSwitcher;
         // The space state depends only on the last character pressed and its own previous
         // state. Here, we revert the space state to neutral if the key is actually modifying
         // the input contents (any non-shift key), which is what we should do for
         // all inputs that do not result in a special state. Each character handling is then
         // free to override the state as they see fit.
         final int spaceState = mSpaceState;
         if (!mWordComposer.isComposingWord()) mIsAutoCorrectionIndicatorOn = false;
 
         // TODO: Consolidate the double space timer, mLastKeyTime, and the space state.
         if (primaryCode != Keyboard.CODE_SPACE) {
             mHandler.cancelDoubleSpacesTimer();
         }
 
         boolean didAutoCorrect = false;
         switch (primaryCode) {
         case Keyboard.CODE_DELETE:
             mSpaceState = SPACE_STATE_NONE;
             handleBackspace(spaceState);
             mDeleteCount++;
             mExpectingUpdateSelection = true;
             mShouldSwitchToLastSubtype = true;
             LatinImeLogger.logOnDelete(x, y);
             break;
         case Keyboard.CODE_SHIFT:
         case Keyboard.CODE_SWITCH_ALPHA_SYMBOL:
             // Shift and symbol key is handled in onPressKey() and onReleaseKey().
             break;
         case Keyboard.CODE_SETTINGS:
             onSettingsKeyPressed();
             break;
         case Keyboard.CODE_SHORTCUT:
             mSubtypeSwitcher.switchToShortcutIME();
             break;
         case Keyboard.CODE_ACTION_ENTER:
             performEditorAction(getActionId(switcher.getKeyboard()));
             break;
         case Keyboard.CODE_ACTION_NEXT:
             performEditorAction(EditorInfo.IME_ACTION_NEXT);
             break;
         case Keyboard.CODE_ACTION_PREVIOUS:
             performEditorAction(EditorInfo.IME_ACTION_PREVIOUS);
             break;
         case Keyboard.CODE_LANGUAGE_SWITCH:
             handleLanguageSwitchKey();
             break;
         case Keyboard.CODE_RESEARCH:
             if (ProductionFlag.IS_EXPERIMENTAL) {
                 ResearchLogger.getInstance().presentResearchDialog(this);
             }
             break;
         default:
             mSpaceState = SPACE_STATE_NONE;
             if (mCurrentSettings.isWordSeparator(primaryCode)) {
                 didAutoCorrect = handleSeparator(primaryCode, x, y, spaceState);
             } else {
                 if (SPACE_STATE_PHANTOM == spaceState) {
                     commitTyped(LastComposedWord.NOT_A_SEPARATOR);
                 }
                 final int keyX, keyY;
                 final Keyboard keyboard = mKeyboardSwitcher.getKeyboard();
                 if (keyboard != null && keyboard.hasProximityCharsCorrection(primaryCode)) {
                     keyX = x;
                     keyY = y;
                 } else {
                     keyX = NOT_A_TOUCH_COORDINATE;
                     keyY = NOT_A_TOUCH_COORDINATE;
                 }
                 handleCharacter(primaryCode, keyX, keyY, spaceState);
             }
             mExpectingUpdateSelection = true;
             mShouldSwitchToLastSubtype = true;
             break;
         }
         switcher.onCodeInput(primaryCode);
         // Reset after any single keystroke, except shift and symbol-shift
         if (!didAutoCorrect && primaryCode != Keyboard.CODE_SHIFT
                 && primaryCode != Keyboard.CODE_SWITCH_ALPHA_SYMBOL)
             mLastComposedWord.deactivate();
         mEnteredText = null;
         mConnection.endBatchEdit();
     }
 
     // Called from PointerTracker through the KeyboardActionListener interface
     @Override
    public void onTextInput(CharSequence rawText) {
         mConnection.beginBatchEdit();
         commitTyped(LastComposedWord.NOT_A_SEPARATOR);
        mHandler.postUpdateSuggestionStrip();
        final CharSequence text = specificTldProcessingOnTextInput(rawText);
         if (SPACE_STATE_PHANTOM == mSpaceState) {
             sendKeyCodePoint(Keyboard.CODE_SPACE);
         }
         mConnection.commitText(text, 1);
         if (ProductionFlag.IS_EXPERIMENTAL) {
             ResearchLogger.latinIME_commitText(text);
         }
         mConnection.endBatchEdit();
         mKeyboardSwitcher.updateShiftState();
         mKeyboardSwitcher.onCodeInput(Keyboard.CODE_OUTPUT_TEXT);
         mSpaceState = SPACE_STATE_NONE;
         mEnteredText = text;
         resetComposingState(true /* alsoResetLastComposedWord */);
     }
 
     @Override
     public void onStartBatchInput() {
         mConnection.beginBatchEdit();
         if (mWordComposer.isComposingWord()) {
             commitTyped(LastComposedWord.NOT_A_SEPARATOR);
             mExpectingUpdateSelection = true;
             // TODO: Can we remove this?
             mSpaceState = SPACE_STATE_PHANTOM;
         }
         mConnection.endBatchEdit();
         // TODO: Should handle TextUtils.CAP_MODE_CHARACTER.
         mWordComposer.setAutoCapitalized(
                 getCurrentAutoCapsState() != Constants.TextUtils.CAP_MODE_OFF);
     }
 
     @Override
     public void onUpdateBatchInput(InputPointers batchPointers) {
         mWordComposer.setBatchInputPointers(batchPointers);
         final SuggestedWords suggestedWords = getSuggestedWords();
         showSuggestionStrip(suggestedWords, null);
         final String gestureFloatingPreviewText = (suggestedWords.size() > 0)
                 ? suggestedWords.getWord(0) : null;
         mKeyboardSwitcher.getKeyboardView()
                 .showGestureFloatingPreviewText(gestureFloatingPreviewText);
     }
 
     @Override
     public void onEndBatchInput(InputPointers batchPointers) {
         mWordComposer.setBatchInputPointers(batchPointers);
         final SuggestedWords suggestedWords = getSuggestedWords();
         showSuggestionStrip(suggestedWords, null);
         mKeyboardSwitcher.getKeyboardView().showGestureFloatingPreviewText(null);
         if (suggestedWords == null || suggestedWords.size() == 0) {
             return;
         }
         final CharSequence text = suggestedWords.getWord(0);
         if (TextUtils.isEmpty(text)) {
             return;
         }
         mWordComposer.setBatchInputWord(text);
         mConnection.beginBatchEdit();
         if (SPACE_STATE_PHANTOM == mSpaceState) {
             sendKeyCodePoint(Keyboard.CODE_SPACE);
         }
         mConnection.setComposingText(text, 1);
         mExpectingUpdateSelection = true;
         mConnection.endBatchEdit();
         mKeyboardSwitcher.updateShiftState();
         mSpaceState = SPACE_STATE_PHANTOM;
     }
 
     private CharSequence specificTldProcessingOnTextInput(final CharSequence text) {
         if (text.length() <= 1 || text.charAt(0) != Keyboard.CODE_PERIOD
                 || !Character.isLetter(text.charAt(1))) {
             // Not a tld: do nothing.
             return text;
         }
         // We have a TLD (or something that looks like this): make sure we don't add
         // a space even if currently in phantom mode.
         mSpaceState = SPACE_STATE_NONE;
         final CharSequence lastOne = mConnection.getTextBeforeCursor(1, 0);
         if (lastOne != null && lastOne.length() == 1
                 && lastOne.charAt(0) == Keyboard.CODE_PERIOD) {
             return text.subSequence(1, text.length());
         } else {
             return text;
         }
     }
 
     // Called from PointerTracker through the KeyboardActionListener interface
     @Override
     public void onCancelInput() {
         // User released a finger outside any key
         mKeyboardSwitcher.onCancelInput();
     }
 
     private void handleBackspace(final int spaceState) {
         // In many cases, we may have to put the keyboard in auto-shift state again.
         mHandler.postUpdateShiftState();
 
         if (mEnteredText != null && mConnection.sameAsTextBeforeCursor(mEnteredText)) {
             // Cancel multi-character input: remove the text we just entered.
             // This is triggered on backspace after a key that inputs multiple characters,
             // like the smiley key or the .com key.
             final int length = mEnteredText.length();
             mConnection.deleteSurroundingText(length, 0);
             if (ProductionFlag.IS_EXPERIMENTAL) {
                 ResearchLogger.latinIME_deleteSurroundingText(length);
             }
             // If we have mEnteredText, then we know that mHasUncommittedTypedChars == false.
             // In addition we know that spaceState is false, and that we should not be
             // reverting any autocorrect at this point. So we can safely return.
             return;
         }
 
         if (mWordComposer.isComposingWord()) {
             final int length = mWordComposer.size();
             if (length > 0) {
                 // Immediately after a batch input.
                 if (SPACE_STATE_PHANTOM == spaceState) {
                     mWordComposer.reset();
                 } else {
                     mWordComposer.deleteLast();
                 }
                 mConnection.setComposingText(getTextWithUnderline(mWordComposer.getTypedWord()), 1);
                 mHandler.postUpdateSuggestionStrip();
             } else {
                 mConnection.deleteSurroundingText(1, 0);
                 if (ProductionFlag.IS_EXPERIMENTAL) {
                     ResearchLogger.latinIME_deleteSurroundingText(1);
                 }
             }
         } else {
             if (mLastComposedWord.canRevertCommit()) {
                 Utils.Stats.onAutoCorrectionCancellation();
                 revertCommit();
                 return;
             }
             if (SPACE_STATE_DOUBLE == spaceState) {
                 mHandler.cancelDoubleSpacesTimer();
                 if (mConnection.revertDoubleSpace()) {
                     // No need to reset mSpaceState, it has already be done (that's why we
                     // receive it as a parameter)
                     return;
                 }
             } else if (SPACE_STATE_SWAP_PUNCTUATION == spaceState) {
                 if (mConnection.revertSwapPunctuation()) {
                     // Likewise
                     return;
                 }
             }
 
             // No cancelling of commit/double space/swap: we have a regular backspace.
             // We should backspace one char and restart suggestion if at the end of a word.
             if (mLastSelectionStart != mLastSelectionEnd) {
                 // If there is a selection, remove it.
                 final int lengthToDelete = mLastSelectionEnd - mLastSelectionStart;
                 mConnection.setSelection(mLastSelectionEnd, mLastSelectionEnd);
                 mConnection.deleteSurroundingText(lengthToDelete, 0);
                 if (ProductionFlag.IS_EXPERIMENTAL) {
                     ResearchLogger.latinIME_deleteSurroundingText(lengthToDelete);
                 }
             } else {
                 // There is no selection, just delete one character.
                 if (NOT_A_CURSOR_POSITION == mLastSelectionEnd) {
                     // This should never happen.
                     Log.e(TAG, "Backspace when we don't know the selection position");
                 }
                 // 16 is android.os.Build.VERSION_CODES.JELLY_BEAN but we can't write it because
                 // we want to be able to compile against the Ice Cream Sandwich SDK.
                 if (mTargetApplicationInfo != null
                         && mTargetApplicationInfo.targetSdkVersion < 16) {
                     // Backward compatibility mode. Before Jelly bean, the keyboard would simulate
                     // a hardware keyboard event on pressing enter or delete. This is bad for many
                     // reasons (there are race conditions with commits) but some applications are
                     // relying on this behavior so we continue to support it for older apps.
                     sendUpDownEnterOrBackspace(KeyEvent.KEYCODE_DEL);
                 } else {
                     mConnection.deleteSurroundingText(1, 0);
                 }
                 if (ProductionFlag.IS_EXPERIMENTAL) {
                     ResearchLogger.latinIME_deleteSurroundingText(1);
                 }
                 if (mDeleteCount > DELETE_ACCELERATE_AT) {
                     mConnection.deleteSurroundingText(1, 0);
                     if (ProductionFlag.IS_EXPERIMENTAL) {
                         ResearchLogger.latinIME_deleteSurroundingText(1);
                     }
                 }
             }
             if (mCurrentSettings.isSuggestionsRequested(mDisplayOrientation)) {
                 restartSuggestionsOnWordBeforeCursorIfAtEndOfWord();
             }
         }
     }
 
     private boolean maybeStripSpace(final int code,
             final int spaceState, final boolean isFromSuggestionStrip) {
         if (Keyboard.CODE_ENTER == code && SPACE_STATE_SWAP_PUNCTUATION == spaceState) {
             mConnection.removeTrailingSpace();
             return false;
         } else if ((SPACE_STATE_WEAK == spaceState
                 || SPACE_STATE_SWAP_PUNCTUATION == spaceState)
                 && isFromSuggestionStrip) {
             if (mCurrentSettings.isWeakSpaceSwapper(code)) {
                 return true;
             } else {
                 if (mCurrentSettings.isWeakSpaceStripper(code)) {
                     mConnection.removeTrailingSpace();
                 }
                 return false;
             }
         } else {
             return false;
         }
     }
 
     private void handleCharacter(final int primaryCode, final int x,
             final int y, final int spaceState) {
         boolean isComposingWord = mWordComposer.isComposingWord();
 
         if (SPACE_STATE_PHANTOM == spaceState &&
                 !mCurrentSettings.isSymbolExcludedFromWordSeparators(primaryCode)) {
             if (isComposingWord) {
                 // Sanity check
                 throw new RuntimeException("Should not be composing here");
             }
             sendKeyCodePoint(Keyboard.CODE_SPACE);
         }
 
         // NOTE: isCursorTouchingWord() is a blocking IPC call, so it often takes several
         // dozen milliseconds. Avoid calling it as much as possible, since we are on the UI
         // thread here.
         if (!isComposingWord && (isAlphabet(primaryCode)
                 || mCurrentSettings.isSymbolExcludedFromWordSeparators(primaryCode))
                 && mCurrentSettings.isSuggestionsRequested(mDisplayOrientation) &&
                 !mConnection.isCursorTouchingWord(mCurrentSettings)) {
             // Reset entirely the composing state anyway, then start composing a new word unless
             // the character is a single quote. The idea here is, single quote is not a
             // separator and it should be treated as a normal character, except in the first
             // position where it should not start composing a word.
             isComposingWord = (Keyboard.CODE_SINGLE_QUOTE != primaryCode);
             // Here we don't need to reset the last composed word. It will be reset
             // when we commit this one, if we ever do; if on the other hand we backspace
             // it entirely and resume suggestions on the previous word, we'd like to still
             // have touch coordinates for it.
             resetComposingState(false /* alsoResetLastComposedWord */);
         }
         if (isComposingWord) {
             final int keyX, keyY;
             if (KeyboardActionListener.Adapter.isInvalidCoordinate(x)
                     || KeyboardActionListener.Adapter.isInvalidCoordinate(y)) {
                 keyX = x;
                 keyY = y;
             } else {
                 final KeyDetector keyDetector =
                         mKeyboardSwitcher.getKeyboardView().getKeyDetector();
                 keyX = keyDetector.getTouchX(x);
                 keyY = keyDetector.getTouchY(y);
             }
             mWordComposer.add(primaryCode, keyX, keyY);
             // If it's the first letter, make note of auto-caps state
             if (mWordComposer.size() == 1) {
                 mWordComposer.setAutoCapitalized(
                         getCurrentAutoCapsState() != Constants.TextUtils.CAP_MODE_OFF);
             }
             mConnection.setComposingText(getTextWithUnderline(mWordComposer.getTypedWord()), 1);
         } else {
             final boolean swapWeakSpace = maybeStripSpace(primaryCode,
                     spaceState, KeyboardActionListener.SUGGESTION_STRIP_COORDINATE == x);
 
             sendKeyCodePoint(primaryCode);
 
             if (swapWeakSpace) {
                 swapSwapperAndSpace();
                 mSpaceState = SPACE_STATE_WEAK;
             }
             // In case the "add to dictionary" hint was still displayed.
             if (null != mSuggestionStripView) mSuggestionStripView.dismissAddToDictionaryHint();
         }
         mHandler.postUpdateSuggestionStrip();
         Utils.Stats.onNonSeparator((char)primaryCode, x, y);
     }
 
     // Returns true if we did an autocorrection, false otherwise.
     private boolean handleSeparator(final int primaryCode, final int x, final int y,
             final int spaceState) {
         boolean didAutoCorrect = false;
         // Handle separator
         if (mWordComposer.isComposingWord()) {
             if (mCurrentSettings.mCorrectionEnabled) {
                 commitCurrentAutoCorrection(primaryCode);
                 didAutoCorrect = true;
             } else {
                 commitTyped(primaryCode);
             }
         }
 
         final boolean swapWeakSpace = maybeStripSpace(primaryCode, spaceState,
                 KeyboardActionListener.SUGGESTION_STRIP_COORDINATE == x);
 
         if (SPACE_STATE_PHANTOM == spaceState &&
                 mCurrentSettings.isPhantomSpacePromotingSymbol(primaryCode)) {
             sendKeyCodePoint(Keyboard.CODE_SPACE);
         }
         sendKeyCodePoint(primaryCode);
 
         if (Keyboard.CODE_SPACE == primaryCode) {
             if (mCurrentSettings.isSuggestionsRequested(mDisplayOrientation)) {
                 if (maybeDoubleSpace()) {
                     mSpaceState = SPACE_STATE_DOUBLE;
                 } else if (!isShowingPunctuationList()) {
                     mSpaceState = SPACE_STATE_WEAK;
                 }
             }
 
             mHandler.startDoubleSpacesTimer();
             if (!mConnection.isCursorTouchingWord(mCurrentSettings)) {
                 mHandler.postUpdateSuggestionStrip();
             }
         } else {
             if (swapWeakSpace) {
                 swapSwapperAndSpace();
                 mSpaceState = SPACE_STATE_SWAP_PUNCTUATION;
             } else if (SPACE_STATE_PHANTOM == spaceState
                     && !mCurrentSettings.isWeakSpaceStripper(primaryCode)) {
                 // If we are in phantom space state, and the user presses a separator, we want to
                 // stay in phantom space state so that the next keypress has a chance to add the
                 // space. For example, if I type "Good dat", pick "day" from the suggestion strip
                 // then insert a comma and go on to typing the next word, I want the space to be
                 // inserted automatically before the next word, the same way it is when I don't
                 // input the comma.
                 // The case is a little different if the separator is a space stripper. Such a
                 // separator does not normally need a space on the right (that's the difference
                 // between swappers and strippers), so we should not stay in phantom space state if
                 // the separator is a stripper. Hence the additional test above.
                 mSpaceState = SPACE_STATE_PHANTOM;
             }
 
             // Set punctuation right away. onUpdateSelection will fire but tests whether it is
             // already displayed or not, so it's okay.
             setPunctuationSuggestions();
         }
 
         Utils.Stats.onSeparator((char)primaryCode, x, y);
 
         return didAutoCorrect;
     }
 
     private CharSequence getTextWithUnderline(final CharSequence text) {
         return mIsAutoCorrectionIndicatorOn
                 ? SuggestionSpanUtils.getTextWithAutoCorrectionIndicatorUnderline(this, text)
                 : text;
     }
 
     private void handleClose() {
         commitTyped(LastComposedWord.NOT_A_SEPARATOR);
         requestHideSelf(0);
         MainKeyboardView inputView = mKeyboardSwitcher.getKeyboardView();
        if (inputView != null) {
             inputView.closing();
        }
     }
 
     // TODO: make this private
     // Outside LatinIME, only used by the test suite.
     /* package for tests */ boolean isShowingPunctuationList() {
         if (mSuggestionStripView == null) return false;
         return mCurrentSettings.mSuggestPuncList == mSuggestionStripView.getSuggestions();
     }
 
     private boolean isSuggestionsStripVisible() {
         if (mSuggestionStripView == null)
             return false;
         if (mSuggestionStripView.isShowingAddToDictionaryHint())
             return true;
         if (!mCurrentSettings.isSuggestionStripVisibleInOrientation(mDisplayOrientation))
             return false;
         if (mCurrentSettings.isApplicationSpecifiedCompletionsOn())
             return true;
         return mCurrentSettings.isSuggestionsRequested(mDisplayOrientation);
     }
 
     private void clearSuggestionStrip() {
         setSuggestionStrip(SuggestedWords.EMPTY, false);
         setAutoCorrectionIndicator(false);
     }
 
     private void setSuggestionStrip(final SuggestedWords words, final boolean isAutoCorrection) {
         if (mSuggestionStripView != null) {
             mSuggestionStripView.setSuggestions(words);
             mKeyboardSwitcher.onAutoCorrectionStateChanged(isAutoCorrection);
         }
     }
 
     private void setAutoCorrectionIndicator(final boolean newAutoCorrectionIndicator) {
         // Put a blue underline to a word in TextView which will be auto-corrected.
         if (mIsAutoCorrectionIndicatorOn != newAutoCorrectionIndicator
                 && mWordComposer.isComposingWord()) {
             mIsAutoCorrectionIndicatorOn = newAutoCorrectionIndicator;
             final CharSequence textWithUnderline =
                     getTextWithUnderline(mWordComposer.getTypedWord());
             mConnection.setComposingText(textWithUnderline, 1);
         }
     }
 
     private void updateSuggestionStrip() {
         mHandler.cancelUpdateSuggestionStrip();
 
         // Check if we have a suggestion engine attached.
         if (mSuggest == null || !mCurrentSettings.isSuggestionsRequested(mDisplayOrientation)) {
             if (mWordComposer.isComposingWord()) {
                 Log.w(TAG, "Called updateSuggestionsOrPredictions but suggestions were not "
                         + "requested!");
                 mWordComposer.setAutoCorrection(mWordComposer.getTypedWord());
             }
             return;
         }
 
         if (!mWordComposer.isComposingWord() && !mCurrentSettings.mBigramPredictionEnabled) {
             setPunctuationSuggestions();
             return;
         }
 
         final SuggestedWords suggestedWords = getSuggestedWords();
         final String typedWord = mWordComposer.getTypedWord();
         showSuggestionStrip(suggestedWords, typedWord);
     }
 
     private SuggestedWords getSuggestedWords() {
         final String typedWord = mWordComposer.getTypedWord();
         // Get the word on which we should search the bigrams. If we are composing a word, it's
         // whatever is *before* the half-committed word in the buffer, hence 2; if we aren't, we
         // should just skip whitespace if any, so 1.
         // TODO: this is slow (2-way IPC) - we should probably cache this instead.
         final CharSequence prevWord =
                 mConnection.getNthPreviousWord(mCurrentSettings.mWordSeparators,
                 mWordComposer.isComposingWord() ? 2 : 1);
         final SuggestedWords suggestedWords = mSuggest.getSuggestedWords(mWordComposer,
                 prevWord, mKeyboardSwitcher.getKeyboard().getProximityInfo(),
                 mCurrentSettings.mCorrectionEnabled);
         return maybeRetrieveOlderSuggestions(typedWord, suggestedWords);
     }
 
     private SuggestedWords maybeRetrieveOlderSuggestions(final CharSequence typedWord,
             final SuggestedWords suggestedWords) {
         // TODO: consolidate this into getSuggestedWords
         // We update the suggestion strip only when we have some suggestions to show, i.e. when
         // the suggestion count is > 1; else, we leave the old suggestions, with the typed word
         // replaced with the new one. However, when the word is a dictionary word, or when the
         // length of the typed word is 1 or 0 (after a deletion typically), we do want to remove the
         // old suggestions. Also, if we are showing the "add to dictionary" hint, we need to
         // revert to suggestions - although it is unclear how we can come here if it's displayed.
         if (suggestedWords.size() > 1 || typedWord.length() <= 1
                 || !suggestedWords.mTypedWordValid
                 || mSuggestionStripView.isShowingAddToDictionaryHint()) {
             return suggestedWords;
         } else {
             SuggestedWords previousSuggestions = mSuggestionStripView.getSuggestions();
             if (previousSuggestions == mCurrentSettings.mSuggestPuncList) {
                 previousSuggestions = SuggestedWords.EMPTY;
             }
             final ArrayList<SuggestedWords.SuggestedWordInfo> typedWordAndPreviousSuggestions =
                     SuggestedWords.getTypedWordAndPreviousSuggestions(
                             typedWord, previousSuggestions);
             return new SuggestedWords(typedWordAndPreviousSuggestions,
                             false /* typedWordValid */,
                             false /* hasAutoCorrectionCandidate */,
                             false /* isPunctuationSuggestions */,
                             true /* isObsoleteSuggestions */,
                             false /* isPrediction */);
         }
     }
 
     private void showSuggestionStrip(final SuggestedWords suggestedWords,
             final CharSequence typedWord) {
         if (null == suggestedWords || suggestedWords.size() <= 0) {
             clearSuggestionStrip();
             return;
         }
         final CharSequence autoCorrection;
         if (suggestedWords.size() > 0) {
             if (suggestedWords.mWillAutoCorrect) {
                 autoCorrection = suggestedWords.getWord(1);
             } else {
                 autoCorrection = typedWord;
             }
         } else {
             autoCorrection = null;
         }
         mWordComposer.setAutoCorrection(autoCorrection);
         final boolean isAutoCorrection = suggestedWords.willAutoCorrect();
         setSuggestionStrip(suggestedWords, isAutoCorrection);
         setAutoCorrectionIndicator(isAutoCorrection);
         setSuggestionStripShown(isSuggestionsStripVisible());
     }
 
     private void commitCurrentAutoCorrection(final int separatorCodePoint) {
         // Complete any pending suggestions query first
         if (mHandler.hasPendingUpdateSuggestions()) {
             updateSuggestionStrip();
         }
         final CharSequence typedAutoCorrection = mWordComposer.getAutoCorrectionOrNull();
         final String typedWord = mWordComposer.getTypedWord();
         final CharSequence autoCorrection = (typedAutoCorrection != null)
                 ? typedAutoCorrection : typedWord;
         if (autoCorrection != null) {
             if (TextUtils.isEmpty(typedWord)) {
                 throw new RuntimeException("We have an auto-correction but the typed word "
                         + "is empty? Impossible! I must commit suicide.");
             }
             Utils.Stats.onAutoCorrection(typedWord, autoCorrection.toString(), separatorCodePoint);
             if (ProductionFlag.IS_EXPERIMENTAL) {
                 ResearchLogger.latinIME_commitCurrentAutoCorrection(typedWord,
                         autoCorrection.toString());
             }
             mExpectingUpdateSelection = true;
             commitChosenWord(autoCorrection, LastComposedWord.COMMIT_TYPE_DECIDED_WORD,
                     separatorCodePoint);
             if (!typedWord.equals(autoCorrection)) {
                 // This will make the correction flash for a short while as a visual clue
                 // to the user that auto-correction happened.
                 mConnection.commitCorrection(
                         new CorrectionInfo(mLastSelectionEnd - typedWord.length(),
                         typedWord, autoCorrection));
             }
         }
     }
 
     // Called from {@link SuggestionStripView} through the {@link SuggestionStripView#Listener}
     // interface
     @Override
     public void pickSuggestionManually(final int index, final CharSequence suggestion,
             final int x, final int y) {
         final SuggestedWords suggestedWords = mSuggestionStripView.getSuggestions();
         // If this is a punctuation picked from the suggestion strip, pass it to onCodeInput
         if (suggestion.length() == 1 && isShowingPunctuationList()) {
             // Word separators are suggested before the user inputs something.
             // So, LatinImeLogger logs "" as a user's input.
             LatinImeLogger.logOnManualSuggestion("", suggestion.toString(), index, suggestedWords);
             // Rely on onCodeInput to do the complicated swapping/stripping logic consistently.
             if (ProductionFlag.IS_EXPERIMENTAL) {
                 ResearchLogger.latinIME_punctuationSuggestion(index, suggestion, x, y);
             }
             final int primaryCode = suggestion.charAt(0);
             onCodeInput(primaryCode,
                     KeyboardActionListener.SUGGESTION_STRIP_COORDINATE,
                     KeyboardActionListener.SUGGESTION_STRIP_COORDINATE);
             return;
         }
 
         mConnection.beginBatchEdit();
         if (SPACE_STATE_PHANTOM == mSpaceState && suggestion.length() > 0
                 // In the batch input mode, a manually picked suggested word should just replace
                 // the current batch input text and there is no need for a phantom space.
                 && !mWordComposer.isBatchMode()) {
             int firstChar = Character.codePointAt(suggestion, 0);
             if ((!mCurrentSettings.isWeakSpaceStripper(firstChar))
                     && (!mCurrentSettings.isWeakSpaceSwapper(firstChar))) {
                 sendKeyCodePoint(Keyboard.CODE_SPACE);
             }
         }
 
         if (mCurrentSettings.isApplicationSpecifiedCompletionsOn()
                 && mApplicationSpecifiedCompletions != null
                 && index >= 0 && index < mApplicationSpecifiedCompletions.length) {
             if (mSuggestionStripView != null) {
                 mSuggestionStripView.clear();
             }
             mKeyboardSwitcher.updateShiftState();
             resetComposingState(true /* alsoResetLastComposedWord */);
             final CompletionInfo completionInfo = mApplicationSpecifiedCompletions[index];
             mConnection.commitCompletion(completionInfo);
             mConnection.endBatchEdit();
             if (ProductionFlag.IS_EXPERIMENTAL) {
                 ResearchLogger.latinIME_pickApplicationSpecifiedCompletion(index,
                         completionInfo.getText(), x, y);
             }
             return;
         }
 
         // We need to log before we commit, because the word composer will store away the user
         // typed word.
         final String replacedWord = mWordComposer.getTypedWord().toString();
         LatinImeLogger.logOnManualSuggestion(replacedWord,
                 suggestion.toString(), index, suggestedWords);
         if (ProductionFlag.IS_EXPERIMENTAL) {
             ResearchLogger.latinIME_pickSuggestionManually(replacedWord, index, suggestion, x, y);
         }
         mExpectingUpdateSelection = true;
         commitChosenWord(suggestion, LastComposedWord.COMMIT_TYPE_MANUAL_PICK,
                 LastComposedWord.NOT_A_SEPARATOR);
         mConnection.endBatchEdit();
         // Don't allow cancellation of manual pick
         mLastComposedWord.deactivate();
         mSpaceState = SPACE_STATE_PHANTOM;
         // TODO: is this necessary?
         mKeyboardSwitcher.updateShiftState();
 
         // We should show the "Touch again to save" hint if the user pressed the first entry
         // AND it's in none of our current dictionaries (main, user or otherwise).
         // Please note that if mSuggest is null, it means that everything is off: suggestion
         // and correction, so we shouldn't try to show the hint
         final boolean showingAddToDictionaryHint = index == 0 && mSuggest != null
                 // If the suggestion is not in the dictionary, the hint should be shown.
                 && !AutoCorrection.isValidWord(mSuggest.getUnigramDictionaries(), suggestion, true);
 
         Utils.Stats.onSeparator((char)Keyboard.CODE_SPACE, WordComposer.NOT_A_COORDINATE,
                 WordComposer.NOT_A_COORDINATE);
         if (showingAddToDictionaryHint && mIsUserDictionaryAvailable) {
             mSuggestionStripView.showAddToDictionaryHint(
                     suggestion, mCurrentSettings.mHintToSaveText);
         } else {
             // If we're not showing the "Touch again to save", then update the suggestion strip.
             mHandler.postUpdateSuggestionStrip();
         }
     }
 
     /**
      * Commits the chosen word to the text field and saves it for later retrieval.
      */
     private void commitChosenWord(final CharSequence chosenWord, final int commitType,
             final int separatorCode) {
         final SuggestedWords suggestedWords = mSuggestionStripView.getSuggestions();
         mConnection.commitText(SuggestionSpanUtils.getTextWithSuggestionSpan(
                 this, chosenWord, suggestedWords, mIsMainDictionaryAvailable), 1);
         if (ProductionFlag.IS_EXPERIMENTAL) {
             ResearchLogger.latinIME_commitText(chosenWord);
         }
         // Add the word to the user history dictionary
         final CharSequence prevWord = addToUserHistoryDictionary(chosenWord);
         // TODO: figure out here if this is an auto-correct or if the best word is actually
         // what user typed. Note: currently this is done much later in
         // LastComposedWord#didCommitTypedWord by string equality of the remembered
         // strings.
         mLastComposedWord = mWordComposer.commitWord(commitType, chosenWord.toString(),
                 separatorCode, prevWord);
     }
 
     private void setPunctuationSuggestions() {
         if (mCurrentSettings.mBigramPredictionEnabled) {
             clearSuggestionStrip();
         } else {
             setSuggestionStrip(mCurrentSettings.mSuggestPuncList, false);
         }
         setAutoCorrectionIndicator(false);
         setSuggestionStripShown(isSuggestionsStripVisible());
     }
 
     private CharSequence addToUserHistoryDictionary(final CharSequence suggestion) {
         if (TextUtils.isEmpty(suggestion)) return null;
 
         // If correction is not enabled, we don't add words to the user history dictionary.
         // That's to avoid unintended additions in some sensitive fields, or fields that
         // expect to receive non-words.
         if (!mCurrentSettings.mCorrectionEnabled) return null;
 
         final UserHistoryDictionary userHistoryDictionary = mUserHistoryDictionary;
         if (userHistoryDictionary != null) {
             final CharSequence prevWord
                     = mConnection.getNthPreviousWord(mCurrentSettings.mWordSeparators, 2);
             final String secondWord;
             if (mWordComposer.isAutoCapitalized() && !mWordComposer.isMostlyCaps()) {
                 secondWord = suggestion.toString().toLowerCase(
                         mSubtypeSwitcher.getCurrentSubtypeLocale());
             } else {
                 secondWord = suggestion.toString();
             }
             // We demote unrecognized words (frequency < 0, below) by specifying them as "invalid".
             // We don't add words with 0-frequency (assuming they would be profanity etc.).
             final int maxFreq = AutoCorrection.getMaxFrequency(
                     mSuggest.getUnigramDictionaries(), suggestion);
             if (maxFreq == 0) return null;
             userHistoryDictionary.addToUserHistory(null == prevWord ? null : prevWord.toString(),
                     secondWord, maxFreq > 0);
             return prevWord;
         }
         return null;
     }
 
     /**
      * Check if the cursor is actually at the end of a word. If so, restart suggestions on this
      * word, else do nothing.
      */
     private void restartSuggestionsOnWordBeforeCursorIfAtEndOfWord() {
         final CharSequence word = mConnection.getWordBeforeCursorIfAtEndOfWord(mCurrentSettings);
         if (null != word) {
             restartSuggestionsOnWordBeforeCursor(word);
         }
     }
 
     private void restartSuggestionsOnWordBeforeCursor(final CharSequence word) {
         mWordComposer.setComposingWord(word, mKeyboardSwitcher.getKeyboard());
         final int length = word.length();
         mConnection.deleteSurroundingText(length, 0);
         if (ProductionFlag.IS_EXPERIMENTAL) {
             ResearchLogger.latinIME_deleteSurroundingText(length);
         }
         mConnection.setComposingText(word, 1);
         mHandler.postUpdateSuggestionStrip();
     }
 
     private void revertCommit() {
         final CharSequence previousWord = mLastComposedWord.mPrevWord;
         final String originallyTypedWord = mLastComposedWord.mTypedWord;
         final CharSequence committedWord = mLastComposedWord.mCommittedWord;
         final int cancelLength = committedWord.length();
         final int separatorLength = LastComposedWord.getSeparatorLength(
                 mLastComposedWord.mSeparatorCode);
         // TODO: should we check our saved separator against the actual contents of the text view?
         final int deleteLength = cancelLength + separatorLength;
         if (DEBUG) {
             if (mWordComposer.isComposingWord()) {
                 throw new RuntimeException("revertCommit, but we are composing a word");
             }
             final String wordBeforeCursor =
                     mConnection.getTextBeforeCursor(deleteLength, 0)
                             .subSequence(0, cancelLength).toString();
             if (!TextUtils.equals(committedWord, wordBeforeCursor)) {
                 throw new RuntimeException("revertCommit check failed: we thought we were "
                         + "reverting \"" + committedWord
                         + "\", but before the cursor we found \"" + wordBeforeCursor + "\"");
             }
         }
         mConnection.deleteSurroundingText(deleteLength, 0);
         if (ProductionFlag.IS_EXPERIMENTAL) {
             ResearchLogger.latinIME_deleteSurroundingText(deleteLength);
         }
         if (!TextUtils.isEmpty(previousWord) && !TextUtils.isEmpty(committedWord)) {
             mUserHistoryDictionary.cancelAddingUserHistory(
                     previousWord.toString(), committedWord.toString());
         }
         mConnection.commitText(originallyTypedWord, 1);
         // Re-insert the separator
         sendKeyCodePoint(mLastComposedWord.mSeparatorCode);
         Utils.Stats.onSeparator(mLastComposedWord.mSeparatorCode, WordComposer.NOT_A_COORDINATE,
                 WordComposer.NOT_A_COORDINATE);
         if (ProductionFlag.IS_EXPERIMENTAL) {
             ResearchLogger.latinIME_revertCommit(originallyTypedWord);
         }
         // Don't restart suggestion yet. We'll restart if the user deletes the
         // separator.
         mLastComposedWord = LastComposedWord.NOT_A_COMPOSED_WORD;
         // We have a separator between the word and the cursor: we should show predictions.
         mHandler.postUpdateSuggestionStrip();
     }
 
     // Used by the RingCharBuffer
     public boolean isWordSeparator(int code) {
         return mCurrentSettings.isWordSeparator(code);
     }
 
     // Notify that language or mode have been changed and toggleLanguage will update KeyboardID
     // according to new language or mode. Called from SubtypeSwitcher.
     public void onRefreshKeyboard() {
         // When the device locale is changed in SetupWizard etc., this method may get called via
         // onConfigurationChanged before SoftInputWindow is shown.
         initSuggest();
         loadSettings();
         if (mKeyboardSwitcher.getKeyboardView() != null) {
             // Reload keyboard because the current language has been changed.
             mKeyboardSwitcher.loadKeyboard(getCurrentInputEditorInfo(), mCurrentSettings);
             updateKeyboardViewGestureHandlingModeByMainDictionaryAvailability();
         }
         // Since we just changed languages, we should re-evaluate suggestions with whatever word
         // we are currently composing. If we are not composing anything, we may want to display
         // predictions or punctuation signs (which is done by the updateSuggestionStrip anyway).
         mHandler.postUpdateSuggestionStrip();
     }
 
     private void updateKeyboardViewGestureHandlingModeByMainDictionaryAvailability() {
         final MainKeyboardView keyboardView = mKeyboardSwitcher.getKeyboardView();
         if (keyboardView != null) {
             final boolean shouldHandleGesture = mCurrentSettings.mGestureInputEnabled
                     && mIsMainDictionaryAvailable;
             keyboardView.setGestureHandlingMode(shouldHandleGesture,
                     mCurrentSettings.mGesturePreviewTrailEnabled,
                     mCurrentSettings.mGestureFloatingPreviewTextEnabled);
         }
     }
 
     // TODO: Remove this method from {@link LatinIME} and move {@link FeedbackManager} to
     // {@link KeyboardSwitcher}. Called from KeyboardSwitcher
     public void hapticAndAudioFeedback(final int primaryCode) {
         mFeedbackManager.hapticAndAudioFeedback(primaryCode, mKeyboardSwitcher.getKeyboardView());
     }
 
     // Callback called by PointerTracker through the KeyboardActionListener. This is called when a
     // key is depressed; release matching call is onReleaseKey below.
     @Override
     public void onPressKey(int primaryCode) {
         mKeyboardSwitcher.onPressKey(primaryCode);
     }
 
     // Callback by PointerTracker through the KeyboardActionListener. This is called when a key
     // is released; press matching call is onPressKey above.
     @Override
     public void onReleaseKey(int primaryCode, boolean withSliding) {
         mKeyboardSwitcher.onReleaseKey(primaryCode, withSliding);
 
         // If accessibility is on, ensure the user receives keyboard state updates.
         if (AccessibilityUtils.getInstance().isTouchExplorationEnabled()) {
             switch (primaryCode) {
             case Keyboard.CODE_SHIFT:
                 AccessibleKeyboardViewProxy.getInstance().notifyShiftState();
                 break;
             case Keyboard.CODE_SWITCH_ALPHA_SYMBOL:
                 AccessibleKeyboardViewProxy.getInstance().notifySymbolsState();
                 break;
             }
         }
 
         if (Keyboard.CODE_DELETE == primaryCode) {
             // This is a stopgap solution to avoid leaving a high surrogate alone in a text view.
             // In the future, we need to deprecate deteleSurroundingText() and have a surrogate
             // pair-friendly way of deleting characters in InputConnection.
             final CharSequence lastChar = mConnection.getTextBeforeCursor(1, 0);
             if (!TextUtils.isEmpty(lastChar) && Character.isHighSurrogate(lastChar.charAt(0))) {
                 mConnection.deleteSurroundingText(1, 0);
             }
         }
     }
 
     // receive ringer mode change and network state change.
     private BroadcastReceiver mReceiver = new BroadcastReceiver() {
         @Override
         public void onReceive(Context context, Intent intent) {
             final String action = intent.getAction();
             if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                 mSubtypeSwitcher.onNetworkStateChanged(intent);
             } else if (action.equals(AudioManager.RINGER_MODE_CHANGED_ACTION)) {
                 mFeedbackManager.onRingerModeChanged();
             }
         }
     };
 
     private void launchSettings() {
         handleClose();
         launchSubActivity(SettingsActivity.class);
     }
 
     // Called from debug code only
     public void launchDebugSettings() {
         handleClose();
         launchSubActivity(DebugSettingsActivity.class);
     }
 
     public void launchKeyboardedDialogActivity(Class<? extends Activity> activityClass) {
         // Put the text in the attached EditText into a safe, saved state before switching to a
         // new activity that will also use the soft keyboard.
         commitTyped(LastComposedWord.NOT_A_SEPARATOR);
         launchSubActivity(activityClass);
     }
 
     private void launchSubActivity(Class<? extends Activity> activityClass) {
         Intent intent = new Intent();
         intent.setClass(LatinIME.this, activityClass);
         intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
         startActivity(intent);
     }
 
     private void showSubtypeSelectorAndSettings() {
         final CharSequence title = getString(R.string.english_ime_input_options);
         final CharSequence[] items = new CharSequence[] {
                 // TODO: Should use new string "Select active input modes".
                 getString(R.string.language_selection_title),
                 getString(R.string.english_ime_settings),
         };
         final Context context = this;
         final DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
             @Override
             public void onClick(DialogInterface di, int position) {
                 di.dismiss();
                 switch (position) {
                 case 0:
                     Intent intent = CompatUtils.getInputLanguageSelectionIntent(
                             ImfUtils.getInputMethodIdOfThisIme(context),
                             Intent.FLAG_ACTIVITY_NEW_TASK
                             | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                             | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                     startActivity(intent);
                     break;
                 case 1:
                     launchSettings();
                     break;
                 }
             }
         };
         final AlertDialog.Builder builder = new AlertDialog.Builder(this)
                 .setItems(items, listener)
                 .setTitle(title);
         showOptionDialog(builder.create());
     }
 
     public void showOptionDialog(AlertDialog dialog) {
         final IBinder windowToken = mKeyboardSwitcher.getKeyboardView().getWindowToken();
         if (windowToken == null) return;
 
         dialog.setCancelable(true);
         dialog.setCanceledOnTouchOutside(true);
 
         final Window window = dialog.getWindow();
         final WindowManager.LayoutParams lp = window.getAttributes();
         lp.token = windowToken;
         lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
         window.setAttributes(lp);
         window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
 
         mOptionsDialog = dialog;
         dialog.show();
     }
 
     @Override
     protected void dump(FileDescriptor fd, PrintWriter fout, String[] args) {
         super.dump(fd, fout, args);
 
         final Printer p = new PrintWriterPrinter(fout);
         p.println("LatinIME state :");
         final Keyboard keyboard = mKeyboardSwitcher.getKeyboard();
         final int keyboardMode = keyboard != null ? keyboard.mId.mMode : -1;
         p.println("  Keyboard mode = " + keyboardMode);
         p.println("  mIsSuggestionsSuggestionsRequested = "
                 + mCurrentSettings.isSuggestionsRequested(mDisplayOrientation));
         p.println("  mCorrectionEnabled=" + mCurrentSettings.mCorrectionEnabled);
         p.println("  isComposingWord=" + mWordComposer.isComposingWord());
         p.println("  mSoundOn=" + mCurrentSettings.mSoundOn);
         p.println("  mVibrateOn=" + mCurrentSettings.mVibrateOn);
         p.println("  mKeyPreviewPopupOn=" + mCurrentSettings.mKeyPreviewPopupOn);
         p.println("  inputAttributes=" + mCurrentSettings.getInputAttributesDebugString());
     }
 }
