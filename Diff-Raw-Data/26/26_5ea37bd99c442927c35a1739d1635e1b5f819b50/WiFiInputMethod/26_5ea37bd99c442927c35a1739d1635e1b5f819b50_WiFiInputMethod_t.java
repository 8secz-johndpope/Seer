 package com.volosyukivan;
 
 import java.util.HashSet;
 
 import com.volosyukivan.RemoteKeyListener.Stub;
 
 import android.content.ComponentName;
 import android.content.Context;
 import android.content.Intent;
 import android.content.ServiceConnection;
 import android.inputmethodservice.InputMethodService;
 import android.os.IBinder;
 import android.os.PowerManager;
 import android.os.RemoteException;
 import android.view.KeyEvent;
 import android.view.inputmethod.InputConnection;
 
 public class WiFiInputMethod extends InputMethodService {
   ServiceConnection serviceConnection;
   private RemoteKeyboard removeKeyboard;
   protected Stub keyboardListener;
 
   @Override
   public void onDestroy() {
     Debug.d("WiFiInputMethod onDestroy()");
     try {
      if (removeKeyboard != null)
        removeKeyboard.unregisterKeyListener(keyboardListener);
     } catch (RemoteException e) {
       Debug.d("Failed to unregister listener");
     }
     removeKeyboard = null;
    if (serviceConnection != null)
      unbindService(serviceConnection);
     serviceConnection = null;
     super.onDestroy();
   }
 
   PowerManager.WakeLock wakeLock;
   HashSet<Integer> pressedKeys = new HashSet<Integer>();
   
   @Override
   public void onCreate() {
     super.onCreate();
     PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
     wakeLock = pm.newWakeLock(
         PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "wifikeyboard");
     Debug.d("WiFiInputMethod started");
     serviceConnection = new ServiceConnection() {
       //@Override
       public void onServiceConnected(ComponentName name, IBinder service) {
         Debug.d("WiFiInputMethod connected to HttpService.");
         try {
           removeKeyboard = RemoteKeyboard.Stub.asInterface(service);
           keyboardListener = new RemoteKeyListener.Stub() {
             @Override
             public void keyEvent(int code, boolean pressed) throws RemoteException {
               // Debug.d("got key in WiFiInputMethod");
               receivedKey(code, pressed);
             }
             @Override
             public void charEvent(char code) throws RemoteException {
               // Debug.d("got key in WiFiInputMethod");
               receivedChar(code);
             }
           };
           RemoteKeyboard.Stub.asInterface(service).registerKeyListener(keyboardListener);
         } catch (RemoteException e) {
           throw new RuntimeException(
               "WiFiInputMethod failed to connected to HttpService.", e);
         }
       }
       //@Override
       public void onServiceDisconnected(ComponentName name) {
         Debug.d("WiFiInputMethod disconnected from HttpService.");
       }
     }; 
     if (this.bindService(new Intent(this, HttpService.class),
         serviceConnection, BIND_AUTO_CREATE) == false) {
       throw new RuntimeException("failed to connect to HttpService");
     }
   }
   
   void receivedChar(char code) {
     wakeLock.acquire();
     wakeLock.release();
     InputConnection conn = getCurrentInputConnection();
     if (conn == null) {
       Debug.d("connection closed");
       return;
     }
     String text = new String(new char[] { code } );
     conn.commitText(text, 1);
   }
   
   void receivedKey(int code, boolean pressed) {
     if (code == HttpServer.FOCUS) {
       for (int key : pressedKeys) {
         sendKey(key, false, false);
       }
       pressedKeys.clear();
      resetModifiers();
       return;
     }
     if (pressedKeys.contains(code) == pressed) {
       if (pressed == false) return;
       // ignore autorepeat on following keys
       switch (code) {
       case KeyEvent.KEYCODE_ALT_LEFT:
       case KeyEvent.KEYCODE_SHIFT_LEFT:
       case KeyEvent.KEYCODE_HOME:
       case KeyEvent.KEYCODE_MENU: return;
       }
     }
     if (pressed) {
       pressedKeys.add(code);
       sendKey(code, pressed, false);
     } else {
       pressedKeys.remove(code);
       sendKey(code, pressed, pressedKeys.isEmpty());
     }
   }
   
  void resetModifiers() {
    InputConnection conn = getCurrentInputConnection();
    conn.clearMetaKeyStates(
        KeyEvent.META_ALT_ON | KeyEvent.META_SHIFT_ON | KeyEvent.META_SYM_ON);
  }
  
   void sendKey(int code, boolean down, boolean resetModifiers) {
     wakeLock.acquire();
     wakeLock.release();
     InputConnection conn = getCurrentInputConnection();
     if (conn == null) {
       Debug.d("connection closed");
       return;
     }
     conn.sendKeyEvent(new KeyEvent(
         down ? KeyEvent.ACTION_DOWN : KeyEvent.ACTION_UP, code));
     if (resetModifiers) {
       conn.clearMetaKeyStates(
           KeyEvent.META_ALT_ON | KeyEvent.META_SHIFT_ON | KeyEvent.META_SYM_ON);
     }
   }
 }
