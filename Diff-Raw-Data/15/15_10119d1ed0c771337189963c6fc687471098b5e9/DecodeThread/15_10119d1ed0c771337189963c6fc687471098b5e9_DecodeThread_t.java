 /*
  * Copyright (C) 2008 ZXing authors
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
 
 package com.google.zxing.client.android;
 
 import com.google.zxing.BarcodeFormat;
 import com.google.zxing.DecodeHintType;
 import com.google.zxing.ResultPointCallback;
 
 import android.content.SharedPreferences;
 import android.os.Handler;
 import android.os.Looper;
 import android.preference.PreferenceManager;
 
 import java.util.Hashtable;
 import java.util.Vector;
 
 /**
  * This thread does all the heavy lifting of decoding the images.
  *
  * @author dswitkin@google.com (Daniel Switkin)
  */
 final class DecodeThread extends Thread {
 
   public static final String BARCODE_BITMAP = "barcode_bitmap";
 
  private final CaptureActivity activity;
  private final Hashtable<DecodeHintType, Object> hints;
  private Handler handler;
 
   DecodeThread(CaptureActivity activity,
                Vector<BarcodeFormat> decodeFormats,
                String characterSet,
                ResultPointCallback resultPointCallback) {

    this.activity = activity;

    hints = new Hashtable<DecodeHintType, Object>(3);
 
     // The prefs can't change while the thread is running, so pick them up once here.
     if (decodeFormats == null || decodeFormats.isEmpty()) {
       SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
       boolean decode1D = prefs.getBoolean(PreferencesActivity.KEY_DECODE_1D, true);
       boolean decodeQR = prefs.getBoolean(PreferencesActivity.KEY_DECODE_QR, true);
       if (decode1D && decodeQR) {
         hints.put(DecodeHintType.POSSIBLE_FORMATS, CaptureActivity.ALL_FORMATS);
       } else if (decode1D) {
         hints.put(DecodeHintType.POSSIBLE_FORMATS, CaptureActivity.ONE_D_FORMATS);
       } else if (decodeQR) {
         hints.put(DecodeHintType.POSSIBLE_FORMATS, CaptureActivity.QR_CODE_FORMATS);
       }
     } else {
       hints.put(DecodeHintType.POSSIBLE_FORMATS, decodeFormats);
     }
 
     if (characterSet != null) {
       hints.put(DecodeHintType.CHARACTER_SET, characterSet);
     }
 
     hints.put(DecodeHintType.NEED_RESULT_POINT_CALLBACK, resultPointCallback);
   }
 
   Handler getHandler() {
     return handler;
   }
 
   @Override
   public void run() {
     Looper.prepare();
    handler = new DecodeHandler(activity, hints);    
     Looper.loop();
   }
 
 }
