 package com.menny.android.anysoftkeyboard.keyboards;
 
 import java.util.HashMap;
 
 import android.view.KeyEvent;
 
 import com.menny.android.anysoftkeyboard.AnyKeyboardContextProvider;
 import com.menny.android.anysoftkeyboard.R;
 import com.menny.android.anysoftkeyboard.keyboards.AnyKeyboard.HardKeyboardTranslator;
 
 public class BulgarianPhoneticKeyboard extends AnyKeyboard implements HardKeyboardTranslator
 {
 	private final static HashMap<Integer, Integer> msPhysicalKeysMap;
 	private final static HashMap<Integer, Integer> msPhysicalShiftKeysMap;
     
     static
     {    	
     	msPhysicalKeysMap = new HashMap<Integer, Integer>();
     	msPhysicalKeysMap.put(KeyEvent.KEYCODE_A, 1072);
     	msPhysicalKeysMap.put(KeyEvent.KEYCODE_B, 1073);
     	msPhysicalKeysMap.put(KeyEvent.KEYCODE_C, 1094);
     	msPhysicalKeysMap.put(KeyEvent.KEYCODE_D, 1076);
     	msPhysicalKeysMap.put(KeyEvent.KEYCODE_E, 1077);
     	msPhysicalKeysMap.put(KeyEvent.KEYCODE_F, 1092);
     	msPhysicalKeysMap.put(KeyEvent.KEYCODE_G, 1075);
     	msPhysicalKeysMap.put(KeyEvent.KEYCODE_H, 1093);
     	msPhysicalKeysMap.put(KeyEvent.KEYCODE_I, 1080);
     	msPhysicalKeysMap.put(KeyEvent.KEYCODE_J, 1081);
     	msPhysicalKeysMap.put(KeyEvent.KEYCODE_K, 1082);
     	msPhysicalKeysMap.put(KeyEvent.KEYCODE_L, 1083);
     	msPhysicalKeysMap.put(KeyEvent.KEYCODE_M, 1084);
     	msPhysicalKeysMap.put(KeyEvent.KEYCODE_N, 1085);
     	msPhysicalKeysMap.put(KeyEvent.KEYCODE_O, 1086);
     	msPhysicalKeysMap.put(KeyEvent.KEYCODE_P, 1087);
     	msPhysicalKeysMap.put(KeyEvent.KEYCODE_Q, 1071);
     	msPhysicalKeysMap.put(KeyEvent.KEYCODE_R, 1088);
     	msPhysicalKeysMap.put(KeyEvent.KEYCODE_S, 1089);
     	msPhysicalKeysMap.put(KeyEvent.KEYCODE_T, 1090);
     	msPhysicalKeysMap.put(KeyEvent.KEYCODE_U, 1091);
     	msPhysicalKeysMap.put(KeyEvent.KEYCODE_V, 1078);
     	msPhysicalKeysMap.put(KeyEvent.KEYCODE_W, 1074);
     	msPhysicalKeysMap.put(KeyEvent.KEYCODE_X, 1100);
     	msPhysicalKeysMap.put(KeyEvent.KEYCODE_Y, 1098);
     	msPhysicalKeysMap.put(KeyEvent.KEYCODE_Z, 1079);
    	msPhysicalKeysMap.put(KeyEvent.KEYCODE_ENVELOPE, 1095);
     	
     	msPhysicalShiftKeysMap = new HashMap<Integer, Integer>();
     	msPhysicalShiftKeysMap.put(KeyEvent.KEYCODE_A, 1040);
     	msPhysicalShiftKeysMap.put(KeyEvent.KEYCODE_B, 1041);
     	msPhysicalShiftKeysMap.put(KeyEvent.KEYCODE_C, 1062);
     	msPhysicalShiftKeysMap.put(KeyEvent.KEYCODE_D, 1044);
     	msPhysicalShiftKeysMap.put(KeyEvent.KEYCODE_E, 1045);
     	msPhysicalShiftKeysMap.put(KeyEvent.KEYCODE_F, 1060);
     	msPhysicalShiftKeysMap.put(KeyEvent.KEYCODE_G, 1043);
     	msPhysicalShiftKeysMap.put(KeyEvent.KEYCODE_H, 1061);
     	msPhysicalShiftKeysMap.put(KeyEvent.KEYCODE_I, 1048);
     	msPhysicalShiftKeysMap.put(KeyEvent.KEYCODE_J, 1049);
     	msPhysicalShiftKeysMap.put(KeyEvent.KEYCODE_K, 1050);
     	msPhysicalShiftKeysMap.put(KeyEvent.KEYCODE_L, 1051);
     	msPhysicalShiftKeysMap.put(KeyEvent.KEYCODE_M, 1052);
     	msPhysicalShiftKeysMap.put(KeyEvent.KEYCODE_N, 1053);
     	msPhysicalShiftKeysMap.put(KeyEvent.KEYCODE_O, 1054);
     	msPhysicalShiftKeysMap.put(KeyEvent.KEYCODE_P, 1055);
     	msPhysicalShiftKeysMap.put(KeyEvent.KEYCODE_Q, 1071);
     	msPhysicalShiftKeysMap.put(KeyEvent.KEYCODE_R, 1056);
     	msPhysicalShiftKeysMap.put(KeyEvent.KEYCODE_S, 1057);
     	msPhysicalShiftKeysMap.put(KeyEvent.KEYCODE_T, 1058);
     	msPhysicalShiftKeysMap.put(KeyEvent.KEYCODE_U, 1059);
     	msPhysicalShiftKeysMap.put(KeyEvent.KEYCODE_V, 1046);
     	msPhysicalShiftKeysMap.put(KeyEvent.KEYCODE_W, 1042);
     	msPhysicalShiftKeysMap.put(KeyEvent.KEYCODE_X, 1068);
     	msPhysicalShiftKeysMap.put(KeyEvent.KEYCODE_Y, 1066);
     	msPhysicalShiftKeysMap.put(KeyEvent.KEYCODE_Z, 1047);
    	msPhysicalShiftKeysMap.put(KeyEvent.KEYCODE_ENVELOPE, 1063);
     }
 
 	public BulgarianPhoneticKeyboard(AnyKeyboardContextProvider context) 
 	{
 		super(context, R.xml.bg_ph_qwerty, false, R.string.bg_ph_keyboard, "bg_ph_keyboard", false);
 	}
 
 	@Override
 	public int getKeyboardIcon() 
 	{
 		return R.drawable.bg_ph;
 	}
 	
 	private enum SequenceStage
 	{
 		None,
 		c,
 		C,
 		s,
 		S,
 		sh,
 		SH,
 		y,
 		Y,
 	}
 	
 	private SequenceStage mCurrentSequenceStage = SequenceStage.None;
 	
 	public char translatePhysicalCharacter(int primaryCode, int metaState) 
 	{
 		if ((metaState&KeyEvent.META_ALT_ON) == 0)
 		{
 			if ((mCurrentSequenceStage == SequenceStage.c) &&
 					(primaryCode == KeyEvent.KEYCODE_H))
 			{
 				mCurrentSequenceStage = SequenceStage.None;
 				super.getKeyboardContext().deleteLastCharactersFromInput(1);
 				return (char)1095;
 			}
 			
 			if ((mCurrentSequenceStage == SequenceStage.s) &&
 					(primaryCode == KeyEvent.KEYCODE_H))
 			{
 				mCurrentSequenceStage = SequenceStage.sh;
 				super.getKeyboardContext().deleteLastCharactersFromInput(1);
 				return (char)1096;
 			}
 			
 			if ((mCurrentSequenceStage == SequenceStage.sh) &&
 					(primaryCode == KeyEvent.KEYCODE_T))
 			{
 				mCurrentSequenceStage = SequenceStage.None;
 				super.getKeyboardContext().deleteLastCharactersFromInput(1);
 				return (char)1097;
 			}
 			
 			if ((mCurrentSequenceStage == SequenceStage.y) &&
 					(primaryCode == KeyEvent.KEYCODE_U))
 			{
 				mCurrentSequenceStage = SequenceStage.None;
 				super.getKeyboardContext().deleteLastCharactersFromInput(1);
 				return (char)1102;
 			}
 			
 			if ((mCurrentSequenceStage == SequenceStage.C) &&
 					(primaryCode == KeyEvent.KEYCODE_H))
 			{
 				mCurrentSequenceStage = SequenceStage.None;
 				super.getKeyboardContext().deleteLastCharactersFromInput(1);
 				return (char)1063;
 			}
 			
 			if ((mCurrentSequenceStage == SequenceStage.S) &&
 					(primaryCode == KeyEvent.KEYCODE_H))
 			{
 				mCurrentSequenceStage = SequenceStage.SH;
 				super.getKeyboardContext().deleteLastCharactersFromInput(1);
 				return (char)1064;
 			}
 			
 			if ((mCurrentSequenceStage == SequenceStage.SH) &&
 					(primaryCode == KeyEvent.KEYCODE_T))
 			{
 				mCurrentSequenceStage = SequenceStage.None;
 				super.getKeyboardContext().deleteLastCharactersFromInput(1);
 				return (char)1065;
 			}
 			
 			if ((mCurrentSequenceStage == SequenceStage.Y) &&
 					(primaryCode == KeyEvent.KEYCODE_U))
 			{
 				mCurrentSequenceStage = SequenceStage.None;
 				super.getKeyboardContext().deleteLastCharactersFromInput(1);
 				return (char)1070;
 			}
 				
 			if ((metaState&KeyEvent.META_SHIFT_ON) == 0)
 			{
 				if (primaryCode == KeyEvent.KEYCODE_S)
 					mCurrentSequenceStage = SequenceStage.s;
 				else if (primaryCode == KeyEvent.KEYCODE_C)
 					mCurrentSequenceStage = SequenceStage.c;
 				else if (primaryCode == KeyEvent.KEYCODE_Y)
 					mCurrentSequenceStage = SequenceStage.y;
 				else
 					mCurrentSequenceStage = SequenceStage.None;
 				
 				if (msPhysicalKeysMap.containsKey(primaryCode))
 					return (char)msPhysicalKeysMap.get(primaryCode).intValue();
 				else
 					return 0;
 			}
 			else
 			{
 				if (primaryCode == KeyEvent.KEYCODE_S)
 					mCurrentSequenceStage = SequenceStage.S;
 				else if (primaryCode == KeyEvent.KEYCODE_C)
 					mCurrentSequenceStage = SequenceStage.C;
 				else if (primaryCode == KeyEvent.KEYCODE_Y)
 					mCurrentSequenceStage = SequenceStage.Y;
 				else
 					mCurrentSequenceStage = SequenceStage.None;
 				
 				if (msPhysicalShiftKeysMap.containsKey(primaryCode))
 					return (char)msPhysicalShiftKeysMap.get(primaryCode).intValue();
 				else
 					return 0;
 			}
 		}
 		else
 		{
 			return 0;
 		}
 	}
 }
