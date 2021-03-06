 package com.tealeaf;
 
 import android.app.Activity;
 import android.content.Context;
 import android.graphics.Color;
 import android.graphics.Paint;
 import android.graphics.Rect;
 import android.graphics.Typeface;
 import android.text.Editable;
 import android.text.InputFilter;
 import android.text.InputType;
 import android.text.TextWatcher;
 import android.util.AttributeSet;
 import android.util.TypedValue;
 import android.view.Display;
 import android.view.KeyEvent;
 import android.view.MotionEvent;
 import android.view.View;
 import android.view.ViewTreeObserver.OnGlobalLayoutListener;
 import android.view.inputmethod.EditorInfo;
 import android.view.inputmethod.EditorInfo;
 import android.view.inputmethod.InputMethodManager;
 import android.widget.AbsoluteLayout;
 import android.widget.EditText;
 import android.widget.TextView;
 import com.tealeaf.event.Event;
 import com.tealeaf.event.InputKeyboardFocusNextEvent;
 import com.tealeaf.event.InputKeyboardKeyUpEvent;
 import com.tealeaf.event.InputKeyboardSubmitEvent;
 import java.util.Map;
 import org.json.JSONObject;
 
 
 public class EditTextView extends EditText {
 
 	private static EditTextView instance;
 	private Activity activity;
 	private boolean registerTextChange = true;
 	private OnTouchListener currentTouchListener = null;
 	private OnGlobalLayoutListener onGlobalLayoutListener;
 	private boolean isOpened = false;
 
 	public enum InputName {
 		DEFAULT,
 		NUMBER,
 		PHONE,
 		PASSWORD,
 		CAPITAL
 	}
 
 	public EditTextView(Activity activity) {
 		super(activity);
 		init();
 	}
 
 	public EditTextView(Context context, AttributeSet attrs) {
 		super(context, attrs);
 		init();
 	} 
 	
 	private void init() {
 		this.setCursorVisible(true);
 		this.setTextColor(Color.BLACK);
 		this.setBackgroundColor(Color.TRANSPARENT);
 		this.setVisibility(View.GONE);
 		this.setSingleLine(true);
 	}
 
 
 	public static EditTextView Get(final Activity activity) {
 		if (instance == null && activity != null) {
 			instance = (EditTextView)activity.getLayoutInflater().inflate(R.layout.edit_text_view, null);
 			instance.activity = activity;
 
 
 			instance.addTextChangedListener(new TextWatcher() {
 				private String beforeText = "";
 
 				@Override
 				public void afterTextChanged(Editable s) {
 					// propagate text changes to JS to update views
 					if (instance.registerTextChange) {
 						logger.log("KeyUp textChange in TextEditView");
 						EventQueue.pushEvent(new InputKeyboardKeyUpEvent(s.toString(), beforeText, instance.getSelectionStart()));
 					} else {
 						instance.registerTextChange = true;
 					}
 				} 
 
 			@Override
 			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
 				beforeText = s.toString();
 			}
 
 			@Override
 			public void onTextChanged(CharSequence s, int start, int before, int count) {
 
 			}
 			});
 
 			instance.setOnEditorActionListener(new OnEditorActionListener() {
 				@Override
 				public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
 					if (actionId == EditorInfo.IME_ACTION_DONE) {
 						EventQueue.pushEvent(new InputKeyboardSubmitEvent(0, instance.getText().toString()));
 						instance.hideKeyboard();
 					} else if (actionId == EditorInfo.IME_ACTION_NEXT) {
 						EventQueue.pushEvent(new InputKeyboardFocusNextEvent(true));
 					}
 
 					return false;
 				}
 			});
 
 
 			//register for focus
 			NativeShim.RegisterCallable("editText.focus", new TeaLeafCallable() {
 				public JSONObject call(final JSONObject obj) {
 
 					activity.runOnUiThread(new Runnable() {
 						public void run() {
 							try {
 								instance.registerTextChange = false;
 								instance.setVisibility(View.VISIBLE);
 								instance.requestFocus();
 								instance.currentTouchListener = TeaLeaf.get().glView.getOnTouchListener();
 								TeaLeaf.get().glView.setOnTouchListener(instance.getScreenCaptureListener());
 
 								//set x, y, width and height
 								int x = obj.optInt("x", 0);
 								int y = obj.optInt("y", 0);
 								int width = obj.optInt("width", 0);
 								int height = obj.optInt("height", 0);
 								AbsoluteLayout.LayoutParams layoutParams = new AbsoluteLayout.LayoutParams(width, height, x, y);
 								instance.setLayoutParams(layoutParams);
 
 								//font size
 								int fontSize = (int)(obj.optInt("fontSize", 16) * .9f);
 								instance.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize);
 
 								//text
 								String text = obj.optString("text", "");
 								instance.setText(text);
 								
 								//font color
 								String fontColor = obj.optString("fontColor", "#000000");
 								instance.setTextColor(Color.parseColor(fontColor));
 
 								//font family
 								String font = obj.optString("font", "helvetica");
 								TextManager textManager = TeaLeaf.get().glView.getTextureLoader().getTextManager();
 								Typeface tf = textManager.getTypeface(font);
 								instance.setTypeface(tf);
 
 								//hint text
 								String hint = obj.optString("hint", "");
 								instance.setHint(hint);
 
 								//hint text color
 								String hintColor = obj.optString("hintColor", "#999999");
 								instance.setHintTextColor(Color.parseColor(hintColor));
 
 								//max length
 								int maxLength = obj.optInt("maxLength", -1);
 								if (maxLength == -1) {
 									instance.setFilters(new InputFilter[] {});
 								} else {
 									instance.setFilters(new InputFilter[] { new InputFilter.LengthFilter(maxLength) });
 								}
 
 								//input type
 								String inputType = obj.optString("inputType", "");
 								int type;
 								InputName inputName = InputName.DEFAULT;
 								inputName = InputName.valueOf(inputType.toUpperCase().trim());
 
 
 								boolean hasForward = obj.optBoolean("hasForward", false);
								String inputReturnButton = obj.optString("inputReturnType", "done");
 								if (inputReturnButton.equals("done")) {
 									instance.setImeOptions(EditorInfo.IME_ACTION_DONE);
 								} else if (inputReturnButton.equals("next")) {
 									instance.setImeOptions(EditorInfo.IME_ACTION_NEXT);
 								} else if (inputReturnButton.equals("search")) {
 									instance.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
 								} else if (inputReturnButton.equals("send")) {
 									instance.setImeOptions(EditorInfo.IME_ACTION_SEND);
 								} else if (inputReturnButton.equals("go")) {
 									instance.setImeOptions(EditorInfo.IME_ACTION_GO);
 								} else {
 									int action = hasForward ? EditorInfo.IME_ACTION_NEXT : EditorInfo.IME_ACTION_DONE;
 									if (inputReturnButton.equals("default")) {
 										instance.setImeOptions(action);
 									} else {
 										instance.setImeActionLabel(inputReturnButton, action);
 									}
 								}
 
 
 								//cursor pos
 								int cursorPos = obj.optInt("cursorPos", instance.length());
 								instance.setSelection(cursorPos < 0 || cursorPos > instance.length() ? instance.getText().length() : cursorPos);
 
 								switch (inputName) {
 									case NUMBER:
 										type = InputType.TYPE_CLASS_NUMBER;
 										break;
 									case PHONE:
 										type = InputType.TYPE_CLASS_PHONE;
 										break;
 									case PASSWORD:
 										type = InputType.TYPE_TEXT_VARIATION_PASSWORD;
 										break;
 									case CAPITAL:
 										type = InputType.TYPE_TEXT_FLAG_CAP_WORDS;
 										break;
 									default:
 										type = InputType.TYPE_CLASS_TEXT;
 										break;
 								}
 
 								//for auto correct use this flag -> InputType.TYPE_TEXT_FLAG_AUTO_CORRECT
 								instance.setInputType(type | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
 
 								//padding
 								int paddingLeft = obj.optInt("paddingLeft", 0);
 								int paddingRight = obj.optInt("paddingRight", 0);
 
 								instance.setPadding(paddingLeft, 0, paddingRight, 0);
 								InputMethodManager imm = (InputMethodManager) instance.activity.getSystemService(Context.INPUT_METHOD_SERVICE);
 								imm.showSoftInput(instance, 0);	
 								instance.setListenerToRootView();
 
 							} catch (Exception e) {
 								logger.log(e);
 							}
 						}
 					});
 					return obj;
 				}
 			});
 
 			//register for clear focus
 			NativeShim.RegisterCallable("editText.clearFocus", new TeaLeafCallable() {
 				public JSONObject call(final JSONObject obj) {
 
 					activity.runOnUiThread(new Runnable() {
 						public void run() {
 							TeaLeaf.get().glView.setOnTouchListener(instance.currentTouchListener);
 							instance.hideKeyboard();
 							instance.setVisibility(View.GONE);
 							instance.removeListenerToRootView();
 						}
 					});
 					return obj;
 				}
 			});
 
 			instance.onGlobalLayoutListener = new OnGlobalLayoutListener() {
 				@Override
 				public void onGlobalLayout() {
 
 					View group = (View)TeaLeaf.get().getGroup();
 					// get visible area of the view
 					Rect r = new Rect();
 					group.getWindowVisibleDisplayFrame(r);
 					
 					// get display height
 					Display display = instance.activity.getWindow().getWindowManager().getDefaultDisplay();
 					int height = display.getHeight();
 					
 					// if our visible height is less than 75% normal, assume keyboard on screen
 					int visibleHeight = r.bottom - r.top;
 
 					if (visibleHeight < .75 * height && !instance.isOpened) {
 						instance.isOpened = true;
 					} else if(instance.isOpened){
 						EventQueue.pushEvent(new Event("editText.onFinishEditing"));
 					}
 
 				}
 			};
 
 		}
 
 		return instance;
 	}
 
 	public void setListenerToRootView() {
 		View activityRootView = (View)TeaLeaf.get().getGroup();
 		activityRootView.getViewTreeObserver().addOnGlobalLayoutListener(this.onGlobalLayoutListener);
 	}
 
 	public void removeListenerToRootView() {
 		View activityRootView = (View)TeaLeaf.get().getGroup();
 		activityRootView.getViewTreeObserver().removeGlobalOnLayoutListener(this.onGlobalLayoutListener);
 	}
 
 
 	private void hideKeyboard() {
 		isOpened = false;
 		InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
 		imm.hideSoftInputFromWindow(this.getWindowToken(), 0);	
 	}
 
 	public OnTouchListener getScreenCaptureListener() {
 		return new OnTouchListener() {
 			@Override
 			public boolean onTouch(View v, MotionEvent event) {
 				EventQueue.pushEvent(new Event("editText.onFinishEditing"));
 				return false;
 			}
 		};	
 	}
 
 
 }
 
 
