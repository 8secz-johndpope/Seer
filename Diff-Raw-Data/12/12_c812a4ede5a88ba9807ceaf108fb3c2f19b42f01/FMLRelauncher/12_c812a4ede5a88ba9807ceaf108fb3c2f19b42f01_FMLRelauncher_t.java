 package cpw.mods.fml.relauncher;
 
 import java.applet.Applet;
 import java.applet.AppletStub;
 import java.awt.Dialog.ModalityType;
 import java.awt.HeadlessException;
 import java.io.File;
 import java.io.IOException;
 import java.io.InputStream;
 import java.lang.reflect.Field;
 import java.lang.reflect.InvocationTargetException;
 import java.lang.reflect.Method;
 import java.net.URLClassLoader;
 import java.util.Arrays;
 import java.util.Properties;
 import java.util.logging.Level;
 
 import javax.swing.JDialog;
 import javax.swing.JFrame;
 import javax.swing.JLabel;
 import javax.swing.JOptionPane;
 import javax.swing.ProgressMonitorInputStream;
 
 public class FMLRelauncher
 {
     private static FMLRelauncher INSTANCE;
     public static String logFileNamePattern;
     private static String side;
     private RelaunchClassLoader classLoader;
     private Object newApplet;
     private Class<? super Object> appletClass;
 
     JDialog popupWindow;
 
     public static void handleClientRelaunch(ArgsWrapper wrap)
     {
         logFileNamePattern = "ForgeModLoader-client-%g.log";
         side = "CLIENT";
         instance().relaunchClient(wrap);
     }
 
     public static void handleServerRelaunch(ArgsWrapper wrap)
     {
         logFileNamePattern = "ForgeModLoader-server-%g.log";
         side = "SERVER";
         instance().relaunchServer(wrap);
     }
 
     static FMLRelauncher instance()
     {
         if (INSTANCE == null)
         {
             INSTANCE = new FMLRelauncher();
         }
         return INSTANCE;
 
     }
 
     private FMLRelauncher()
     {
         URLClassLoader ucl = (URLClassLoader)getClass().getClassLoader();
 
         classLoader = new RelaunchClassLoader(ucl.getURLs());
 
     }
     private void showWindow()
     {
         try
         {
             popupWindow = new JDialog(null, "FML Initial Setup", ModalityType.MODELESS);
             JOptionPane optPane = new JOptionPane("<html><font size=\"+1\">FML Setup</font><br/><br/>FML is performing some configuration, please wait</html>", JOptionPane.INFORMATION_MESSAGE, JOptionPane.DEFAULT_OPTION, null, new Object[0]);
             popupWindow.add(optPane);
             popupWindow.pack();
             popupWindow.setLocationRelativeTo(null);
             popupWindow.setVisible(true);
         }
         catch (Exception e)
         {
             popupWindow = null;
         }
     }
 
     private void relaunchClient(ArgsWrapper wrap)
     {
         showWindow();
         // Now we re-inject the home into the "new" minecraft under our control
         Class<? super Object> client;
         try
         {
             File minecraftHome = computeExistingClientHome();
             setupHome(minecraftHome);
 
             client = ReflectionHelper.getClass(classLoader, "net.minecraft.client.Minecraft");
             ReflectionHelper.setPrivateValue(client, null, minecraftHome, "field_71463_am", "am", "minecraftDir");
         }
         finally
         {
             if (popupWindow!=null)
             {
                 popupWindow.setVisible(false);
                 popupWindow.dispose();
             }
         }
 
         try
         {
             ReflectionHelper.findMethod(client, null, new String[] { "fmlReentry" }, ArgsWrapper.class).invoke(null, wrap);
         }
         catch (Exception e)
         {
             e.printStackTrace();
             // Hmmm
         }
     }
 
     private void relaunchServer(ArgsWrapper wrap)
     {
         // Now we re-inject the home into the "new" minecraft under our control
         Class<? super Object> server;
             File minecraftHome = new File(".");
             setupHome(minecraftHome);
 
             server = ReflectionHelper.getClass(classLoader, "net.minecraft.server.MinecraftServer");
         try
         {
             ReflectionHelper.findMethod(server, null, new String[] { "fmlReentry" }, ArgsWrapper.class).invoke(null, wrap);
         }
         catch (Exception e)
         {
             e.printStackTrace();
             // Hmmm
         }
     }
 
     private void setupHome(File minecraftHome)
     {
         FMLInjectionData.build(minecraftHome, classLoader);
         FMLRelaunchLog.info("Forge Mod Loader version %s.%s.%s.%s for Minecraft client:%s, server:%s loading", FMLInjectionData.major, FMLInjectionData.minor, FMLInjectionData.rev, FMLInjectionData.build, FMLInjectionData.mccversion, FMLInjectionData.mcsversion);
 
         try
         {
             RelaunchLibraryManager.handleLaunch(minecraftHome, classLoader);
         }
         catch (Throwable t)
         {
             if (popupWindow != null)
             {
                 try
                 {
                     String logFile = new File(minecraftHome,"ForgeModLoader-0.log").getCanonicalPath();
                     JOptionPane.showMessageDialog(popupWindow,
                             String.format("<html><div align=\"center\"><font size=\"+1\">There was a fatal error starting up minecraft and FML</font></div><br/>" +
                             		"Minecraft cannot launch in it's current configuration<br/>" +
                             		"Please consult the file <i><a href=\"file:///%s\">%s</a></i> for further information</html>", logFile, logFile
                             		), "Fatal FML error", JOptionPane.ERROR_MESSAGE);
                 }
                 catch (Exception ex)
                 {
                     // ah well, we tried
                 }
             }
             throw new RuntimeException(t);
         }
     }
 
     /**
      * @return
      */
     private File computeExistingClientHome()
     {
         Class<? super Object> mcMaster = ReflectionHelper.getClass(getClass().getClassLoader(), "net.minecraft.client.Minecraft");
         // We force minecraft to setup it's homedir very early on so we can inject stuff into it
         Method setupHome = ReflectionHelper.findMethod(mcMaster, null, new String[] { "func_71380_b", "getMinecraftDir", "b"} );
         try
         {
             setupHome.invoke(null);
         }
         catch (Exception e)
         {
             // Hmmm
         }
         File minecraftHome = ReflectionHelper.getPrivateValue(mcMaster, null, "field_71463_am", "am", "minecraftDir");
         return minecraftHome;
     }
 
     public static void appletEntry(Applet minecraftApplet)
     {
         side = "CLIENT";
         logFileNamePattern = "ForgeModLoader-client-%g.log";
         instance().relaunchApplet(minecraftApplet);
     }
 
     private void relaunchApplet(Applet minecraftApplet)
     {
         appletClass = ReflectionHelper.getClass(classLoader, "net.minecraft.client.MinecraftApplet");
         if (minecraftApplet.getClass().getClassLoader() == classLoader)
         {
             try
             {
                 newApplet = minecraftApplet;
                 ReflectionHelper.findMethod(appletClass, newApplet, new String[] {"fmlInitReentry"}).invoke(newApplet);
                 return;
             }
             catch (Exception e)
             {
                System.out.println("FMLRelauncher.relaunchApplet");
                e.printStackTrace();
                 throw new RuntimeException(e);
             }
         }
 
         setupHome(computeExistingClientHome());
 
         Class<? super Object> parentAppletClass = ReflectionHelper.getClass(getClass().getClassLoader(), "java.applet.Applet");
 
         try
         {
             newApplet = appletClass.newInstance();
             Object appletContainer = ReflectionHelper.getPrivateValue(ReflectionHelper.getClass(getClass().getClassLoader(), "java.awt.Component"), minecraftApplet, "parent");
 
             Class<? super Object> launcherClass = ReflectionHelper.getClass(getClass().getClassLoader(), "net.minecraft.Launcher");
             if (launcherClass.isInstance(appletContainer))
             {
                 ReflectionHelper.findMethod(ReflectionHelper.getClass(getClass().getClassLoader(), "java.awt.Container"), minecraftApplet, new String[] { "removeAll" }).invoke(appletContainer);
                 ReflectionHelper.findMethod(launcherClass, appletContainer, new String[] { "replace" }, parentAppletClass).invoke(appletContainer, newApplet);
             }
             else
             {
                 FMLRelaunchLog.severe("Found unknown applet parent %s, unable to inject!\n", launcherClass);
                 throw new RuntimeException();
             }
         }
         catch (Exception e)
         {
             throw new RuntimeException(e);
         }
         finally
         {
             if (popupWindow!=null)
             {
                 popupWindow.setVisible(false);
                 popupWindow.dispose();
             }
         }
     }
 
     public static void appletStart(Applet applet)
     {
         instance().startApplet(applet);
     }
 
     private void startApplet(Applet applet)
     {
         if (applet.getClass().getClassLoader() == classLoader)
         {
             try
             {
                 ReflectionHelper.findMethod(appletClass, newApplet, new String[] {"fmlStartReentry"}).invoke(newApplet);
             }
             catch (Exception e)
             {
                System.out.println("FMLRelauncher.startApplet");
                e.printStackTrace();
                 throw new RuntimeException(e);
             }
         }
         return;
     }
 
     public InputStream wrapStream(InputStream inputStream, String infoString)
     {
         if (popupWindow!=null)
         {
             return new ProgressMonitorInputStream(popupWindow, infoString, inputStream);
         }
         else
         {
             return inputStream;
         }
     }
 
     public static String side()
     {
         return side;
     }
 }
