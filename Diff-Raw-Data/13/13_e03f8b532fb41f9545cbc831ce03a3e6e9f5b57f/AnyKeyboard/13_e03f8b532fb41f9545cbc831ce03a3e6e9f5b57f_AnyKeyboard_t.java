 /*
  * Copyright (C) 2008-2009 Google Inc.
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
 
 package com.menny.android.anysoftkeyboard.keyboards;
 
 import java.util.ArrayList;
 
 import android.content.Context;
 import android.content.SharedPreferences;
 import android.content.res.Resources;
 import android.content.res.XmlResourceParser;
 import android.inputmethodservice.Keyboard;
 import android.preference.PreferenceManager;
 import android.util.Log;
 import android.view.inputmethod.EditorInfo;
 
 import com.menny.android.anysoftkeyboard.R;
 import com.menny.android.anysoftkeyboard.SoftKeyboard;
 
 public abstract class AnyKeyboard extends Keyboard 
 {
 	public final static int KEYCODE_LANG_CHANGE = -99;
 	public final static int KEYCODE_DOT_COM = -80;
 	
 	public interface HardKeyboardTranslator
 	{
 		/*
 		 * Returns the mapped character for the provided parameters.
 		 * If the provided parameters are not to be mapped, returns 0;
 		 */
 		char translatePhysicalCharacter(int keyCode, int metaKeys);
 	}
 
     private Key mEnterKey;
     
     private final String mKeyboardName;
     
     private boolean mEnabled = true;
     
     protected AnyKeyboard(Context context, int xmlLayoutResId, boolean supportsShift,
     		/*mapping XML id will be added here,*/
     		String keyboardName,
     		String keyboardEnabledPref) 
     {
         super(context, xmlLayoutResId);
         //mSupportsShift = supportsShift;
         mKeyboardName = keyboardName;
         Log.i("AnySoftKeyboard", "Creating keyboard: "+mKeyboardName);
         SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
         
         if (keyboardEnabledPref == "")
     		mEnabled = true;
     	else
     		mEnabled = sp.getBoolean(keyboardEnabledPref, true);
     	
         //TODO: parsing of the mapping xml:
         //XmlResourceParser p = getResources().getXml(id from the constructor parameter);
         //parse to a HashMap?
         //mTopKeys = new ArrayList<Key>();
     }
     
     //this function is called from within the super constructor.
     @Override
     protected Key createKeyFromXml(Resources res, Row parent, int x, int y, 
             XmlResourceParser parser) {
         Key key = super.createKeyFromXml(res, parent, x, y, parser);
         
         if (key.codes[0] == 10) 
         {
             mEnterKey = key;
         }
         else if ((key.codes[0] == Keyboard.KEYCODE_MODE_CHANGE) ||
         		 (key.codes[0] == AnyKeyboard.KEYCODE_LANG_CHANGE))
         {
//        	if (SoftKeyboard.mChangeKeysMode.equals("2"))
//        	{
//        		key.label = "";
//        		key.height = 0;
//        		key.width = 0;
//        	}
        	
 //        	String text = res.getString(R.string.change_lang_wide);
 //        	if (mChangeKeysMode.equals("3"))
 //        		text = res.getString(R.string.change_lang_regular);
 //        	else if (mChangeKeysMode.equals("2"))
 //        		text = "";
 //        		
 //        	changeLayoutKey(key, text, parent.defaultHeight);
         }
         else
         {
         	//setting the character label
         	if (isAlphabetKey(key))
         	{
         		key.label = ""+((char)key.codes[0]); 
         	}
         }
         
         Log.v("AnySoftKeyboard", "Key '"+key.codes[0]+"' will have - width: "+key.width+", height:"+key.height+", text: '"+key.label+"'.");
         return key;
     }
 
     @Override
     protected Row createRowFromXml(Resources res, XmlResourceParser parser) 
     {
     	// TODO Auto-generated method stub
     	Row aRow = super.createRowFromXml(res, parser);
     	if ((aRow.rowEdgeFlags&EDGE_TOP) != 0)
     	{
     		//top row
     		if (SoftKeyboard.mChangeKeysMode.equals("2"))
     			aRow.defaultHeight = 0;
     	}
     	return aRow;
     }
     
     private boolean isAlphabetKey(Key key) {
 		return  (!key.modifier) && 
 				(!key.sticky) &&
 				(!key.repeatable) &&
 				(key.icon == null) &&
 				(key.codes[0] > 0);
 	}
     
 //    private void changeLayoutKey(Key key, String changeString, int defaultHeight)
 //    {
 //    	Log.d("AnySoftKeyboard", "Key "+key.codes[0]+": with text '"+changeString+"'. mChangeKeysMode: "+mChangeKeysMode);
 //    	boolean fullHeight = false;
 //    	int widthPercentage = 45;
 //    	if (mChangeKeysMode.equals("3"))
 //    	{
 //    		fullHeight = true;
 //    		widthPercentage = 20;
 //    	}
 //    	else if (mChangeKeysMode.equals("2"))
 //    	{
 //    		widthPercentage = 0;
 //    	}
 //    	
 //    	int keyWidth = SoftKeyboard.msCurrentInstance.getMaxWidth() * widthPercentage / 100;
 //    	int keyHeight = fullHeight? (defaultHeight*2) : defaultHeight;
 //    	if (widthPercentage == 0)
 //    		keyHeight = 0;
 //    	
 //    	Log.d("AnySoftKeyboard", "Key will have - width: "+keyWidth+", height:"+keyHeight);
 //    	key.width = keyWidth;
 //    	key.height = keyHeight;
 //    	key.label = changeString;
 //	}
 
 	/**
      * This looks at the ime options given by the current editor, to set the
      * appropriate label on the keyboard's enter key (if it has one).
      */
     public void setImeOptions(Resources res, int options) {
         if (mEnterKey == null) {
             return;
         }
         
         switch (options&(EditorInfo.IME_MASK_ACTION|EditorInfo.IME_FLAG_NO_ENTER_ACTION)) {
             case EditorInfo.IME_ACTION_GO:
                 mEnterKey.iconPreview = null;
                 mEnterKey.icon = null;
                 mEnterKey.label = res.getText(R.string.label_go_key);
                 break;
             case EditorInfo.IME_ACTION_NEXT:
                 mEnterKey.iconPreview = null;
                 mEnterKey.icon = null;
                 mEnterKey.label = res.getText(R.string.label_next_key);
                 break;
             case EditorInfo.IME_ACTION_SEARCH:
                 mEnterKey.icon = res.getDrawable(
                         R.drawable.sym_keyboard_search);
                 mEnterKey.label = null;
                 break;
             case EditorInfo.IME_ACTION_SEND:
                 mEnterKey.iconPreview = null;
                 mEnterKey.icon = null;
                 mEnterKey.label = res.getText(R.string.label_send_key);
                 break;
             default:
                 mEnterKey.icon = res.getDrawable(
                         R.drawable.sym_keyboard_return);
                 mEnterKey.label = null;
                 break;
         }
     }
     
     public String getKeyboardName()
     {
     	//TODO: this should be taken from the strings.xml, right?
     	return mKeyboardName;
     }
     
     /*
      * This function is overridden by other alphabet keyboards, for nifty icons
      */
     public int getKeyboardIcon()
     {
     	return R.drawable.sym_keyboard_notification_icon;
     }
     
     public boolean isEnabled()
     {
     	return mEnabled;
     }
     
     public boolean isLetter(char letterCode)
     {
     	if (Character.isLetter(letterCode)) 
     		return true;
         else
         	return false;
     }
 	public void addSuggestions(String currentWord, ArrayList<String> list) 
 	{
 	}
 	
 	@Override
 	public boolean setShifted(boolean shiftState) 
 	{
 		boolean result = super.setShifted(shiftState);
 		Log.d("AnySoftKeyboard", "setShifted: shiftState:"+shiftState+". result:"+result);
 		for(Key aKey : getKeys())
 		{
 			if (aKey.codes.length > 1)
 			{
 				aKey.label = shiftState? ""+((char)aKey.codes[1]) : ""+((char)aKey.codes[0]);
 				Log.v("AnySoftKeyboard", "setShifted: changed key:"+aKey.label);
 			}
 			else
 			{
 				Log.v("AnySoftKeyboard", "setShifted: not changed key:"+aKey.label);
 			}
 		}
 		
 		return result;
 	}
 }
