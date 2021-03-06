 package jake2.sys;
 
 import jake2.Defines;
 import jake2.client.Key;
 import jake2.qcommon.Cbuf;
 
 import org.lwjgl.input.Keyboard;
 import org.lwjgl.input.Mouse;
 import org.lwjgl.opengl.Display;
 
 /**
  * @author dsanders
  */
 public class LWJGLKBD extends KBD {
 
 	private int win_w2;
 	private int win_h2;
 	
 	private char[] lwjglKeycodeMap = null;
 	
 	public void Init()
 	{
 		try
 		{
 			if (!Keyboard.isCreated()) Keyboard.create();
 			if (!Mouse.isCreated()) Mouse.create();
 			
 			if (!Keyboard.isBuffered()) Keyboard.enableBuffer();
 			if (!Keyboard.isTranslationEnabled()) Keyboard.enableTranslation();
 			if (!Mouse.isBuffered()) Mouse.enableBuffer();
 			
 			win_w2=Display.getDisplayMode().getWidth()/2;
 			win_h2=Display.getDisplayMode().getHeight()/2;
 
 			if (lwjglKeycodeMap == null) lwjglKeycodeMap = new char[256];
 			
 		} catch (Exception e) {;}	
 	}
 
 	public void Update() {
 		// get events
 		HandleEvents();
 	}
 
 	public void Close() {
 		Keyboard.destroy();
 		Mouse.destroy();
 		// free the memory for GC
 		lwjglKeycodeMap = null;
 	}
 	
 	private void HandleEvents() 
 	{
 		Keyboard.poll();
 		
 		if (Display.isCloseRequested())
 		{
 			Cbuf.ExecuteText(Defines.EXEC_APPEND, "quit");
 		}
 
 		while (Keyboard.next())
 		{
 			int key = Keyboard.getEventKey();
 			char ch = Keyboard.getEventCharacter();
 			boolean down =  Keyboard.getEventKeyState();
 			
 			// fill the character translation table
 			// this is needed because the getEventCharacter() returns \0 if a key is released
 			// keycode is correct but the charachter value is not
 			if (down) lwjglKeycodeMap[key] = ch;
 
 			Do_Key_Event(XLateKey(key,ch), down);
 		}	
 		
 		if (IN.mouse_active)
 		{
 			mx =  Mouse.getDX() << 1;
 			my = -Mouse.getDY() << 1;
 		}
 		else
 		{
 			mx=0;
 			my=0;
 		}
 		
 		while (Mouse.next())
 		{
 			int button=Mouse.getEventButton();
			if (button>=0)
 			{
 				Do_Key_Event(Key.K_MOUSE1 + button, Mouse.getEventButtonState());
 			}
 		}	
 	}
 
 	private int XLateKey(int code, int ch) 
 	{
 		int key = 0;
 
 		switch(code) 
 		{
 //	00626                 case XK_KP_Page_Up:      key = K_KP_PGUP; break;
 			case Keyboard.KEY_PRIOR: key = Key.K_PGUP; break;
  
 //	00629                 case XK_KP_Page_Down: key = K_KP_PGDN; break;
 			case Keyboard.KEY_NEXT: key = Key.K_PGDN; break;
 
 //	00632                 case XK_KP_Home: key = K_KP_HOME; break;
 			case Keyboard.KEY_HOME: key = Key.K_HOME; break;
 
 //	00635                 case XK_KP_End:  key = K_KP_END; break;
 			case Keyboard.KEY_END: key = Key.K_END; break;
  
 			// case Keyboard.KEY_LEFT: key = Key.K_KP_LEFTARROW; break;
 			case Keyboard.KEY_LEFT: key = Key.K_LEFTARROW; break;
  
 			// case Keyboard.KEY_RIGHT: key = Key.K_KP_RIGHTARROW; break;
 			case Keyboard.KEY_RIGHT: key = Key.K_RIGHTARROW; break;
 
 			// case Keyboard.KEY_DOWN: key = Key.K_KP_DOWNARROW; break;
 			case Keyboard.KEY_DOWN: key = Key.K_DOWNARROW; break;
 
 			// case Keyboard.KEY_UP: key = Key.K_KP_UPARROW; break;
 			case Keyboard.KEY_UP: key = Key.K_UPARROW; break; 
 
 			case Keyboard.KEY_ESCAPE: key = Key.K_ESCAPE; break; 
 
 			
 			case Keyboard.KEY_RETURN: key = Key.K_ENTER; break; 
 //	00652                 case XK_KP_Enter: key = K_KP_ENTER;     break;
 
 			case Keyboard.KEY_TAB: key = Key.K_TAB; break; 
 
 			case Keyboard.KEY_F1: key = Key.K_F1; break;
 			case Keyboard.KEY_F2: key = Key.K_F2; break;
 			case Keyboard.KEY_F3: key = Key.K_F3; break;
 			case Keyboard.KEY_F4: key = Key.K_F4; break;
 			case Keyboard.KEY_F5: key = Key.K_F5; break;
 			case Keyboard.KEY_F6: key = Key.K_F6; break;
 			case Keyboard.KEY_F7: key = Key.K_F7; break;
 			case Keyboard.KEY_F8: key = Key.K_F8; break;
 			case Keyboard.KEY_F9: key = Key.K_F9; break;
 			case Keyboard.KEY_F10: key = Key.K_F10; break;
 			case Keyboard.KEY_F11: key = Key.K_F11; break;
 			case Keyboard.KEY_F12: key = Key.K_F12; break; 
 
 			case Keyboard.KEY_BACK: key = Key.K_BACKSPACE; break; 
 
 			case Keyboard.KEY_DELETE: key = Key.K_DEL; break; 
 //	00683                 case XK_KP_Delete: key = K_KP_DEL; break;
 
 			case Keyboard.KEY_PAUSE: key = Key.K_PAUSE; break; 
 	
 			case Keyboard.KEY_RSHIFT:
 			case Keyboard.KEY_LSHIFT: key = Key.K_SHIFT; break; 
 			
 			case Keyboard.KEY_RCONTROL:
 			case Keyboard.KEY_LCONTROL: key = Key.K_CTRL; break; 
 			
 			case Keyboard.KEY_LMENU:
 			case Keyboard.KEY_RMENU: key = Key.K_ALT; break;
  
 //	00700                 case XK_KP_Begin: key = K_KP_5; break;
 //	00701
 			case Keyboard.KEY_INSERT: key = Key.K_INS; break;
 			// toggle console for DE and US keyboards
 			case Keyboard.KEY_GRAVE:
 			case Keyboard.KEY_CIRCUMFLEX: key = '`'; break;
 
 			default:
 				key = lwjglKeycodeMap[code];
 				if (key >= 'A' && key <= 'Z')
 						key = key - 'A' + 'a';
 			break;
 		}
 		if (key > 255) key = 0;
 		return key;
 	}	
 		
 	public void Do_Key_Event(int key, boolean down) {
 		Key.Event(key, down, Sys.Milliseconds());
 	}
 
 	public void centerMouse()
 	{
 		Mouse.setPosition(win_x + win_w2,win_y + win_h2);
 		//Mouse.setGrabbed(true);
 		//Mouse.setTrackingEnabled(true);
 	}
 	
 	public void installGrabs()
 	{
 		Mouse.setGrabbed(true);
 	}
 	
 	public void uninstallGrabs()
 	{
 		Mouse.setGrabbed(false);
 	}
 }
