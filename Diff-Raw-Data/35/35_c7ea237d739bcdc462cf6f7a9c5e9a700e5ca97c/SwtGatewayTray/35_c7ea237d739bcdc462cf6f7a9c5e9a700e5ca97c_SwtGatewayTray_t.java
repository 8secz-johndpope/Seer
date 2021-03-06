 package davmail.tray;
 
import davmail.Settings;
import davmail.SettingsFrame;
 import org.apache.log4j.Logger;
 import org.apache.log4j.Priority;
 import org.apache.log4j.lf5.LF5Appender;
 import org.apache.log4j.lf5.LogLevel;
 import org.apache.log4j.lf5.viewer.LogBrokerMonitor;
 import org.eclipse.swt.SWT;
 import org.eclipse.swt.graphics.Image;
 import org.eclipse.swt.widgets.*;
 import org.eclipse.swt.widgets.Event;
 import org.eclipse.swt.widgets.Menu;
 import org.eclipse.swt.widgets.MenuItem;
 
 import javax.swing.*;
import java.awt.*;
 import java.io.IOException;
 import java.net.URL;
 
 /**
  * Tray icon handler based on SWT
  */
 public class SwtGatewayTray implements DavGatewayTrayInterface {
     protected SwtGatewayTray() {
     }
 
     protected static final Logger LOGGER = Logger.getLogger("davmail");
 
     // LOCK for synchronized block
     protected static final Object LOCK = new Object();
 
     private static TrayItem trayItem = null;
     private static Image image = null;
     private static Image image2 = null;
     private static Display display;
     private static Shell shell;
 
     public void switchIcon() {
         display.syncExec(
                 new Runnable() {
                     public void run() {
                         if (trayItem.getImage() == image) {
                             trayItem.setImage(image2);
                         } else {
                             trayItem.setImage(image);
                         }
                     }
                 });
 
     }
 
     public void resetIcon() {
         display.syncExec(
                 new Runnable() {
                     public void run() {
                         trayItem.setImage(image);
                     }
                 });
     }
 
     public void displayMessage(final String message, final Priority priority) {
         synchronized (LOCK) {
             if (trayItem != null) {
                 display.syncExec(
                         new Runnable() {
                             public void run() {
                                 int messageType = 0;
                                 if (priority == Priority.INFO) {
                                     messageType = SWT.ICON_INFORMATION;
                                 } else if (priority == Priority.WARN) {
                                     messageType = SWT.ICON_WARNING;
                                 } else if (priority == Priority.ERROR) {
                                     messageType = SWT.ICON_ERROR;
                                 }
                                 if (messageType == 0) {
                                     trayItem.setToolTipText("DavMail gateway \n" + message);
                                 } else {
                                     final ToolTip toolTip = new ToolTip(shell, SWT.BALLOON | messageType);
                                     toolTip.setText("DavMail gateway");
                                     toolTip.setMessage(message);
                                     trayItem.setToolTip(toolTip);
                                     toolTip.setVisible(true);
                                 }
                             }
                         });
             }
             LOGGER.log(priority, message);
         }
 
     }
 
     public void init() {
         // set native look and feel
         try {
             String lafClassName = UIManager.getSystemLookAndFeelClassName();
             // workaround for bug when SWT and AWT both try to access Gtk
             if (lafClassName.indexOf("gtk") > 0) {
                 lafClassName = UIManager.getCrossPlatformLookAndFeelClassName();
             }
             UIManager.setLookAndFeel(lafClassName);
         } catch (Exception e) {
             LOGGER.warn("Unable to set look and feel");
         }
 
         new Thread() {
             public void run() {
                 display = new Display();
                 shell = new Shell(display);
 
                 final Tray tray = display.getSystemTray();
                 if (tray != null) {
 
                     trayItem = new TrayItem(tray, SWT.NONE);
                     trayItem.setToolTipText("DavMail gateway");
 
                     // load an image
                     ClassLoader classloader = DavGatewayTray.class.getClassLoader();
                     java.awt.Image awtImage = null;
                     try {
                         URL imageUrl = classloader.getResource("tray.png");
                         image = new Image(display, imageUrl.openStream());
                         awtImage = Toolkit.getDefaultToolkit().getImage(imageUrl);
                     } catch (IOException e) {
                         LOGGER.warn("Unable to load image");
                     }
 
                     try {
                         URL imageUrl2 = classloader.getResource("tray2.png");
                         image2 = new Image(display, imageUrl2.openStream());
                     } catch (IOException e) {
                         LOGGER.warn("Unable to load image");
                     }
 
                     trayItem.setImage(image);
 
                     // create a popup menu
                     final Menu popup = new Menu(shell, SWT.POP_UP);
                     trayItem.addListener(SWT.MenuDetect, new Listener() {
                         public void handleEvent(Event event) {
                             popup.setVisible(true);
                         }
                     });
 
                     final SettingsFrame settingsFrame = new SettingsFrame();
                     if (awtImage != null) {
                         settingsFrame.setIconImage(awtImage);
                     }
                     trayItem.addListener(SWT.DefaultSelection, new Listener() {
                         public void handleEvent(Event event) {
                             display.asyncExec(
                                     new Runnable() {
                                         public void run() {
                                             settingsFrame.setVisible(true);
                                         }
                                     });
                         }
                     });
 
                     // create menu item for the default action
                     MenuItem defaultItem = new MenuItem(popup, SWT.PUSH);
                     defaultItem.setText("Settings...");
                     defaultItem.addListener(SWT.Selection, new Listener() {
                         public void handleEvent(Event event) {
                             settingsFrame.setVisible(true);
                         }
                     });
 
                     MenuItem logItem = new MenuItem(popup, SWT.PUSH);
                     logItem.setText("Show logs...");
                     logItem.addListener(SWT.Selection, new Listener() {
                         public void handleEvent(Event event) {
                             display.asyncExec(
                                     new Runnable() {
                                         public void run() {
 
                                             Logger rootLogger = Logger.getRootLogger();
                                             LF5Appender lf5Appender = (LF5Appender) rootLogger.getAppender("LF5Appender");
                                             if (lf5Appender == null) {
                                                 lf5Appender = new LF5Appender(new LogBrokerMonitor(LogLevel.getLog4JLevels()) {
                                                     protected void closeAfterConfirm() {
                                                         hide();
                                                     }
                                                 });
                                                 lf5Appender.setName("LF5Appender");
                                                 rootLogger.addAppender(lf5Appender);
                                             }
                                             lf5Appender.getLogBrokerMonitor().show();
                                         }
                                     });
                         }
                     });
 
                     MenuItem exitItem = new MenuItem(popup, SWT.PUSH);
                     exitItem.setText("Exit");
                     exitItem.addListener(SWT.Selection, new Listener() {
                         public void handleEvent(Event event) {
                             shell.dispose();
 
                             if (image != null)
                                 image.dispose();
                             if (image2 != null)
                                 image2.dispose();
                             display.dispose();
 
                             //noinspection CallToSystemExit
                             System.exit(0);
                         }
                     });
 
                     // display settings frame on first start
                     if (Settings.isFirstStart()) {
                         settingsFrame.setVisible(true);
                     }
 
                     while (!shell.isDisposed()) {
                         if (!display.readAndDispatch())
                             display.sleep();
                     }
 
                     if (image != null)
                         image.dispose();
                     if (image2 != null)
                         image2.dispose();
                     display.dispose();
                 }
             }
         }.start();
     }
 }
