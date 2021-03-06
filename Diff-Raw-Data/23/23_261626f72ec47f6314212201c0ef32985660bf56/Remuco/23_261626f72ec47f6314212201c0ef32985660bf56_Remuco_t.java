 /*   
  *   Remuco - A remote control system for media players.
  *   Copyright (C) 2006-2009 Oben Sonne <obensonne@googlemail.com>
  *
  *   This file is part of Remuco.
  *
  *   Remuco is free software: you can redistribute it and/or modify
  *   it under the terms of the GNU General Public License as published by
  *   the Free Software Foundation, either version 3 of the License, or
  *   (at your option) any later version.
  *
  *   Remuco is distributed in the hope that it will be useful,
  *   but WITHOUT ANY WARRANTY; without even the implied warranty of
  *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  *   GNU General Public License for more details.
  *
  *   You should have received a copy of the GNU General Public License
  *   along with Remuco.  If not, see <http://www.gnu.org/licenses/>.
  *   
  */
 package remuco;
 
 import java.util.Hashtable;
 
 import javax.microedition.lcdui.Alert;
 import javax.microedition.lcdui.AlertType;
 import javax.microedition.lcdui.Command;
 import javax.microedition.lcdui.CommandListener;
 import javax.microedition.lcdui.Display;
 import javax.microedition.lcdui.Displayable;
 import javax.microedition.lcdui.Form;
 import javax.microedition.lcdui.ImageItem;
 import javax.microedition.lcdui.Item;
 import javax.microedition.lcdui.List;
 import javax.microedition.lcdui.StringItem;
 
 import remuco.comm.BluetoothFactory;
 import remuco.comm.Connection;
 import remuco.comm.Device;
 import remuco.comm.IConnectionListener;
 import remuco.comm.IDeviceSelectionListener;
 import remuco.comm.IServiceFinder;
 import remuco.comm.IServiceListener;
 import remuco.comm.InetServiceFinder;
 import remuco.player.Player;
 import remuco.ui.CMD;
 import remuco.ui.Theme;
 import remuco.ui.screens.DeviceSelectorScreen;
 import remuco.ui.screens.LogScreen;
 import remuco.ui.screens.PlayerScreen;
 import remuco.ui.screens.ServiceSelectorScreen;
 import remuco.ui.screens.WaitingScreen;
 import remuco.util.FormLogger;
 import remuco.util.Log;
 
 /**
  * MIDlet of the Remuco client.
  * <p>
  * <h1>Emulator Code</h1>
  * Some code is only used while running inside the WTK emulator. All
  * corresponding code is either tagged with <code>emulator</code> in its JavaDoc
  * or is located inside an if-statement block using the condition
  * {@link #EMULATION}.
  * 
  * @author Oben Sonne
  * 
  */
 public class Remuco implements CommandListener, IConnectionListener,
 		IServiceListener, IDeviceSelectionListener {
 
 	private class ReconnectDialog extends Form implements CommandListener {
 
 		private final String url;
 
 		public ReconnectDialog(String url, String msg) {
 			super("Disconnected");
 			this.url = url;
 			final ImageItem img = new ImageItem(null,
 					Theme.getInstance().aicConnecting, Item.LAYOUT_CENTER
 							| Item.LAYOUT_NEWLINE_AFTER, "");
 			append(img);
 			final StringItem text = new StringItem(null, msg);
 			text.setLayout(Item.LAYOUT_CENTER | Item.LAYOUT_NEWLINE_AFTER);
 			append(text);
 			append("\n");
 			final StringItem question = new StringItem("Reconnect?", null);
 			question.setLayout(Item.LAYOUT_CENTER | Item.LAYOUT_NEWLINE_AFTER);
 			append(question);
 			addCommand(CMD.YES);
 			addCommand(CMD.NO);
 			setCommandListener(this);
 
 		}
 
 		public void commandAction(Command c, Displayable d) {
 
 			if (c == CMD.YES) {
 				connect(url);
 			} else if (c == CMD.NO) {
 				display.setCurrent(screenDeviceSelector);
 			} else {
 				Log.bug("Apr 9, 2009.9:53:36 PM");
 			}
 		}
 
 	}
 
 	/**
 	 * @emulator If <code>true</code>, the client runs inside the WTK emulator.
 	 */
 	public static final boolean EMULATION;
 
 	public static final String VERSION = "@VERSION@";
 
 	static {
 		EMULATION = "@EMULATION@".equalsIgnoreCase("true") ? true : false;
 	}
 
 	/** An alert to signal an alerting message :) */
 	private final Alert alert;
 
 	private final Alert alertLoadConfig;
 
 	private final Alert alertSaveConfig;
 
 	private final Config config;
 
 	private final Display display;
 
 	private Displayable displayableAfterLog;
 
 	private final Entry midlet;
 
 	/**
 	 * Screen to show progress while connecting.
 	 * <p>
 	 * This waiting screen's property is used for synchronizing connection state
 	 * handling between the UI event thread and the global timer thread.
 	 * */
 	private final WaitingScreen screenConnecting;
 
 	/** Screen to select a device to connect to */
 	private final DeviceSelectorScreen screenDeviceSelector;
 
 	private final Form screenLog;
 
 	/** Main player interaction screen */
 	private PlayerScreen screenPlayer = null;
 
 	/** Screen to select a service (media player) */
 	private final ServiceSelectorScreen screenServiceSelector;
 
 	public Remuco(Entry midlet) {
 
 		this.midlet = midlet;
 		display = Display.getDisplay(midlet);
 
 		// set up logging
 
 		screenLog = new LogScreen(display);
 		screenLog.addCommand(CMD.BACK);
 		screenLog.setCommandListener(this);
 		if (EMULATION) {
 			Log.ln("RUNING IN EMULATION MODE ..");
 			screenLog.append("Emulation -> logging goes to standard out!");
 		} else {
 			Log.setOut(new FormLogger(screenLog));
 		}
 
 		// init configuration
 
 		Config.init(midlet);
 
 		config = Config.getInstance();
 
 		// set up some displayables
 
 		alertLoadConfig = new Alert("Error");
 		alertLoadConfig.setString("Errors while loading configuration. "
 				+ "Please inspect the log for details! Configuration erros "
 				+ "are normal, if you installed a new version of the client.");
 		alertLoadConfig.setType(AlertType.ERROR);
 		alertLoadConfig.setTimeout(Alert.FOREVER);
 		alertLoadConfig.setCommandListener(this);
 
 		alertSaveConfig = new Alert("Error");
 		alertSaveConfig.setString("Errors while saving configuration."
 				+ " Please inspect the log for details!");
 		alertSaveConfig.setType(AlertType.ERROR);
 		alertSaveConfig.setTimeout(Alert.FOREVER);
 		alertSaveConfig.setCommandListener(this);
 
 		// set up the start screen
 
 		alert = new Alert("");
 		alert.setTimeout(Alert.FOREVER);
 		alert.setType(AlertType.ERROR);
 
 		screenServiceSelector = new ServiceSelectorScreen();
 		screenServiceSelector.addCommand(CMD.BACK);
 		screenServiceSelector.setCommandListener(this);
 
 		screenConnecting = new WaitingScreen();
 		// screenConnecting = new WaitingAlert();
 		screenConnecting.setTitle("Connecting");
 		screenConnecting.setImage(Theme.getInstance().aicConnecting);
 		screenConnecting.setCommandListener(this);
 
 		screenDeviceSelector = new DeviceSelectorScreen(this, display, this);
 		screenDeviceSelector.addCommand(CMD.LOG);
 		screenDeviceSelector.addCommand(CMD.EXIT);
 
 		// TODO: handle no device situation
 		if (config.loadedSuccessfully) {
 			display.setCurrent(screenDeviceSelector);
 		} else {
 			display.setCurrent(alertLoadConfig, screenDeviceSelector);
 		}
 	}
 
 	public void commandAction(Command c, Displayable d) {
 
 		if (c == CMD.LOG) {
 
 			displayableAfterLog = d;
 
 			display.setCurrent(screenLog);
 
 		} else if (c == CMD.BACK && d == screenLog) {
 
 			// display the displayable shown before the log
 
 			if (displayableAfterLog != null) {
 				display.setCurrent(displayableAfterLog);
 				displayableAfterLog = null;
 			} else {
 				Log.bug("Aug 18, 2009.16:38:28 AM");
 			}
 
 		} else if (c == List.SELECT_COMMAND && d == screenServiceSelector) {
 
 			connect(screenServiceSelector.getSelectedService());
 
 		} else if (c == CMD.BACK && d == screenServiceSelector) {
 
 			display.setCurrent(screenDeviceSelector);
 
 		} else if (c == WaitingScreen.CMD_CANCEL && d == screenConnecting) {
 
 			// user canceled connection setup
 
 			final Object property = screenConnecting.detachProperty();
 
 			if (property == null) {
 				return; // already connected
 			}
 
 			if (property instanceof IServiceFinder) {
 				// currently searching for services
 				((IServiceFinder) property).cancelServiceSearch();
 			} else if (property instanceof Connection) {
 				// currently waiting for player description
 				((Connection) property).down();
 			} else {
 				Log.bug("Mar 17, 2009.9:40:43 PM");
 			}
 
 			display.setCurrent(screenDeviceSelector);
 
 		} else if (c == CMD.EXIT) {
 
 			disconnect();
 
 			if (config.save()) {
 				midlet.notifyExit();
 			} else {
 				display.setCurrent(alertSaveConfig);
 			}
 
 		} else if (c == CMD.BACK && d == screenPlayer) {
 
 			disconnect();
 
 			final Alert confirm = new Alert("Disconnected",
 					"Disconnected from remote player.",
 					Theme.getInstance().aicConnecting, AlertType.CONFIRMATION);
 			confirm.setTimeout(1500);
 			display.setCurrent(confirm, screenDeviceSelector);
 
 		} else if (c == Alert.DISMISS_COMMAND && d == alertLoadConfig) {
 
 			// continue startup
 			
 			display.setCurrent(screenDeviceSelector);
 
 		} else if (c == Alert.DISMISS_COMMAND && d == alertSaveConfig) {
 
 			// continue shut down
 
 			midlet.notifyExit();
 
 		} else {
 
 			Log.ln("[ROOT] unexpected command: " + c.getLabel());
 
 		}
 
 	}
 
 	public void notifyConnected(Player player) {
 
 		if (screenConnecting.detachProperty() == null) {
 			// connection set up already canceled by user
 			return;
 		}
 
 		screenPlayer = new PlayerScreen(display, player);
 		screenPlayer.addCommand(CMD.BACK);
 		screenPlayer.addCommand(CMD.LOG);
 		screenPlayer.addCommand(CMD.EXIT);
 		screenPlayer.setCommandListener(this);
 
 		Log.ln("[UI] show player screen");
 
 		display.setCurrent(screenPlayer);
 	}
 
 	public void notifyDisconnected(String url, UserException reason) {
 
 		disconnect();
 
 		if (url != null) {
 			display.setCurrent(new ReconnectDialog(url, reason.getDetails()));
 		} else {
 			alert(reason, screenDeviceSelector);
 		}
 	}
 
 	public void notifySelectedDevice(Device device) {
 
 		final IServiceFinder sf;
 
 		if (device.type == Device.BLUETOOTH && device.address.indexOf(':') < 0) {
 
			sf = BluetoothFactory.createBluetoothServiceFinder();
 
 		} else if (device.type == Device.WIFI) {
 
			sf = new InetServiceFinder();
 
 		} else {
 
 			Log.bug("Jan 26, 2009.7:29:56 PM");
 			return;
 		}
 
 		try {
 			sf.findServices(device.address, this);
 		} catch (UserException e) {
 			alert(e, screenDeviceSelector);
 			return;
 		}
 
 		screenConnecting.attachProperty(sf);
 		screenConnecting.setMessage("Searching for players.");
 		display.setCurrent(screenConnecting);
 
 	}
 
 	public void notifyServices(Hashtable services, UserException ex) {
 
 		if (screenConnecting.detachProperty() == null) {
 			// connection set up already canceled by user
 			return;
 		}
 
 		if (ex != null) {
 			alert(ex, screenDeviceSelector);
 			return;
 		}
 
 		if (services.size() == 1) {
 
 			final String url = (String) services.elements().nextElement();
 
 			connect(url);
 
 		} else {
 
 			screenServiceSelector.setServices(services);
 
 			display.setCurrent(screenServiceSelector);
 		}
 	}
 
 	/**
 	 * Called when the application managed requests to shutdown.
 	 * <p>
 	 * In this method important and delay-free clean up stuff has to be
 	 * implemented.
 	 */
 	protected void destroy() {
 
 		disconnect();
 		config.save();
 
 	}
 
 	/**
 	 * Alert an error user to the user.
 	 * 
 	 * @param ue
 	 *            the user exception describing the error
 	 * @param next
 	 *            the displayable to show after the alert
 	 */
 	private void alert(UserException ue, Displayable next) {
 
 		alert.setTitle(ue.getError());
 		alert.setString(ue.getDetails());
 		display.setCurrent(alert, next);
 	}
 
 	/**
 	 * Connect to the given service and set up a waiting screen.
 	 * 
 	 * @param url
 	 *            the service url
 	 */
 	private void connect(String url) {
 
 		final Connection conn;
 
 		try {
 			conn = new Connection(url, this);
 		} catch (UserException e) {
 			alert(e, screenDeviceSelector);
 			return;
 		}
 
 		screenConnecting.attachProperty(conn);
 		screenConnecting.setMessage("Connecting to player.");
 		display.setCurrent(screenConnecting);
 	}
 
 	/**
 	 * Disconnects from the currently connected player (if there is one) and do
 	 * related clean up.
 	 */
 	private void disconnect() {
 
 		config.removeSessionOptionListener();
 
 		if (screenPlayer != null) {
 			screenPlayer.getPlayer().disconnect();
 			screenPlayer = null;
 		}
 	}
 }
