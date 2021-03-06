 package org.zlibrary.core.application;
 
 import java.util.*;
 
 import org.zlibrary.core.library.ZLibrary;
 import org.zlibrary.core.options.*;
 import org.zlibrary.core.resources.*;
 import org.zlibrary.core.view.*;
 
 public abstract class ZLApplication {
 	private static final String MouseScrollUpKey = "<MouseScrollDown>";
 	private static final String MouseScrollDownKey = "<MouseScrollUp>";
 	
 	private static final String ROTATION = "Rotation";
 	private static final String ANGLE = "Angle";
 	private static final String STATE = "State";
 	private static final String KEYBOARD = "Keyboard";
 	private static final String FULL_CONTROL = "FullControl";
 	private static final String CONFIG = "Config";
 	private static final String AUTO_SAVE = "AutoSave";
 	private static final String TIMEOUT = "Timeout";
 
 	public final ZLIntegerOption RotationAngleOption =
		new ZLIntegerOption(ZLOption.CONFIG_CATEGORY, ROTATION, ANGLE, ZLViewWidget.Angle.DEGREES90.getDegrees());
 	public final ZLIntegerOption AngleStateOption =
 		new ZLIntegerOption(ZLOption.CONFIG_CATEGORY, STATE, ANGLE, ZLViewWidget.Angle.DEGREES0.getDegrees());	
 
 	public final ZLBooleanOption KeyboardControlOption =
 		new ZLBooleanOption(ZLOption.CONFIG_CATEGORY, KEYBOARD, FULL_CONTROL, false);
 
 	public final ZLBooleanOption ConfigAutoSavingOption =
 		new ZLBooleanOption(ZLOption.CONFIG_CATEGORY, CONFIG, AUTO_SAVE, true);
 	public final ZLIntegerRangeOption ConfigAutoSaveTimeoutOption =
 		new ZLIntegerRangeOption(ZLOption.CONFIG_CATEGORY, CONFIG, TIMEOUT, 1, 6000, 30);
 
 	public final ZLIntegerRangeOption KeyDelayOption =
 		new ZLIntegerRangeOption(ZLOption.CONFIG_CATEGORY, "Options", "KeyDelay", 0, 5000, 250);
 	
 	private final ZLPaintContext myContext;
 	private ZLViewWidget myViewWidget;
 	private ZLApplicationWindow myWindow;
 	private ZLView myInitialView;
 
 	private final Map<Integer,ZLAction> myActionMap = new HashMap<Integer,ZLAction>();
 	private Toolbar myToolbar;
 	private Menubar myMenubar;
 	//private ZLTime myLastKeyActionTime;
 	//private ZLMessageHandler myPresentWindowHandler;
 
 	protected ZLApplication() {
 		myContext = ZLibrary.getInstance().createPaintContext();
 		
 		if (ConfigAutoSavingOption.getValue()) {
 			//ZLOption.startAutoSave((int)(ConfigAutoSaveTimeoutOption.getValue()));
 		}
 
 		//myPresentWindowHandler = new PresentWindowHandler(this);
 		//ZLCommunicationManager.instance().registerHandler("present", myPresentWindowHandler);
 	}
 	
 	protected final Toolbar getToolbar() {
 		if (myToolbar == null) {
 			myToolbar = new Toolbar();
 		}
 		return myToolbar;
 	}
 
 	public final Menubar getMenubar() {
 		if (myMenubar == null) {
 			myMenubar = new Menubar();
 		}
 		return myMenubar;
 	}
 
 	protected final void setView(ZLView view) {
 		if (view != null) {
 			if (myViewWidget != null) {
 				myViewWidget.setView(view);
 				resetWindowCaption();
 				refreshWindow();
 			} else {
 				myInitialView = view;
 			}
 		}
 	}
 
 	protected final ZLView getCurrentView() {
 		return (myViewWidget != null) ? myViewWidget.getView() : null;
 	}
 
 	private final void quit() {
 		if (myWindow != null) {
 			myWindow.close();
 		}
 	}
 
 	final void setWindow(ZLApplicationWindow window) {
 		myWindow = window;
 	}
 
 	public void initWindow() {
 		setViewWidget(myWindow.createViewWidget());
 		if (KeyboardControlOption.getValue()) {
 			grabAllKeys(true);
 		}
 		myWindow.init();
 		setView(myInitialView);
 	}
 
 	protected final ZLPaintContext getContext() {
 		return myContext;
 	}
 
 	public final void refreshWindow() {
 		if (myViewWidget != null) {
 			myViewWidget.repaint();
 		}
 		if (myWindow != null) {
 			myWindow.refresh();
 		}
 	}
 
 	private final void resetWindowCaption() {
 		if (myWindow != null) {
 			ZLView view = getCurrentView();
 			if (view != null) {
 				myWindow.setCaption(view.caption());
 			}
 		}
 	}
 	
 	private final void setFullscreen(boolean fullscreen) {
 		if (myWindow != null) {
 			myWindow.setFullscreen(fullscreen);
 		}
 	}
 	
 	protected final boolean isFullscreen() {
 		return (myWindow != null) && myWindow.isFullscreen();
 	}
 	
 	protected final void addAction(int actionId, ZLAction action) {
 		myActionMap.put(actionId, action);
 	}
 
 	public final boolean isFullKeyboardControlSupported() {
 		return true;//(myWindow != null) && myWindow.isFullKeyboardControlSupported();
 	}
 	
 	private final void grabAllKeys(boolean grab) {
 		if (myWindow != null) {
 			//myWindow.grabAllKeys(grab);
 		}
 	}
 
 	public final boolean isFingerTapEventSupported() {
 		return (myWindow != null) && myWindow.isFingerTapEventSupported();
 	}
 	
 	public final boolean isMousePresented() {
 		return (myWindow != null) && myWindow.isMousePresented();
 	}
 	
 	public final boolean isKeyboardPresented() {
 		return (myWindow != null) && myWindow.isKeyboardPresented();
 	}
 	
 	public final void trackStylus(boolean track) {
 		if (myViewWidget != null) {
 			myViewWidget.trackStylus(track);
 		}
 	}
 
 	public final void setHyperlinkCursor(boolean hyperlink) {
 		if (myWindow != null) {
 			//myWindow.setHyperlinkCursor(hyperlink);
 		}
 	}
 
 	private final ZLAction getAction(int actionId) {
 		return myActionMap.get(actionId);
 	}
 	
 	final boolean isActionVisible(int actionId) {
 		ZLAction action = getAction(actionId);
 		return (action != null) && action.isVisible();
 	}
 	
 	final boolean isActionEnabled(int actionId) {
 		ZLAction action = getAction(actionId);
 		return (action != null) && action.isEnabled();
 	}
 	
 	public final void doAction(int actionId) {
 		ZLAction action = getAction(actionId);
 		if (action != null) {
 			action.checkAndRun();
 		}
 	}
 
 	abstract protected ZLKeyBindings keyBindings();
 	
 	public final void doActionByKey(String key) {
 		ZLAction a = getAction(keyBindings().getBinding(key));
 		if ((a != null) &&
 				(!a.useKeyDelay() /*||
 				 (myLastKeyActionTime.millisecondsTo(ZLTime()) >= KeyDelayOption.getValue())*/)) {
 			a.checkAndRun();
 			//myLastKeyActionTime = ZLTime();
 		}
 	}
 
 	public boolean closeView() {
 		quit();
 		return true;
 	}
 	
 	public void openFile(String fileName) {
 		// TODO: implement or change to abstract
 	}
 
 	public final void presentWindow() {
 		if (myWindow != null) {
 			//myWindow.present();
 		}
 	}
 
 	//public String lastCaller() {
 		//return null;//((PresentWindowHandler)myPresentWindowHandler).lastCaller();
 	//}
 	
 	//public void resetLastCaller() {
 		//((PresentWindowHandler)myPresentWindowHandler).resetLastCaller();
 	//}
 
 	final void setViewWidget(ZLViewWidget viewWidget) {
 		myViewWidget = viewWidget;
 	}
 
 	//Action
 	static abstract public class ZLAction {
 		public boolean isVisible() {
 			return true;
 		}
 
 		public boolean isEnabled() {
 			return isVisible();
 		}
 		
 		public final void checkAndRun() {
 			if (isEnabled()) {
 				run();
 			}
 		}
 		
 		public boolean useKeyDelay() {
 			return true;
 		}
 		
 		abstract protected void run();
 	}
 
 	//full screen action
 	protected static class FullscreenAction extends ZLAction {
 		private final ZLApplication myApplication;
 		private	final boolean myIsToggle;
 
 		public FullscreenAction(ZLApplication application, boolean toggle) {
 			myApplication = application;
 			myIsToggle = toggle;
 		}
 		
 		public boolean isVisible() {
 			return myIsToggle || !myApplication.isFullscreen();
 		}
 		
 		public void run() {
 			myApplication.setFullscreen(!myApplication.isFullscreen());
 		}
 	}
 
 	//rotation action
 	protected static final class RotationAction extends ZLAction {
 		private ZLApplication myApplication;
 
 		public RotationAction(ZLApplication application) {
 			myApplication = application;
 		}
 		
 		public boolean isVisible() {
			// temporary commented while any option return 0 :(((((
			//return (myApplication.myViewWidget != null) &&
			// ((myApplication.RotationAngleOption.getValue() != ZLViewWidget.Angle.DEGREES0.getDegrees()) ||
			//	(myApplication.myViewWidget.getRotation() != ZLViewWidget.Angle.DEGREES0));
			return true;
 		}
 		
 		public void run() {
			// temporary commented while any option return 0 :(((((
			//int optionValue = (int)myApplication.RotationAngleOption.getValue();
			int optionValue = -1;
 			ZLViewWidget.Angle oldAngle = myApplication.myViewWidget.getRotation();
 			ZLViewWidget.Angle newAngle = ZLViewWidget.Angle.DEGREES0;
 			if (optionValue == -1) {
 				newAngle = ZLViewWidget.Angle.getByDegrees((oldAngle.getDegrees() + 90) % 360);
 			} else {
 				newAngle = (oldAngle == ZLViewWidget.Angle.DEGREES0) ?
 					ZLViewWidget.Angle.getByDegrees(optionValue) : ZLViewWidget.Angle.DEGREES0;
 			}
 			myApplication.myViewWidget.rotate(newAngle);
 			myApplication.AngleStateOption.setValue(newAngle.getDegrees());
 			myApplication.refreshWindow();		
 		}
 	}
 	
 	//toolbar
 	static public final class Toolbar {
 		private final List<Item> myItems = new LinkedList<Item>();
 		private final ZLResource myResource = ZLResource.resource("toolbar");
 
 		public void addButton(int actionId, ZLResourceKey key) {
 			addButton(actionId, key, null);
 		}
 
 		private void addButton(int actionId, ZLResourceKey key, ButtonGroup group) {
 			ButtonItem button = new ButtonItem(actionId, key.Name, myResource.getResource(key));
 			myItems.add(button);
 			button.setButtonGroup(group);
 		}
 		
 		ButtonGroup createButtonGroup(int unselectAllButtonsActionId) {
 			return new ButtonGroup(unselectAllButtonsActionId);
 		}
 		
 		/*public void addOptionEntry(ZLOptionEntry entry) {
 			if (entry != null) {
 				myItems.add(new OptionEntryItem(entry));
 			}
 		}*/
 		
 		public void addSeparator() {
 			myItems.add(new SeparatorItem());
 		}
 
 		List<Item> getItems() {
 			return Collections.unmodifiableList(myItems);
 		}
 		
 		public interface Item {
 		}
 		
 		public final class ButtonItem implements Item {
 			private final int myActionId;
 			private final String myIconName;
 			private final ZLResource myTooltip;
 			private	ButtonGroup myButtonGroup;
 			
 			public ButtonItem(int actionId, String iconName, ZLResource tooltip) {
 				myActionId = actionId;
 				myIconName = iconName;
 				myTooltip = tooltip;
 			}
 
 			int getActionId() {
 				return myActionId;
 			}
 			
 			public String getIconName() {
 				return myIconName;
 			}
 			
 			public String getTooltip() {
 				return myTooltip.hasValue() ? myTooltip.value() : "";
 			}
 
 			ButtonGroup getButtonGroup() {
 				return myButtonGroup;
 			}
 			
 			boolean isToggleButton() {
 				return myButtonGroup != null;
 			}
 			
 			void press() {
 				if (isToggleButton()) { 
 					myButtonGroup.press(this);
 				}
 			}
 			
 			boolean isPressed() {
 				return isToggleButton() && (this == myButtonGroup.PressedItem);
 			}
 
 			private void setButtonGroup(ButtonGroup bg) {
 				if (myButtonGroup != null) {
 					myButtonGroup.Items.remove(this);
 				}
 				
 				myButtonGroup = bg;
 				
 				if (myButtonGroup != null) {
 					myButtonGroup.Items.add(this);
 				}
 			}	
 		}
 		
 		public class SeparatorItem implements Item {
 		}
 		
 		public class OptionEntryItem implements Item {
 			//private ZLOptionEntry myOptionEntry;
 
 			//public OptionEntryItem(ZLOptionEntry entry) {
 				//myOptionEntry = entry;
 			//}
 				
 			//public ZLOptionEntry entry() {
 			//	return myOptionEntry;
 			//}
 		}
 
 		public final class ButtonGroup {
 			public final int UnselectAllButtonsActionId;
 			public final Set<ButtonItem> Items = new HashSet<ButtonItem>();
 			public ButtonItem PressedItem;
 
 			ButtonGroup(int unselectAllButtonsActionId) {
 				UnselectAllButtonsActionId = unselectAllButtonsActionId;
 				PressedItem = null;
 			}
 			
 			void press(ButtonItem item) {
 				PressedItem = item;
 			}
 		}
 	}
 	
 	//Menu
 	static public class Menu {
 		public interface Item {
 		}
 
 		private final List<Item> myItems = new LinkedList<Item>();
 		private final ZLResource myResource;
 
 		Menu(ZLResource resource) {
 			myResource = resource;
 		}
 
 		ZLResource getResource() {
 			return myResource;
 		}
 
 		public void addItem(int actionId, ZLResourceKey key) {
 			myItems.add(new Menubar.PlainItem(myResource.getResource(key).value(), actionId));
 		}
 		
 		public void addSeparator() {
 			myItems.add(new Menubar.Separator());
 		}
 		
 		public Menu addSubmenu(ZLResourceKey key) {
 			Menubar.Submenu submenu = new Menubar.Submenu(myResource.getResource(key));
 			myItems.add(submenu);
 			return submenu;
 		}
 
 		List<Item> getItems() {
 			return Collections.unmodifiableList(myItems);
 		}
 	}
 	
 	//MenuBar
 	public static final class Menubar extends Menu {
 		public static final class PlainItem implements Item {
 			private final String myName;
 			private final int myActionId;
 
 			public PlainItem(String name, int actionId) {
 				myName = name;
 				myActionId = actionId;
 			}
 
 			public String getName() {
 				return myName;
 			}
 			
 			public int getActionId() {
 				return myActionId;
 			}
 		};
 
 		public static final class Submenu extends Menu implements Item {
 			public Submenu(ZLResource resource) {
 				super(resource);
 			}
 
 			public String getMenuName() {
 				return getResource().value();
 			}
 		};
 		
 		public static final class Separator implements Item {
 		};
 			
 		public Menubar() {
 			super(ZLResource.resource("menu"));
 		}
 	}
 
 	//MenuVisitor
 	static public abstract class MenuVisitor {
 		public final void processMenu(Menu menu) {
 			for (Menu.Item item : menu.getItems()) {
 				if (item instanceof Menubar.PlainItem) {
 					processItem((Menubar.PlainItem)item);
 				} else if (item instanceof Menubar.Submenu) {
 					Menubar.Submenu submenu = (Menubar.Submenu)item;
 					processSubmenuBeforeItems(submenu);
 					processMenu(submenu);
 					processSubmenuAfterItems(submenu);
 				} else if (item instanceof Menubar.Separator) {
 					processSepartor((Menubar.Separator)item);
 				}
 			}
 		}
 
 		protected abstract void processSubmenuBeforeItems(Menubar.Submenu submenu);
 		protected abstract void processSubmenuAfterItems(Menubar.Submenu submenu);
 		protected abstract void processItem(Menubar.PlainItem item);
 		protected abstract void processSepartor(Menubar.Separator separator);
 	}
 	
 	static public class PresentWindowHandler {//extends ZLMessageHandler {
 		private ZLApplication myApplication;
 		private String myLastCaller;
 
 		//public PresentWindowHandler(ZLApplication application);
 		//public void onMessageReceived(List<String> arguments);
 		//public String lastCaller();
 		//public void resetLastCaller();
 	}
 }
