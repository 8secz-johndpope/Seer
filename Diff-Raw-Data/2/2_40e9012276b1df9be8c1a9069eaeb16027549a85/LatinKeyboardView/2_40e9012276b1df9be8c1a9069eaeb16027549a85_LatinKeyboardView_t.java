 /*
  * Copyright (C) 2011 The Android Open Source Project
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
 
 package com.android.inputmethod.keyboard;
 
 import android.content.Context;
 import android.content.pm.PackageManager;
 import android.content.res.TypedArray;
 import android.graphics.Canvas;
 import android.graphics.Color;
 import android.graphics.Paint;
 import android.graphics.Paint.Align;
 import android.graphics.Typeface;
 import android.graphics.drawable.Drawable;
 import android.os.Message;
 import android.text.TextUtils;
 import android.util.AttributeSet;
 import android.util.Log;
 import android.view.LayoutInflater;
 import android.view.MotionEvent;
 import android.view.View;
 import android.view.ViewConfiguration;
 import android.view.ViewGroup;
 import android.view.accessibility.AccessibilityEvent;
 import android.widget.PopupWindow;
 
 import com.android.inputmethod.accessibility.AccessibilityUtils;
 import com.android.inputmethod.accessibility.AccessibleKeyboardViewProxy;
 import com.android.inputmethod.deprecated.VoiceProxy;
 import com.android.inputmethod.keyboard.PointerTracker.DrawingProxy;
 import com.android.inputmethod.keyboard.PointerTracker.TimerProxy;
 import com.android.inputmethod.latin.LatinIME;
 import com.android.inputmethod.latin.LatinImeLogger;
 import com.android.inputmethod.latin.R;
 import com.android.inputmethod.latin.StaticInnerHandlerWrapper;
 import com.android.inputmethod.latin.Utils;
 import com.android.inputmethod.latin.Utils.UsabilityStudyLogUtils;
 
 import java.util.Locale;
 import java.util.WeakHashMap;
 
 /**
  * A view that is responsible for detecting key presses and touch movements.
  *
  * @attr ref R.styleable#KeyboardView_keyHysteresisDistance
  * @attr ref R.styleable#KeyboardView_verticalCorrection
  * @attr ref R.styleable#KeyboardView_popupLayout
  */
 public class LatinKeyboardView extends KeyboardView implements PointerTracker.KeyEventHandler,
         SuddenJumpingTouchEventHandler.ProcessMotionEvent {
     private static final String TAG = LatinKeyboardView.class.getSimpleName();
 
     // TODO: Kill process when the usability study mode was changed.
     private static final boolean ENABLE_USABILITY_STUDY_LOG = LatinImeLogger.sUsabilityStudy;
 
     /** Listener for {@link KeyboardActionListener}. */
     private KeyboardActionListener mKeyboardActionListener;
 
     /* Space key and its icons */
     private Key mSpaceKey;
     private Drawable mSpaceIcon;
     // Stuff to draw language name on spacebar.
     private boolean mNeedsToDisplayLanguage;
     private Locale mSpacebarLocale;
     private float mSpacebarTextFadeFactor = 0.0f;
     private final float mSpacebarTextRatio;
     private float mSpacebarTextSize;
     private final int mSpacebarTextColor;
     private final int mSpacebarTextShadowColor;
     // Height in space key the language name will be drawn. (proportional to space key height)
     private static final float SPACEBAR_LANGUAGE_BASELINE = 0.6f;
     // If the full language name needs to be smaller than this value to be drawn on space key,
     // its short language name will be used instead.
     private static final float MINIMUM_SCALE_OF_LANGUAGE_NAME = 0.8f;
     // Stuff to draw auto correction LED on spacebar.
     private boolean mAutoCorrectionSpacebarLedOn;
     private final boolean mAutoCorrectionSpacebarLedEnabled;
     private final Drawable mAutoCorrectionSpacebarLedIcon;
     private static final int SPACE_LED_LENGTH_PERCENT = 80;
 
     // Mini keyboard
     private PopupWindow mMoreKeysWindow;
     private MoreKeysPanel mMoreKeysPanel;
     private int mMoreKeysPanelPointerTrackerId;
     private final WeakHashMap<Key, MoreKeysPanel> mMoreKeysPanelCache =
             new WeakHashMap<Key, MoreKeysPanel>();
     private final boolean mConfigShowMiniKeyboardAtTouchedPoint;
 
     private final PointerTrackerParams mPointerTrackerParams;
     private final boolean mIsSpacebarTriggeringPopupByLongPress;
     private final SuddenJumpingTouchEventHandler mTouchScreenRegulator;
 
     protected KeyDetector mKeyDetector;
     private boolean mHasDistinctMultitouch;
     private int mOldPointerCount = 1;
     private Key mOldKey;
 
     private final KeyTimerHandler mKeyTimerHandler;
 
     private static class KeyTimerHandler extends StaticInnerHandlerWrapper<LatinKeyboardView>
             implements TimerProxy {
         private static final int MSG_REPEAT_KEY = 1;
         private static final int MSG_LONGPRESS_KEY = 2;
         private static final int MSG_DOUBLE_TAP = 3;
         private static final int MSG_KEY_TYPED = 4;
 
         private final KeyTimerParams mParams;
         private boolean mInKeyRepeat;
 
         public KeyTimerHandler(LatinKeyboardView outerInstance, KeyTimerParams params) {
             super(outerInstance);
             mParams = params;
         }
 
         @Override
         public void handleMessage(Message msg) {
             final LatinKeyboardView keyboardView = getOuterInstance();
             final PointerTracker tracker = (PointerTracker) msg.obj;
             switch (msg.what) {
             case MSG_REPEAT_KEY:
                 tracker.onRepeatKey(tracker.getKey());
                 startKeyRepeatTimer(tracker);
                 break;
             case MSG_LONGPRESS_KEY:
                 if (tracker != null) {
                     keyboardView.openMiniKeyboardIfRequired(tracker.getKey(), tracker);
                 } else {
                     KeyboardSwitcher.getInstance().onLongPressTimeout(msg.arg1);
                 }
                 break;
             }
         }
 
         @Override
         public void startKeyRepeatTimer(PointerTracker tracker) {
             mInKeyRepeat = true;
             sendMessageDelayed(obtainMessage(MSG_REPEAT_KEY, tracker),
                     mParams.mKeyRepeatStartTimeout);
         }
 
         public void cancelKeyRepeatTimer() {
             mInKeyRepeat = false;
             removeMessages(MSG_REPEAT_KEY);
         }
 
         public boolean isInKeyRepeat() {
             return mInKeyRepeat;
         }
 
         @Override
         public void startLongPressTimer(int code) {
             cancelLongPressTimer();
             final int delay;
             switch (code) {
             case Keyboard.CODE_SHIFT:
                delay = mParams.mLongPressShiftKeyTimeout;
                 break;
             default:
                 delay = 0;
                 break;
             }
             if (delay > 0) {
                 sendMessageDelayed(obtainMessage(MSG_LONGPRESS_KEY, code, 0), delay);
             }
         }
 
         @Override
         public void startLongPressTimer(PointerTracker tracker) {
             cancelLongPressTimer();
             if (tracker != null) {
                 final Key key = tracker.getKey();
                 final int delay;
                 switch (key.mCode) {
                 case Keyboard.CODE_SHIFT:
                     delay = mParams.mLongPressShiftKeyTimeout;
                     break;
                 case Keyboard.CODE_SPACE:
                     delay = mParams.mLongPressSpaceKeyTimeout;
                     break;
                 default:
                     if (KeyboardSwitcher.getInstance().isInMomentarySwitchState()) {
                         // We use longer timeout for sliding finger input started from the symbols
                         // mode key.
                         delay = mParams.mLongPressKeyTimeout * 3;
                     } else {
                         delay = mParams.mLongPressKeyTimeout;
                     }
                     break;
                 }
                 if (delay > 0) {
                     sendMessageDelayed(obtainMessage(MSG_LONGPRESS_KEY, tracker), delay);
                 }
             }
         }
 
         @Override
         public void cancelLongPressTimer() {
             removeMessages(MSG_LONGPRESS_KEY);
         }
 
         @Override
         public void startKeyTypedTimer() {
             removeMessages(MSG_KEY_TYPED);
             sendMessageDelayed(obtainMessage(MSG_KEY_TYPED), mParams.mIgnoreSpecialKeyTimeout);
         }
 
         @Override
         public boolean isTyping() {
             return hasMessages(MSG_KEY_TYPED);
         }
 
         @Override
         public void startDoubleTapTimer() {
             sendMessageDelayed(obtainMessage(MSG_DOUBLE_TAP),
                     ViewConfiguration.getDoubleTapTimeout());
         }
 
         @Override
         public boolean isInDoubleTapTimeout() {
             return hasMessages(MSG_DOUBLE_TAP);
         }
 
         @Override
         public void cancelKeyTimers() {
             cancelKeyRepeatTimer();
             cancelLongPressTimer();
         }
 
         public void cancelAllMessages() {
             cancelKeyTimers();
         }
     }
 
     public static class PointerTrackerParams {
         public final boolean mSlidingKeyInputEnabled;
         public final int mTouchNoiseThresholdTime;
         public final float mTouchNoiseThresholdDistance;
 
         public static final PointerTrackerParams DEFAULT = new PointerTrackerParams();
 
         private PointerTrackerParams() {
             mSlidingKeyInputEnabled = false;
             mTouchNoiseThresholdTime =0;
             mTouchNoiseThresholdDistance = 0;
         }
 
         public PointerTrackerParams(TypedArray latinKeyboardViewAttr) {
             mSlidingKeyInputEnabled = latinKeyboardViewAttr.getBoolean(
                     R.styleable.LatinKeyboardView_slidingKeyInputEnable, false);
             mTouchNoiseThresholdTime = latinKeyboardViewAttr.getInt(
                     R.styleable.LatinKeyboardView_touchNoiseThresholdTime, 0);
             mTouchNoiseThresholdDistance = latinKeyboardViewAttr.getDimension(
                     R.styleable.LatinKeyboardView_touchNoiseThresholdDistance, 0);
         }
     }
 
     static class KeyTimerParams {
         public final int mKeyRepeatStartTimeout;
         public final int mKeyRepeatInterval;
         public final int mLongPressKeyTimeout;
         public final int mLongPressShiftKeyTimeout;
         public final int mLongPressSpaceKeyTimeout;
         public final int mIgnoreSpecialKeyTimeout;
 
         KeyTimerParams() {
             mKeyRepeatStartTimeout = 0;
             mKeyRepeatInterval = 0;
             mLongPressKeyTimeout = 0;
             mLongPressShiftKeyTimeout = 0;
             mLongPressSpaceKeyTimeout = 0;
             mIgnoreSpecialKeyTimeout = 0;
         }
 
         public KeyTimerParams(TypedArray latinKeyboardViewAttr) {
             mKeyRepeatStartTimeout = latinKeyboardViewAttr.getInt(
                     R.styleable.LatinKeyboardView_keyRepeatStartTimeout, 0);
             mKeyRepeatInterval = latinKeyboardViewAttr.getInt(
                     R.styleable.LatinKeyboardView_keyRepeatInterval, 0);
             mLongPressKeyTimeout = latinKeyboardViewAttr.getInt(
                     R.styleable.LatinKeyboardView_longPressKeyTimeout, 0);
             mLongPressShiftKeyTimeout = latinKeyboardViewAttr.getInt(
                     R.styleable.LatinKeyboardView_longPressShiftKeyTimeout, 0);
             mLongPressSpaceKeyTimeout = latinKeyboardViewAttr.getInt(
                     R.styleable.LatinKeyboardView_longPressSpaceKeyTimeout, 0);
             mIgnoreSpecialKeyTimeout = latinKeyboardViewAttr.getInt(
                     R.styleable.LatinKeyboardView_ignoreSpecialKeyTimeout, 0);
         }
     }
 
     public LatinKeyboardView(Context context, AttributeSet attrs) {
         this(context, attrs, R.attr.latinKeyboardViewStyle);
     }
 
     public LatinKeyboardView(Context context, AttributeSet attrs, int defStyle) {
         super(context, attrs, defStyle);
 
         mTouchScreenRegulator = new SuddenJumpingTouchEventHandler(getContext(), this);
 
         mHasDistinctMultitouch = context.getPackageManager()
                 .hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH_DISTINCT);
 
         PointerTracker.init(mHasDistinctMultitouch);
 
         final TypedArray a = context.obtainStyledAttributes(
                 attrs, R.styleable.LatinKeyboardView, defStyle, R.style.LatinKeyboardView);
         mAutoCorrectionSpacebarLedEnabled = a.getBoolean(
                 R.styleable.LatinKeyboardView_autoCorrectionSpacebarLedEnabled, false);
         mAutoCorrectionSpacebarLedIcon = a.getDrawable(
                 R.styleable.LatinKeyboardView_autoCorrectionSpacebarLedIcon);
         mSpacebarTextRatio = a.getFraction(R.styleable.LatinKeyboardView_spacebarTextRatio,
                 1000, 1000, 1) / 1000.0f;
         mSpacebarTextColor = a.getColor(R.styleable.LatinKeyboardView_spacebarTextColor, 0);
         mSpacebarTextShadowColor = a.getColor(
                 R.styleable.LatinKeyboardView_spacebarTextShadowColor, 0);
 
         final KeyTimerParams keyTimerParams = new KeyTimerParams(a);
         mPointerTrackerParams = new PointerTrackerParams(a);
         mIsSpacebarTriggeringPopupByLongPress = (keyTimerParams.mLongPressSpaceKeyTimeout > 0);
 
         final float keyHysteresisDistance = a.getDimension(
                 R.styleable.LatinKeyboardView_keyHysteresisDistance, 0);
         mKeyDetector = new KeyDetector(keyHysteresisDistance);
         mKeyTimerHandler = new KeyTimerHandler(this, keyTimerParams);
         mConfigShowMiniKeyboardAtTouchedPoint = a.getBoolean(
                 R.styleable.LatinKeyboardView_showMiniKeyboardAtTouchedPoint, false);
         a.recycle();
 
         PointerTracker.setParameters(mPointerTrackerParams);
     }
 
     public void setKeyboardActionListener(KeyboardActionListener listener) {
         mKeyboardActionListener = listener;
         PointerTracker.setKeyboardActionListener(listener);
     }
 
     /**
      * Returns the {@link KeyboardActionListener} object.
      * @return the listener attached to this keyboard
      */
     @Override
     public KeyboardActionListener getKeyboardActionListener() {
         return mKeyboardActionListener;
     }
 
     @Override
     public KeyDetector getKeyDetector() {
         return mKeyDetector;
     }
 
     @Override
     public DrawingProxy getDrawingProxy() {
         return this;
     }
 
     @Override
     public TimerProxy getTimerProxy() {
         return mKeyTimerHandler;
     }
 
     /**
      * Attaches a keyboard to this view. The keyboard can be switched at any time and the
      * view will re-layout itself to accommodate the keyboard.
      * @see Keyboard
      * @see #getKeyboard()
      * @param keyboard the keyboard to display in this view
      */
     @Override
     public void setKeyboard(Keyboard keyboard) {
         // Remove any pending messages, except dismissing preview
         mKeyTimerHandler.cancelKeyTimers();
         super.setKeyboard(keyboard);
         mKeyDetector.setKeyboard(
                 keyboard, -getPaddingLeft(), -getPaddingTop() + mVerticalCorrection);
         mKeyDetector.setProximityThreshold(keyboard.mMostCommonKeyWidth);
         PointerTracker.setKeyDetector(mKeyDetector);
         mTouchScreenRegulator.setKeyboard(keyboard);
         mMoreKeysPanelCache.clear();
 
         mSpaceKey = keyboard.getKey(Keyboard.CODE_SPACE);
         mSpaceIcon = keyboard.mIconsSet.getIconByAttrId(R.styleable.Keyboard_iconSpaceKey);
         final int keyHeight = keyboard.mMostCommonKeyHeight - keyboard.mVerticalGap;
         mSpacebarTextSize = keyHeight * mSpacebarTextRatio;
         mSpacebarLocale = keyboard.mId.mLocale;
     }
 
     /**
      * Returns whether the device has distinct multi-touch panel.
      * @return true if the device has distinct multi-touch panel.
      */
     public boolean hasDistinctMultitouch() {
         return mHasDistinctMultitouch;
     }
 
     public void setDistinctMultitouch(boolean hasDistinctMultitouch) {
         mHasDistinctMultitouch = hasDistinctMultitouch;
     }
 
     /**
      * When enabled, calls to {@link KeyboardActionListener#onCodeInput} will include key
      * codes for adjacent keys.  When disabled, only the primary key code will be
      * reported.
      * @param enabled whether or not the proximity correction is enabled
      */
     public void setProximityCorrectionEnabled(boolean enabled) {
         mKeyDetector.setProximityCorrectionEnabled(enabled);
     }
 
     /**
      * Returns true if proximity correction is enabled.
      */
     public boolean isProximityCorrectionEnabled() {
         return mKeyDetector.isProximityCorrectionEnabled();
     }
 
     @Override
     public void cancelAllMessages() {
         mKeyTimerHandler.cancelAllMessages();
         super.cancelAllMessages();
     }
 
     private boolean openMiniKeyboardIfRequired(Key parentKey, PointerTracker tracker) {
         // Check if we have a popup layout specified first.
         if (mMoreKeysLayout == 0) {
             return false;
         }
 
         // Check if we are already displaying popup panel.
         if (mMoreKeysPanel != null)
             return false;
         if (parentKey == null)
             return false;
         return onLongPress(parentKey, tracker);
     }
 
     // This default implementation returns a more keys panel.
     protected MoreKeysPanel onCreateMoreKeysPanel(Key parentKey) {
         if (parentKey.mMoreKeys == null)
             return null;
 
         final View container = LayoutInflater.from(getContext()).inflate(mMoreKeysLayout, null);
         if (container == null)
             throw new NullPointerException();
 
         final MiniKeyboardView miniKeyboardView =
                 (MiniKeyboardView)container.findViewById(R.id.mini_keyboard_view);
         final Keyboard parentKeyboard = getKeyboard();
         final Keyboard miniKeyboard = new MiniKeyboard.Builder(
                 this, parentKeyboard.mMoreKeysTemplate, parentKey, parentKeyboard).build();
         miniKeyboardView.setKeyboard(miniKeyboard);
         container.measure(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
 
         return miniKeyboardView;
     }
 
     /**
      * Called when a key is long pressed. By default this will open mini keyboard associated
      * with this key.
      * @param parentKey the key that was long pressed
      * @param tracker the pointer tracker which pressed the parent key
      * @return true if the long press is handled, false otherwise. Subclasses should call the
      * method on the base class if the subclass doesn't wish to handle the call.
      */
     protected boolean onLongPress(Key parentKey, PointerTracker tracker) {
         final int primaryCode = parentKey.mCode;
         final Keyboard keyboard = getKeyboard();
         if (primaryCode == Keyboard.CODE_DIGIT0 && keyboard.mId.isPhoneKeyboard()) {
             tracker.onLongPressed();
             // Long pressing on 0 in phone number keypad gives you a '+'.
             invokeCodeInput(Keyboard.CODE_PLUS);
             invokeReleaseKey(primaryCode);
             KeyboardSwitcher.getInstance().hapticAndAudioFeedback(primaryCode);
             return true;
         }
         if (primaryCode == Keyboard.CODE_SPACE) {
             // Long pressing the space key invokes IME switcher dialog.
             if (invokeCustomRequest(LatinIME.CODE_SHOW_INPUT_METHOD_PICKER)) {
                 tracker.onLongPressed();
                 invokeReleaseKey(primaryCode);
                 return true;
             }
         }
         return openMoreKeysPanel(parentKey, tracker);
     }
 
     private boolean invokeCustomRequest(int code) {
         return mKeyboardActionListener.onCustomRequest(code);
     }
 
     private void invokeCodeInput(int primaryCode) {
         mKeyboardActionListener.onCodeInput(primaryCode, null,
                 KeyboardActionListener.NOT_A_TOUCH_COORDINATE,
                 KeyboardActionListener.NOT_A_TOUCH_COORDINATE);
     }
 
     private void invokeReleaseKey(int primaryCode) {
         mKeyboardActionListener.onReleaseKey(primaryCode, false);
     }
 
     private boolean openMoreKeysPanel(Key parentKey, PointerTracker tracker) {
         MoreKeysPanel moreKeysPanel = mMoreKeysPanelCache.get(parentKey);
         if (moreKeysPanel == null) {
             moreKeysPanel = onCreateMoreKeysPanel(parentKey);
             if (moreKeysPanel == null)
                 return false;
             mMoreKeysPanelCache.put(parentKey, moreKeysPanel);
         }
         if (mMoreKeysWindow == null) {
             mMoreKeysWindow = new PopupWindow(getContext());
             mMoreKeysWindow.setBackgroundDrawable(null);
             mMoreKeysWindow.setAnimationStyle(R.style.MiniKeyboardAnimation);
         }
         mMoreKeysPanel = moreKeysPanel;
         mMoreKeysPanelPointerTrackerId = tracker.mPointerId;
 
         final Keyboard keyboard = getKeyboard();
         final int pointX = (mConfigShowMiniKeyboardAtTouchedPoint) ? tracker.getLastX()
                 : parentKey.mX + parentKey.mWidth / 2;
         final int pointY = parentKey.mY - keyboard.mVerticalGap;
         moreKeysPanel.showMoreKeysPanel(
                 this, this, pointX, pointY, mMoreKeysWindow, mKeyboardActionListener);
         final int translatedX = moreKeysPanel.translateX(tracker.getLastX());
         final int translatedY = moreKeysPanel.translateY(tracker.getLastY());
         tracker.onShowMoreKeysPanel(translatedX, translatedY, moreKeysPanel);
         dimEntireKeyboard(true);
         return true;
     }
 
     public boolean isInSlidingKeyInput() {
         if (mMoreKeysPanel != null) {
             return true;
         } else {
             return PointerTracker.isAnyInSlidingKeyInput();
         }
     }
 
     public int getPointerCount() {
         return mOldPointerCount;
     }
 
     @Override
     public boolean onTouchEvent(MotionEvent me) {
         if (getKeyboard() == null) {
             return false;
         }
         return mTouchScreenRegulator.onTouchEvent(me);
     }
 
     @Override
     public boolean processMotionEvent(MotionEvent me) {
         final boolean nonDistinctMultitouch = !mHasDistinctMultitouch;
         final int action = me.getActionMasked();
         final int pointerCount = me.getPointerCount();
         final int oldPointerCount = mOldPointerCount;
         mOldPointerCount = pointerCount;
 
         // TODO: cleanup this code into a multi-touch to single-touch event converter class?
         // If the device does not have distinct multi-touch support panel, ignore all multi-touch
         // events except a transition from/to single-touch.
         if (nonDistinctMultitouch && pointerCount > 1 && oldPointerCount > 1) {
             return true;
         }
 
         final long eventTime = me.getEventTime();
         final int index = me.getActionIndex();
         final int id = me.getPointerId(index);
         final int x, y;
         if (mMoreKeysPanel != null && id == mMoreKeysPanelPointerTrackerId) {
             x = mMoreKeysPanel.translateX((int)me.getX(index));
             y = mMoreKeysPanel.translateY((int)me.getY(index));
         } else {
             x = (int)me.getX(index);
             y = (int)me.getY(index);
         }
         if (ENABLE_USABILITY_STUDY_LOG) {
             final String eventTag;
             switch (action) {
                 case MotionEvent.ACTION_UP:
                     eventTag = "[Up]";
                     break;
                 case MotionEvent.ACTION_DOWN:
                     eventTag = "[Down]";
                     break;
                 case MotionEvent.ACTION_POINTER_UP:
                     eventTag = "[PointerUp]";
                     break;
                 case MotionEvent.ACTION_POINTER_DOWN:
                     eventTag = "[PointerDown]";
                     break;
                 case MotionEvent.ACTION_MOVE: // Skip this as being logged below
                     eventTag = "";
                     break;
                 default:
                     eventTag = "[Action" + action + "]";
                     break;
             }
             if (!TextUtils.isEmpty(eventTag)) {
                 UsabilityStudyLogUtils.getInstance().write(
                         eventTag + eventTime + "," + id + "," + x + "," + y + ","
                         + me.getSize(index) + "," + me.getPressure(index));
             }
         }
 
         if (mKeyTimerHandler.isInKeyRepeat()) {
             final PointerTracker tracker = PointerTracker.getPointerTracker(id, this);
             // Key repeating timer will be canceled if 2 or more keys are in action, and current
             // event (UP or DOWN) is non-modifier key.
             if (pointerCount > 1 && !tracker.isModifier()) {
                 mKeyTimerHandler.cancelKeyRepeatTimer();
             }
             // Up event will pass through.
         }
 
         // TODO: cleanup this code into a multi-touch to single-touch event converter class?
         // Translate mutli-touch event to single-touch events on the device that has no distinct
         // multi-touch panel.
         if (nonDistinctMultitouch) {
             // Use only main (id=0) pointer tracker.
             final PointerTracker tracker = PointerTracker.getPointerTracker(0, this);
             if (pointerCount == 1 && oldPointerCount == 2) {
                 // Multi-touch to single touch transition.
                 // Send a down event for the latest pointer if the key is different from the
                 // previous key.
                 final Key newKey = tracker.getKeyOn(x, y);
                 if (mOldKey != newKey) {
                     tracker.onDownEvent(x, y, eventTime, this);
                     if (action == MotionEvent.ACTION_UP)
                         tracker.onUpEvent(x, y, eventTime);
                 }
             } else if (pointerCount == 2 && oldPointerCount == 1) {
                 // Single-touch to multi-touch transition.
                 // Send an up event for the last pointer.
                 final int lastX = tracker.getLastX();
                 final int lastY = tracker.getLastY();
                 mOldKey = tracker.getKeyOn(lastX, lastY);
                 tracker.onUpEvent(lastX, lastY, eventTime);
             } else if (pointerCount == 1 && oldPointerCount == 1) {
                 tracker.processMotionEvent(action, x, y, eventTime, this);
             } else {
                 Log.w(TAG, "Unknown touch panel behavior: pointer count is " + pointerCount
                         + " (old " + oldPointerCount + ")");
             }
             return true;
         }
 
         if (action == MotionEvent.ACTION_MOVE) {
             for (int i = 0; i < pointerCount; i++) {
                 final PointerTracker tracker = PointerTracker.getPointerTracker(
                         me.getPointerId(i), this);
                 final int px, py;
                 if (mMoreKeysPanel != null
                         && tracker.mPointerId == mMoreKeysPanelPointerTrackerId) {
                     px = mMoreKeysPanel.translateX((int)me.getX(i));
                     py = mMoreKeysPanel.translateY((int)me.getY(i));
                 } else {
                     px = (int)me.getX(i);
                     py = (int)me.getY(i);
                 }
                 tracker.onMoveEvent(px, py, eventTime);
                 if (ENABLE_USABILITY_STUDY_LOG) {
                     UsabilityStudyLogUtils.getInstance().write("[Move]"  + eventTime + ","
                             + me.getPointerId(i) + "," + px + "," + py + ","
                             + me.getSize(i) + "," + me.getPressure(i));
                 }
             }
         } else {
             final PointerTracker tracker = PointerTracker.getPointerTracker(id, this);
             tracker.processMotionEvent(action, x, y, eventTime, this);
         }
 
         return true;
     }
 
     @Override
     public void closing() {
         super.closing();
         dismissMoreKeysPanel();
         mMoreKeysPanelCache.clear();
     }
 
     @Override
     public boolean dismissMoreKeysPanel() {
         if (mMoreKeysWindow != null && mMoreKeysWindow.isShowing()) {
             mMoreKeysWindow.dismiss();
             mMoreKeysPanel = null;
             mMoreKeysPanelPointerTrackerId = -1;
             dimEntireKeyboard(false);
             return true;
         }
         return false;
     }
 
     public boolean handleBack() {
         return dismissMoreKeysPanel();
     }
 
     @Override
     public void draw(Canvas c) {
         Utils.GCUtils.getInstance().reset();
         boolean tryGC = true;
         for (int i = 0; i < Utils.GCUtils.GC_TRY_LOOP_MAX && tryGC; ++i) {
             try {
                 super.draw(c);
                 tryGC = false;
             } catch (OutOfMemoryError e) {
                 tryGC = Utils.GCUtils.getInstance().tryGCOrWait("LatinKeyboardView", e);
             }
         }
     }
 
     @Override
     protected void onAttachedToWindow() {
         // Token is available from here.
         VoiceProxy.getInstance().onAttachedToWindow();
     }
 
     @Override
     public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
         if (AccessibilityUtils.getInstance().isTouchExplorationEnabled()) {
             return AccessibleKeyboardViewProxy.getInstance().dispatchPopulateAccessibilityEvent(
                     event) || super.dispatchPopulateAccessibilityEvent(event);
         }
 
         return super.dispatchPopulateAccessibilityEvent(event);
     }
 
     /**
      * Receives hover events from the input framework. This method overrides
      * View.dispatchHoverEvent(MotionEvent) on SDK version ICS or higher. On
      * lower SDK versions, this method is never called.
      *
      * @param event The motion event to be dispatched.
      * @return {@code true} if the event was handled by the view, {@code false}
      *         otherwise
      */
     public boolean dispatchHoverEvent(MotionEvent event) {
         if (AccessibilityUtils.getInstance().isTouchExplorationEnabled()) {
             final PointerTracker tracker = PointerTracker.getPointerTracker(0, this);
             return AccessibleKeyboardViewProxy.getInstance().dispatchHoverEvent(event, tracker);
         }
 
         // Reflection doesn't support calling superclass methods.
         return false;
     }
 
     public void updateShortcutKey(boolean available) {
         final Keyboard keyboard = getKeyboard();
         if (keyboard == null) return;
         final Key shortcutKey = keyboard.getKey(Keyboard.CODE_SHORTCUT);
         if (shortcutKey == null) return;
         shortcutKey.setEnabled(available);
         invalidateKey(shortcutKey);
     }
 
     public void updateSpacebar(float fadeFactor, boolean needsToDisplayLanguage) {
         mSpacebarTextFadeFactor = fadeFactor;
         mNeedsToDisplayLanguage = needsToDisplayLanguage;
         invalidateKey(mSpaceKey);
     }
 
     public void updateAutoCorrectionState(boolean isAutoCorrection) {
         if (!mAutoCorrectionSpacebarLedEnabled) return;
         mAutoCorrectionSpacebarLedOn = isAutoCorrection;
         invalidateKey(mSpaceKey);
     }
 
     @Override
     protected void onDrawKeyTopVisuals(Key key, Canvas canvas, Paint paint, KeyDrawParams params) {
         super.onDrawKeyTopVisuals(key, canvas, paint, params);
 
         if (key.mCode == Keyboard.CODE_SPACE) {
             drawSpacebar(key, canvas, paint);
 
             // Whether space key needs to show the "..." popup hint for special purposes
             if (mIsSpacebarTriggeringPopupByLongPress
                     && Utils.hasMultipleEnabledIMEsOrSubtypes(true /* include aux subtypes */)) {
                 drawKeyPopupHint(key, canvas, paint, params);
             }
         }
     }
 
     private static int getSpacebarTextColor(int color, float fadeFactor) {
         final int newColor = Color.argb((int)(Color.alpha(color) * fadeFactor),
                 Color.red(color), Color.green(color), Color.blue(color));
         return newColor;
     }
 
     // Compute width of text with specified text size using paint.
     private int getTextWidth(Paint paint, String text, float textSize) {
         paint.setTextSize(textSize);
         return (int)getLabelWidth(text, paint);
     }
 
     // Layout locale language name on spacebar.
     private String layoutLanguageOnSpacebar(Paint paint, Locale locale, int width,
             float origTextSize) {
         paint.setTextAlign(Align.CENTER);
         paint.setTypeface(Typeface.DEFAULT);
         // Estimate appropriate language name text size to fit in maxTextWidth.
         String language = Utils.getFullDisplayName(locale, true);
         int textWidth = getTextWidth(paint, language, origTextSize);
         // Assuming text width and text size are proportional to each other.
         float textSize = origTextSize * Math.min(width / textWidth, 1.0f);
         // allow variable text size
         textWidth = getTextWidth(paint, language, textSize);
         // If text size goes too small or text does not fit, use middle or short name
         final boolean useMiddleName = (textSize / origTextSize < MINIMUM_SCALE_OF_LANGUAGE_NAME)
                 || (textWidth > width);
 
         final boolean useShortName;
         if (useMiddleName) {
             language = Utils.getMiddleDisplayLanguage(locale);
             textWidth = getTextWidth(paint, language, origTextSize);
             textSize = origTextSize * Math.min(width / textWidth, 1.0f);
             useShortName = (textSize / origTextSize < MINIMUM_SCALE_OF_LANGUAGE_NAME)
                     || (textWidth > width);
         } else {
             useShortName = false;
         }
 
         if (useShortName) {
             language = Utils.getShortDisplayLanguage(locale);
             textWidth = getTextWidth(paint, language, origTextSize);
             textSize = origTextSize * Math.min(width / textWidth, 1.0f);
         }
         paint.setTextSize(textSize);
 
         return language;
     }
 
     private void drawSpacebar(Key key, Canvas canvas, Paint paint) {
         final int width = key.mWidth;
         final int height = mSpaceIcon != null ? mSpaceIcon.getIntrinsicHeight() : key.mHeight;
 
         // If application locales are explicitly selected.
         if (mNeedsToDisplayLanguage) {
             final String language = layoutLanguageOnSpacebar(paint, mSpacebarLocale, width,
                     mSpacebarTextSize);
             // Draw language text with shadow
             // In case there is no space icon, we will place the language text at the center of
             // spacebar.
             final float descent = paint.descent();
             final float textHeight = -paint.ascent() + descent;
             final float baseline = (mSpaceIcon != null) ? height * SPACEBAR_LANGUAGE_BASELINE
                     : height / 2 + textHeight / 2;
             paint.setColor(getSpacebarTextColor(mSpacebarTextShadowColor, mSpacebarTextFadeFactor));
             canvas.drawText(language, width / 2, baseline - descent - 1, paint);
             paint.setColor(getSpacebarTextColor(mSpacebarTextColor, mSpacebarTextFadeFactor));
             canvas.drawText(language, width / 2, baseline - descent, paint);
         }
 
         // Draw the spacebar icon at the bottom
         if (mAutoCorrectionSpacebarLedOn) {
             final int iconWidth = width * SPACE_LED_LENGTH_PERCENT / 100;
             final int iconHeight = mAutoCorrectionSpacebarLedIcon.getIntrinsicHeight();
             int x = (width - iconWidth) / 2;
             int y = height - iconHeight;
             drawIcon(canvas, mAutoCorrectionSpacebarLedIcon, x, y, iconWidth, iconHeight);
         } else if (mSpaceIcon != null) {
             final int iconWidth = mSpaceIcon.getIntrinsicWidth();
             final int iconHeight = mSpaceIcon.getIntrinsicHeight();
             int x = (width - iconWidth) / 2;
             int y = height - iconHeight;
             drawIcon(canvas, mSpaceIcon, x, y, iconWidth, iconHeight);
         }
     }
 }
