 /*
  * @(#)LookAndFeelFactory.java 5/28/2005
  *
  * Copyright 2002 - 2005 JIDE Software Inc. All rights reserved.
  */
 
 package com.jidesoft.plaf;
 
 import com.jidesoft.icons.IconsFactory;
 import com.jidesoft.plaf.basic.Painter;
 import com.jidesoft.plaf.eclipse.Eclipse3xMetalUtils;
 import com.jidesoft.plaf.eclipse.Eclipse3xWindowsUtils;
 import com.jidesoft.plaf.eclipse.EclipseMetalUtils;
 import com.jidesoft.plaf.eclipse.EclipseWindowsUtils;
 import com.jidesoft.plaf.office2003.Office2003Painter;
 import com.jidesoft.plaf.office2003.Office2003WindowsUtils;
 import com.jidesoft.plaf.vsnet.VsnetMetalUtils;
 import com.jidesoft.plaf.vsnet.VsnetWindowsUtils;
 import com.jidesoft.plaf.xerto.XertoMetalUtils;
 import com.jidesoft.plaf.xerto.XertoWindowsUtils;
 import com.jidesoft.swing.JideSwingUtilities;
 import com.jidesoft.swing.JideTabbedPane;
 import com.jidesoft.utils.ProductNames;
 import com.jidesoft.utils.SecurityUtils;
 import com.jidesoft.utils.SystemInfo;
 import com.sun.java.swing.plaf.windows.WindowsLookAndFeel;
 
 import javax.swing.*;
 import javax.swing.plaf.BorderUIResource;
 import javax.swing.plaf.ColorUIResource;
 import javax.swing.plaf.metal.MetalLookAndFeel;
 import java.awt.*;
 import java.lang.reflect.InvocationTargetException;
 import java.util.List;
 import java.util.Vector;
 
 /**
  * JIDE Software created many new components that need their own ComponentUI classes and additional UIDefaults in UIDefaults table.
  * LookAndFeelFactory can take the UIDefaults from any existing look and feel
  * and add the extra UIDefaults JIDE components need.
  * <p/>
  * Before using any JIDE components, please make you call one of the two LookAndFeelFactory.installJideExtension(...) methods.
  * Bascially, you set L&F using UIManager first just like before, then call installJideExtension. See code below for an example.
  * <code><pre>
  * UIManager.setLookAndFeel(WindowsLookAndFeel.class.getName()); // you need to catch the exceptions on this call.
  * LookAndFeelFactory.installJideExtension();
  * </pre></code>
  * LookAndFeelFactory.installJideExtension() method will check what kind of L&F you set and what operating system you are on and
  * decide which style of JIDE extension it will install. Here is the rule.
  * <ul>
  * <li> OS: Windows XP with XP theme on, L&F: Windows L&F => OFFICE2003_STYLE
  * <li> OS: any Windows, L&F: Windows L&F => VSNET_STYLE
  * <li> OS: Linux, L&F: any L&F based on Metal L&F => VSNET_STYLE
  * <li> OS: Mac OSX, L&F: Aqua L&F => AQUA_STYLE
  * <li> OS: any OS, L&F: Quaqua L&F => AQUA_STYLE
  * <li> Otherwise => VSNET_STYLE
  * </ul>
  * There is also another installJideExtension which takes an int style parameter. You can pass in {@link #VSNET_STYLE},
  * {@link #ECLIPSE_STYLE}, {@link #ECLIPSE3X_STYLE}, {@link #OFFICE2003_STYLE}, or {@link #XERTO_STYLE}. In the other word,
  * you will make the choice of style instead of letting LookAndFeelFactory to decide one for you. Please note, there is no constant defined for
  * AQUA_STYLE. The only way to use it is when you are using Aqua L&F or Quaqua L&F and you call installJideExtension() method,
  * the one without parameter.
  * <p/>
  * LookAndFeelFactory supports a number of known L&Fs. You can see those L&Fs as constants whose names are
  * something like "_LNF" such as WINDOWS_LNF.
  * <p/>
  * If you are using a 3rd party L&F we are not officially supporting, we might need to customize it. Here are two classes you can use.
  * The first one is {@link UIDefaultsCustomizer}. You can add a number of customizers to LookAndFeelFactory. After LookAndFeelFactory
  * installJideExtension method is called, we will call customize() method on each UIDefaultsCustomizer to add additional UIDefaults
  * you specified. You can use UIDefaultsCustomizer to do things like small tweaks to UIDefaults without the hassle of creating a new style.
  * <p/>
  * Most likely, we will not need to use {@link UIDefaultsInitializer} if you are use L&Fs such as WindowsLookAndFeel,
  * any L&Fs based on MetalLookAndFeel, or AquaLookAndFeel etc. The only exception is Synth L&F and any L&Fs based on it. The reason is we
  * calcualte all colors we will use in JIDE components from existing wel-known UIDefaults. For example, we will use UIManagerLookup.getColor("activeCaption")
  * to calculate a color that we can use in dockable frame's title pane. We will use UIManagerLookup.getColor("control") to calculate a color that
  * we can use as background of JIDE component. Most L&Fs will fill those UIDefaults. However in Synth L&F, those UIDefaults may or may not
  * have a valid value. You will end up with NPE later in the code when you call installJideExtension. In this case, you can add those extra UIDefaults
  * in UIDefaultsInitializer. We will call it before installJideExtension is called so that those UIDefaults are there ready for us to use.
  * This is how added support to GTK L&F and Synthethica L&F.
  * <p/>
  * {@link #installJideExtension()} method will only add the additional UIDefaults to current ClassLoader. If you have several class loaders in your system,
  * you probably should tell the UIManager to use the class loader that called <code>installJideExtension</code>. Otherwise, you might some unexpected errors.
  * Here is how to specify the class loaders.
  * <code><pre>
  * UIManager.put("ClassLoader", currentClass.getClassLoader()); // currentClass is the class where the code is.
  * LookAndFeelFactory.installDefaultLookAndFeelAndExtension(); // or installJideExtension()
  * </pre></code>
  */
 public class LookAndFeelFactory implements ProductNames {
 
     /**
      * Class name of Windows L&F provided in Sun JDK.
      */
     public static final String WINDOWS_LNF = "com.sun.java.swing.plaf.windows.WindowsLookAndFeel";
 
     /**
      * Class name of Metal L&F provided in Sun JDK.
      */
     public static final String METAL_LNF = "javax.swing.plaf.metal.MetalLookAndFeel";
 
     /**
      * Class name of Aqua L&F provided in Apple Mac OSX JDK.
      */
     public static final String AQUA_LNF = "apple.laf.AquaLookAndFeel";
 
     /**
      * Class name of Quaqua L&F.
      */
     public static final String QUAQUA_LNF = "ch.randelshofer.quaqua.QuaquaLookAndFeel";
 
     /**
      * Class name of Quaqua Alloy L&F.
      */
     public static final String ALLOY_LNF = "com.incors.plaf.alloy.AlloyLookAndFeel";
 
     /**
      * Class name of Synthetica L&F.
      */
     public static final String SYNTHETICA_LNF = "de.javasoft.plaf.synthetica.SyntheticaStandardLookAndFeel";
 
     private static final String SYNTHETICA_LNF_PREFIX = "de.javasoft.plaf.synthetica.Synthetica";
 
     /**
      * Class name of Plastic3D L&F before JGoodies Look 1.3 release.
      */
     public static final String PLASTIC3D_LNF = "com.jgoodies.plaf.plastic.Plastic3DLookAndFeel";
 
     /**
      * Class name of Plastic3D L&F after JGoodies Look 1.3 release.
      */
     public static final String PLASTIC3D_LNF_1_3 = "com.jgoodies.looks.plastic.Plastic3DLookAndFeel";
 
     /**
      * Class name of PlasticXP L&F.
      */
     public static final String PLASTICXP_LNF = "com.jgoodies.looks.plastic.PlasticXPLookAndFeel";
 
     /**
      * Class name of Tonic L&F.
      */
     public static final String TONIC_LNF = "com.digitprop.tonic.TonicLookAndFeel";
 
     /**
      * Class name of A03 L&F.
      */
    public static final String A03_LNF = "a03.swing.plaf.A03LookAndFeel";
 
     /**
      * Class name of Pgs L&F.
      */
     public static final String PGS_LNF = "com.pagosoft.plaf.PgsLookAndFeel";
 
     /**
      * Class name of GTK L&F provided by Sun JDK.
      */
     public static final String GTK_LNF = "com.sun.java.swing.plaf.gtk.GTKLookAndFeel";
 
     /**
      * A style that you can use with {@link #installJideExtension(int)} method. This style is the same as VSNET_STYLE
      * except it doesn't have menu related UIDefaults. You can only use this style if you didn't use any component from JIDE Action Framework.
      * <p/>
      *
      * @see #VSNET_STYLE
      */
     public final static int VSNET_STYLE_WITHOUT_MENU = 0;
 
     /**
      * A style that you can use with {@link #installJideExtension(int)} method.
      * This style mimics the visual style of Microsoft Visuasl Studio .NET for the toolbars, menus and dockable windows.
      * <p/>
      * Vsnet style is a very simple style with no gradient. Although it works on almost all L&Fs in any operating systems, it looks the best
      * on Windows 2000 or 98, or on Windows XP when XP theme is not on. If XP theme is on, we suggest you use Office2003 style or Xerto style.
      * Since the style is so simple, it works with a lot of the 3rd party L&F such as Tonic, Pgs, Alloy etc without causing too much noice.
      * That's why this is also the default style for any L&Fs we don't recognize when you call {@link #installJideExtension()},
      * the one with out style parameter. If you would like another style to be used as the default style, you can call {@link #setDefaultStyle(int)} method.
      * <p/>
      * Here is the code to set to Windows L&F with Vsnet style extension.
      * <code><pre>
      * UIManager.setLookAndFeel(WindowsLookAndFeel.class.getName()); // you need to catch the exceptions on this call.
      * LookAndFeelFactory.installJideExtension(LookAndFeelFactory.VSNET_STYLE);
      * </pre></code>
      * There is a special system property "shading theme" you can use. If you turn it on using the code below, you will see
      * a graident on dockable frame's title pane and rounded corner and graident on the tabs of JideTabbedPane.
      * So if the L&F you are using uses graident, you can set this property to true to match with your L&F. For example, if you use
      * Plastic3D L&F, turning this property on will look better.
      * <code><pre>
      * System.setProperty("shadingtheme", "true");
      * </pre></code>
      */
     public final static int VSNET_STYLE = 1;
 
     /**
      * A style that you can use with {@link #installJideExtension(int)} method.
      * This style mimics the visual style of Eclipse 2.x for the toolbars, menus and dockable windows.
      * <p/>
      * Eclipse style works for almost all L&Fs and on any operating systems, although it looks the best on Windows.
      * For any other operating systems we suggest you to use XERTO_STYLE or VSNET_STYLE.
      * <p/>
      * Here is the code to set to any L&F with Eclipse style extension.
      * <code><pre>
      * UIManager.setLookAndFeel(AnyLookAndFeel.class.getName()); // you need to catch the exceptions on this call.
      * LookAndFeelFactory.installJideExtension(LookAndFeelFactory.ECLIPSE_STYLE);
      * </pre></code>
      */
     public final static int ECLIPSE_STYLE = 2;
 
     /**
      * A style that you can use with {@link #installJideExtension(int)} method.
      * This style mimics the visual style of Microsoft Office2003 for the toolbars, menus and dockable windows.
      * <p/>
      * Office2003 style looks great on Windows XP when Windows or Windows XP L&F from Sun JDK is used. It replicated
      * the exact same style as Microsoft Office 2003, to give your end user a familar visual style.
      * <p/>
      * Here is the code to set to Windows L&F with Office2003 style extension.
      * <code><pre>
      * UIManager.setLookAndFeel(WindowsLookAndFeel.class.getName()); // you need to catch the exceptions on this call.
      * LookAndFeelFactory.installJideExtension(LookAndFeelFactory.OFFICE2003_STYLE);
      * </pre></code>
      * It works either on any other Windows such as Winodows 2000, Windows 98 etc.
      * If you are on Windows XP, Office2003 style will change theme based on the theme setting in Windows Display Property.
      * But if you are not on XP, Office2003 style will use the default gray theme only. You can force to change it using
      * {@link Office2003Painter#setColorName(String)} method, but it won't look good as other non-JIDE components won't have
      * the matching theme.
      * <p/>
      * Office2003 style doesn't work on any operating systems other than Windows mainly because the design of Office2003 style is so
      * centric to Windows that it doesn't look good on other operating systems.
      */
     public final static int OFFICE2003_STYLE = 3;
 
     /**
      * A style that you can use with {@link #installJideExtension(int)} method.
      * This style is created by Xerto (http://www.xerto.com) which is used in their Imagery product.
      * <p/>
      * Xerto style looks great on Windows XP when Windows XP L&F from Sun JDK is used.
      * <p/>
      * Here is the code to set to Windows L&F with Xerto style extension.
      * <code><pre>
      * UIManager.setLookAndFeel(WindowsLookAndFeel.class.getName()); // you need to catch the exceptions on this call.
      * LookAndFeelFactory.installJideExtension(LookAndFeelFactory.XERTO_STYLE);
      * </pre></code>
      * Although it looks the best on Windows, Xerto style also supports Linux or Solaris if you use any L&Fs based
      * on Metal L&F or Synth L&F. For example, we recommend you to use Xerto style as default if you use SyntheticaL&F, a L&F based on Synth.
      * To use it, you bascially replace WindowsLookAndFeel to the L&F you want to use in setLookAndFeel line above.
      */
     public final static int XERTO_STYLE = 4;
 
     /**
      * A style that you can use with {@link #installJideExtension(int)} method. This style is the same as XERTO_STYLE
      * except it doesn't have menu related UIDefaults. You can only use this style if you didn't use any component from JIDE Action Framework.
      * Please note, we only use menu extension for Xerto style when the underlying L&F is Windows L&F. If you are using L&F such as Metal or other 3rd party L&F based on Metal,
      * XERTO_STYLE_WITHOUT_MENU will be used even you use XERTO_STYLE when calling to installJideExtension().
      * <p/>
      *
      * @see #XERTO_STYLE
      */
     public final static int XERTO_STYLE_WITHOUT_MENU = 6;
 
     /**
      * A style that you can use with {@link #installJideExtension(int)} method.
      * This style mimics the visual style of Eclipse 3.x for the toolbars, menus and dockable windows.
      * <p/>
      * Eclipse 3x style works for almost all L&Fs and on any operating systems, although it looks the best on Windows.
      * For any other OS's we suggest you to use XERTO_STYLE or VSNET_STYLE.
      * <code><pre>
      * UIManager.setLookAndFeel(AnyLookAndFeel.class.getName()); // you need to catch the exceptions on this call.
      * LookAndFeelFactory.installJideExtension(LookAndFeelFactory.ECLIPSE3X_STYLE);
      * </pre></code>
      */
     public final static int ECLIPSE3X_STYLE = 5;
 
     private static int _style = -1;
     private static int _defaultStyle = -1;
     private static LookAndFeel _lookAndFeel;
 
     /**
      * If installJideExtension is called, it will put an entry on UIDefaults table. UIManagerLookup.getBoolean(JIDE_EXTENSION_INSTALLLED) will
      * return true. You can also use {@link #isJideExtensionInstalled()} to check the value instead of using UIManagerLookup.getBoolean(JIDE_EXTENSION_INSTALLLED).
      */
     public final static String JIDE_EXTENSION_INSTALLLED = "jidesoft.extendsionInstalled";
 
     /**
      * If installJideExtension is called, a JIDE style will be installed on UIDefaults table. If so, UIManagerLookup.getInt(JIDE_STYLE_INSTALLED)
      * will return you the style that is installed. For example, if the value is 1, it means VSNET_STYLE is installed because 1 is the value of VSNET_STYLE.
      */
     public final static String JIDE_STYLE_INSTALLED = "jidesoft.extendsionStyle";
 
     /**
      * @deprecated JIDE_STYLE name is confusing because people think this is a special style such as VSNET_STYLE or OFFICE2003_STYLE. So we decided
      *             to rename it to  {@link #JIDE_STYLE_INSTALLED}. If you used this before, please change it to use JIDE_STYLE_INSTALLED instead.
      */
     public final static String JIDE_STYLE = JIDE_STYLE_INSTALLED;
 
     /**
      * An interface to make the customization of UIDefaults easier. This customizer will be called after installJideExtension()
      * is called. So if you want to further customize UIDefault, you can use this customzier to do it.
      */
     public static interface UIDefaultsCustomizer {
         void customize(UIDefaults defaults);
     }
 
     /**
      * An interface to make the initialization of UIDefaults easier. This initializer will be called before installJideExtension()
      * is called. So if you want to initialize UIDefault before installJideExtension is called, you can use this initializer to do it.
      */
     public static interface UIDefaultsInitializer {
         void initialize(UIDefaults defaults);
     }
 
     private static List<UIDefaultsCustomizer> _uiDefaultsCustomizers = new Vector();
     private static List<UIDefaultsInitializer> _uiDefaultsInitializers = new Vector();
 
     protected LookAndFeelFactory() {
     }
 
     /**
      * Gets the default style. If you never set default style before, it will return OFFICE2003_STYLE
      * if you are on Windows XP, L&F is instance of Windows L&F and XP theme is on. Otherwise, it will return VSNET_STYLE.
      * If you set default style before, it will return whatever style you set.
      *
      * @return the default style.
      */
     public static int getDefaultStyle() {
         if (_defaultStyle == -1) {
             int suggestedStyle;
             try {
                 if (XPUtils.isXPStyleOn() && UIManager.getLookAndFeel() instanceof WindowsLookAndFeel) {
                     suggestedStyle = OFFICE2003_STYLE;
                 }
                 else {
                     suggestedStyle = VSNET_STYLE;
                 }
             }
             catch (UnsupportedOperationException e) {
                 suggestedStyle = VSNET_STYLE;
             }
             return suggestedStyle;
         }
         return _defaultStyle;
     }
 
     /**
      * Sets the default style. If you call this method to set a default style, {@link #installJideExtension()} will
      * use it as the default style.
      *
      * @param defaultStyle the default style.
      */
     public static void setDefaultStyle(int defaultStyle) {
         _defaultStyle = defaultStyle;
     }
 
     /**
      * Adds additional UIDefaults JIDE needed to UIDefault table. You must call this method
      * everytime switching look and feel. And callupdateComponentTreeUI() in corresponding DockingManager
      * or DockableBarManager after this call.
      * <pre><code>
      *  try {
      *      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      *  }
      *  catch (ClassNotFoundException e) {
      *     e.printStackTrace();
      *  }
      *  catch (InstantiationException e) {
      *     e.printStackTrace();
      *  }
      *  catch (IllegalAccessException e) {
      *      e.printStackTrace();
      *  }
      *  catch (UnsupportedLookAndFeelException e) {
      *      e.printStackTrace();
      *  }
      * <p/>
      *  // to add attitional UIDefault for JIDE components
      *  LookAndFeelFactory.installJideExtension(); // use default style VSNET_STYLE. You can change to a different style
      * using setDefaultStyle(int style) and then call this method. Or simply call installJideExtension(style).
      * <p/>
      *  // call updateComponentTreeUI
      *  frame.getDockableBarManager().updateComponentTreeUI();
      *  frame.getDockingManager().updateComponentTreeUI();
      * </code></pre>
      */
     public static void installJideExtension() {
         installJideExtension(getDefaultStyle());
     }
 
     /**
      * Add additional UIDefaults JIDE needed to UIDefaults table. You must call this method
      * everytime switching look and feel. And call updateComponentTreeUI() in corresponding DockingManager
      * or DockableBarManager after this call.
      * <pre><code>
      *  try {
      *      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      *  }
      *  catch (ClassNotFoundException e) {
      *     e.printStackTrace();
      *  }
      *  catch (InstantiationException e) {
      *     e.printStackTrace();
      *  }
      *  catch (IllegalAccessException e) {
      *      e.printStackTrace();
      *  }
      *  catch (UnsupportedLookAndFeelException e) {
      *      e.printStackTrace();
      *  }
      * <p/>
      *  // to add attitional UIDefault for JIDE components
      *  LookAndFeelFactory.installJideExtension(LookAndFeelFactory.OFFICE2003_STYLE);
      * <p/>
      *  // call updateComponentTreeUI
      *  frame.getDockableBarManager().updateComponentTreeUI();
      *  frame.getDockingManager().updateComponentTreeUI();
      * </code></pre>
      *
      * @param style the style of the extension.
      */
     public static void installJideExtension(int style) {
         installJideExtension(UIManager.getLookAndFeelDefaults(), UIManager.getLookAndFeel(), style);
     }
 
     /**
      * Checks if JIDE extension is installed. Please note, UIManager.setLookAndFeel() method will
      * overwrite the whole UIDefaults table. So even you called {@link #installJideExtension()} method before,
      * UIManager.setLookAndFeel() method make isJideExtensionInstalled returning false.
      *
      * @return true if installed.
      */
     public static boolean isJideExtensionInstalled() {
         return UIDefaultsLookup.getBoolean(JIDE_EXTENSION_INSTALLLED);
     }
 
     /**
      * Installs the UIDefault needed by JIDE component to the uiDefaults table passed in.
      *
      * @param uiDefaults the UIDefault tables where JIDE UIDefaults will be installed.
      * @param lnf        the LookAndFeel. This may have an effect on which set of JIDE UIDefaults we will install.
      * @param style      the style of the JIDE UIDefaults.
      */
     public static void installJideExtension(UIDefaults uiDefaults, LookAndFeel lnf, int style) {
         if (isJideExtensionInstalled() && _style == style && _lookAndFeel == lnf) {
             return;
         }
 
         _style = style;
         uiDefaults.put(JIDE_STYLE_INSTALLED, _style);
 
         _lookAndFeel = lnf;
 
         UIDefaultsInitializer[] initializers = getUIDefaultsInitializers();
         for (UIDefaultsInitializer initializer : initializers) {
             if (initializer != null) {
                 initializer.initialize(uiDefaults);
             }
         }
 
         // For Alloy
 /*        if (lnf.getClass().getName().equals(ALLOY_LNF) && isAlloyLnfInstalled()) {
             Object progressBarUI = uiDefaults.get("ProgressBarUI");
             VsnetMetalUtils.initClassDefaults(uiDefaults);
             VsnetMetalUtils.initComponentDefaults(uiDefaults);
             uiDefaults.put("ProgressBarUI", progressBarUI);
             uiDefaults.put("DockableFrameUI", "com.jidesoft.plaf.vsnet.VsnetDockableFrameUI");
             uiDefaults.put("DockableFrameTitlePane.hideIcon", IconsFactory.getIcon(null, titleButtonImage, 0, 0, titleButtonSize, titleButtonSize));
             uiDefaults.put("DockableFrameTitlePane.unfloatIcon", IconsFactory.getIcon(null, titleButtonImage, 0, titleButtonSize, titleButtonSize, titleButtonSize));
             uiDefaults.put("DockableFrameTitlePane.floatIcon", IconsFactory.getIcon(null, titleButtonImage, 0, 2 * titleButtonSize, titleButtonSize, titleButtonSize));
             uiDefaults.put("DockableFrameTitlePane.autohideIcon", IconsFactory.getIcon(null, titleButtonImage, 0, 3 * titleButtonSize, titleButtonSize, titleButtonSize));
             uiDefaults.put("DockableFrameTitlePane.stopAutohideIcon", IconsFactory.getIcon(null, titleButtonImage, 0, 4 * titleButtonSize, titleButtonSize, titleButtonSize));
             uiDefaults.put("DockableFrameTitlePane.hideAutohideIcon", IconsFactory.getIcon(null, titleButtonImage, 0, 5 * titleButtonSize, titleButtonSize, titleButtonSize));
             uiDefaults.put("DockableFrameTitlePane.maximizeIcon", IconsFactory.getIcon(null, titleButtonImage, 0, 6 * titleButtonSize, titleButtonSize, titleButtonSize));
             uiDefaults.put("DockableFrameTitlePane.restoreIcon", IconsFactory.getIcon(null, titleButtonImage, 0, 7 * titleButtonSize, titleButtonSize, titleButtonSize));
             uiDefaults.put("DockableFrameTitlePane.buttonGap", new Integer(4)); // gap between buttons
         }
         else */
         if ((lnf.getClass().getName().equals(ALLOY_LNF) && isAlloyLnfInstalled())
                 || (lnf.getClass().getName().equals(PLASTIC3D_LNF) && isPlastic3DLnfInstalled())
                 || (lnf.getClass().getName().equals(PLASTIC3D_LNF_1_3) && isPlastic3D13LnfInstalled())
                 || (lnf.getClass().getName().equals(PLASTICXP_LNF) && isPlasticXPLnfInstalled())
                 || (lnf.getClass().getName().equals(PGS_LNF) && isPgsLnfInstalled())
                 || (lnf.getClass().getName().equals(TONIC_LNF) && isTonicLnfInstalled())) {
             switch (style) {
                 case OFFICE2003_STYLE:
                     VsnetWindowsUtils.initComponentDefaults(uiDefaults);
                     Office2003WindowsUtils.initComponentDefaults(uiDefaults);
                     Office2003WindowsUtils.initClassDefaults(uiDefaults, false);
                     break;
                 case VSNET_STYLE:
                 case VSNET_STYLE_WITHOUT_MENU:
                     VsnetMetalUtils.initComponentDefaults(uiDefaults);
                     VsnetMetalUtils.initClassDefaults(uiDefaults);
 
                     Painter gripperPainter = new Painter() {
                         public void paint(JComponent c, Graphics g, Rectangle rect, int orientation, int state) {
                             Office2003Painter.getInstance().paintGripper(c, g, rect, orientation, state);
                         }
                     };
 
                     // set all grippers to Office2003 style gripper
                     uiDefaults.put("Gripper.painter", gripperPainter);
                     uiDefaults.put("JideTabbedPane.gripperPainter", gripperPainter);
                     uiDefaults.put("JideTabbedPane.defaultTabShape", JideTabbedPane.SHAPE_OFFICE2003);
                     uiDefaults.put("JideTabbedPane.defaultTabColorTheme", JideTabbedPane.COLOR_THEME_WINXP);
                     uiDefaults.put("JideTabbedPane.selectedTabTextForeground", UIDefaultsLookup.getColor("controlText"));
                     uiDefaults.put("JideTabbedPane.unselectedTabTextForeground", UIDefaultsLookup.getColor("controlText"));
                     uiDefaults.put("JideTabbedPane.foreground", UIDefaultsLookup.getColor("controlText"));
                     uiDefaults.put("JideTabbedPane.light", UIDefaultsLookup.getColor("control"));
                     uiDefaults.put("JideSplitPaneDivider.gripperPainter", gripperPainter);
 
                     int products = LookAndFeelFactory.getProductsUsed();
                     if ((products & PRODUCT_DOCK) != 0) {
                         ImageIcon titleButtonImage = IconsFactory.getImageIcon(VsnetWindowsUtils.class, "icons/title_buttons_windows.gif"); // 10 x 10 x 8
                         final int titleButtonSize = 10;
 
                         uiDefaults.put("DockableFrameUI", "com.jidesoft.plaf.vsnet.VsnetDockableFrameUI");
                         uiDefaults.put("DockableFrameTitlePane.hideIcon", IconsFactory.getIcon(null, titleButtonImage, 0, 0, titleButtonSize, titleButtonSize));
                         uiDefaults.put("DockableFrameTitlePane.unfloatIcon", IconsFactory.getIcon(null, titleButtonImage, 0, titleButtonSize, titleButtonSize, titleButtonSize));
                         uiDefaults.put("DockableFrameTitlePane.floatIcon", IconsFactory.getIcon(null, titleButtonImage, 0, 2 * titleButtonSize, titleButtonSize, titleButtonSize));
                         uiDefaults.put("DockableFrameTitlePane.autohideIcon", IconsFactory.getIcon(null, titleButtonImage, 0, 3 * titleButtonSize, titleButtonSize, titleButtonSize));
                         uiDefaults.put("DockableFrameTitlePane.stopAutohideIcon", IconsFactory.getIcon(null, titleButtonImage, 0, 4 * titleButtonSize, titleButtonSize, titleButtonSize));
                         uiDefaults.put("DockableFrameTitlePane.hideAutohideIcon", IconsFactory.getIcon(null, titleButtonImage, 0, 5 * titleButtonSize, titleButtonSize, titleButtonSize));
                         uiDefaults.put("DockableFrameTitlePane.maximizeIcon", IconsFactory.getIcon(null, titleButtonImage, 0, 6 * titleButtonSize, titleButtonSize, titleButtonSize));
                         uiDefaults.put("DockableFrameTitlePane.restoreIcon", IconsFactory.getIcon(null, titleButtonImage, 0, 7 * titleButtonSize, titleButtonSize, titleButtonSize));
                         uiDefaults.put("DockableFrameTitlePane.buttonGap", 4); // gap between buttons
                         uiDefaults.put("DockableFrame.titleBorder", new BorderUIResource(BorderFactory.createEmptyBorder(1, 0, 3, 0)));
                         uiDefaults.put("DockableFrame.border", new BorderUIResource(BorderFactory.createEmptyBorder(2, 0, 0, 0)));
                         uiDefaults.put("DockableFrameTitlePane.gripperPainter", gripperPainter);
                     }
                     break;
                 case ECLIPSE_STYLE:
                     EclipseMetalUtils.initComponentDefaults(uiDefaults);
                     EclipseMetalUtils.initClassDefaults(uiDefaults);
                     break;
                 case ECLIPSE3X_STYLE:
                     Eclipse3xMetalUtils.initComponentDefaults(uiDefaults);
                     Eclipse3xMetalUtils.initClassDefaults(uiDefaults);
                     break;
                 case XERTO_STYLE:
                 case XERTO_STYLE_WITHOUT_MENU:
                     XertoMetalUtils.initComponentDefaults(uiDefaults);
                     XertoMetalUtils.initClassDefaults(uiDefaults);
                     break;
             }
         }
         else if (lnf.getClass().equals(MetalLookAndFeel.class.getName())) {
             switch (style) {
                 case OFFICE2003_STYLE:
                 case VSNET_STYLE:
                     VsnetMetalUtils.initComponentDefaults(uiDefaults);
                     VsnetMetalUtils.initClassDefaultsWithMenu(uiDefaults);
                     break;
                 case ECLIPSE_STYLE:
                     EclipseMetalUtils.initComponentDefaults(uiDefaults);
                     EclipseMetalUtils.initClassDefaults(uiDefaults);
                     break;
                 case ECLIPSE3X_STYLE:
                     Eclipse3xMetalUtils.initComponentDefaults(uiDefaults);
                     Eclipse3xMetalUtils.initClassDefaults(uiDefaults);
                     break;
                 case VSNET_STYLE_WITHOUT_MENU:
                     VsnetMetalUtils.initComponentDefaults(uiDefaults);
                     VsnetMetalUtils.initClassDefaults(uiDefaults);
                     break;
                 case XERTO_STYLE:
                 case XERTO_STYLE_WITHOUT_MENU:
                     XertoMetalUtils.initComponentDefaults(uiDefaults);
                     XertoMetalUtils.initClassDefaults(uiDefaults);
                     break;
                 default:
             }
         }
         else if (lnf instanceof MetalLookAndFeel) {
             switch (style) {
                 case OFFICE2003_STYLE:
                     VsnetMetalUtils.initComponentDefaults(uiDefaults);
                     VsnetMetalUtils.initClassDefaults(uiDefaults);
                     break;
                 case ECLIPSE_STYLE:
                     EclipseMetalUtils.initClassDefaults(uiDefaults);
                     EclipseMetalUtils.initComponentDefaults(uiDefaults);
                     break;
                 case ECLIPSE3X_STYLE:
                     Eclipse3xMetalUtils.initClassDefaults(uiDefaults);
                     Eclipse3xMetalUtils.initComponentDefaults(uiDefaults);
                     break;
                 case VSNET_STYLE:
                 case VSNET_STYLE_WITHOUT_MENU:
                     VsnetMetalUtils.initComponentDefaults(uiDefaults);
                     VsnetMetalUtils.initClassDefaults(uiDefaults);
                     break;
                 case XERTO_STYLE:
                     XertoMetalUtils.initComponentDefaults(uiDefaults);
                     XertoMetalUtils.initClassDefaults(uiDefaults);
                     break;
                 default:
             }
         }
         else if (lnf instanceof WindowsLookAndFeel) {
             switch (style) {
                 case OFFICE2003_STYLE:
                     VsnetWindowsUtils.initComponentDefaultsWithMenu(uiDefaults);
                     VsnetWindowsUtils.initClassDefaultsWithMenu(uiDefaults);
                     Office2003WindowsUtils.initClassDefaults(uiDefaults);
                     Office2003WindowsUtils.initComponentDefaults(uiDefaults);
                     break;
                 case ECLIPSE_STYLE:
                     EclipseWindowsUtils.initClassDefaultsWithMenu(uiDefaults);
                     EclipseWindowsUtils.initComponentDefaultsWithMenu(uiDefaults);
                     break;
                 case ECLIPSE3X_STYLE:
                     Eclipse3xWindowsUtils.initClassDefaultsWithMenu(uiDefaults);
                     Eclipse3xWindowsUtils.initComponentDefaultsWithMenu(uiDefaults);
                     break;
                 case VSNET_STYLE:
                     VsnetWindowsUtils.initComponentDefaultsWithMenu(uiDefaults);
                     VsnetWindowsUtils.initClassDefaultsWithMenu(uiDefaults);
                     break;
                 case VSNET_STYLE_WITHOUT_MENU:
                     VsnetWindowsUtils.initComponentDefaults(uiDefaults);
                     VsnetWindowsUtils.initClassDefaults(uiDefaults);
                     break;
                 case XERTO_STYLE:
                     XertoWindowsUtils.initComponentDefaultsWithMenu(uiDefaults);
                     XertoWindowsUtils.initClassDefaultsWithMenu(uiDefaults);
                     break;
                 case XERTO_STYLE_WITHOUT_MENU:
                     XertoWindowsUtils.initComponentDefaults(uiDefaults);
                     XertoWindowsUtils.initClassDefaults(uiDefaults);
                     break;
                 default:
             }
         }
         // For Mac only
         else if ((lnf.getClass().getName().equals(AQUA_LNF) && isAquaLnfInstalled())
                 || (lnf.getClass().getName().equals(QUAQUA_LNF) && isQuaquaLnfInstalled())) {
             // use reflection since we don't deliver source code of AquaJideUtils as most users don't compile it on Mac OS X
             try {
                 Class<?> aquaJideUtils = getValidClassLoader().loadClass("com.jidesoft.plaf.aqua.AquaJideUtils");
                 aquaJideUtils.getMethod("initComponentDefaults", new Class[]{
                         UIDefaults.class}).invoke(null, uiDefaults);
                 aquaJideUtils.getMethod("initClassDefaults", new Class[]{UIDefaults.class}).invoke(null, uiDefaults);
             }
             catch (ClassNotFoundException e) {
                 throw new RuntimeException(e);
             }
             catch (IllegalAccessException e) {
                 throw new RuntimeException(e);
             }
             catch (IllegalArgumentException e) {
                 throw new RuntimeException(e);
             }
             catch (InvocationTargetException e) {
                 JideSwingUtilities.throwInvocationTargetException(e);
             }
             catch (NoSuchMethodException e) {
                 throw new RuntimeException(e);
             }
             catch (SecurityException e) {
                 throw new RuntimeException(e);
             }
         }
         else {
             // built in initializer
             if (lnf.getClass().getName().equals(GTK_LNF) && isGTKLnfInstalled()) {
                 new GTKInitializer().initialize(uiDefaults);
             }
             else if (lnf.getClass().getName().startsWith(SYNTHETICA_LNF_PREFIX) && isSyntheticaLnfInstalled()) {
                 new SyntheticaInitializer().initialize(uiDefaults);
             }
 
             switch (style) {
                 case OFFICE2003_STYLE:
                     if (SystemInfo.isWindows()) {
                         VsnetWindowsUtils.initComponentDefaultsWithMenu(uiDefaults);
                         Office2003WindowsUtils.initComponentDefaults(uiDefaults);
                         Office2003WindowsUtils.initClassDefaults(uiDefaults);
                     }
                     else {
                         VsnetMetalUtils.initComponentDefaults(uiDefaults);
                         VsnetMetalUtils.initClassDefaults(uiDefaults);
                     }
                     break;
                 case ECLIPSE_STYLE:
                     if (SystemInfo.isWindows()) {
                         EclipseWindowsUtils.initClassDefaultsWithMenu(uiDefaults);
                         EclipseWindowsUtils.initComponentDefaultsWithMenu(uiDefaults);
                     }
                     else {
                         EclipseMetalUtils.initClassDefaults(uiDefaults);
                         EclipseMetalUtils.initComponentDefaults(uiDefaults);
                     }
                     break;
                 case ECLIPSE3X_STYLE:
                     if (SystemInfo.isWindows()) {
                         Eclipse3xWindowsUtils.initClassDefaultsWithMenu(uiDefaults);
                         Eclipse3xWindowsUtils.initComponentDefaultsWithMenu(uiDefaults);
                     }
                     else {
                         Eclipse3xMetalUtils.initClassDefaults(uiDefaults);
                         Eclipse3xMetalUtils.initComponentDefaults(uiDefaults);
                     }
                     break;
                 case VSNET_STYLE:
                     if (SystemInfo.isWindows()) {
                         VsnetWindowsUtils.initClassDefaultsWithMenu(uiDefaults);
                         VsnetWindowsUtils.initComponentDefaultsWithMenu(uiDefaults);
                     }
                     else {
                         VsnetMetalUtils.initComponentDefaults(uiDefaults);
                         VsnetMetalUtils.initClassDefaults(uiDefaults);
                     }
                     break;
                 case VSNET_STYLE_WITHOUT_MENU:
                     if (SystemInfo.isWindows()) {
                         VsnetWindowsUtils.initClassDefaults(uiDefaults);
                         VsnetWindowsUtils.initComponentDefaults(uiDefaults);
                     }
                     else {
                         VsnetMetalUtils.initComponentDefaults(uiDefaults);
                         VsnetMetalUtils.initClassDefaults(uiDefaults);
                     }
                     break;
                 case XERTO_STYLE:
                     if (SystemInfo.isWindows()) {
                         XertoWindowsUtils.initClassDefaultsWithMenu(uiDefaults);
                         XertoWindowsUtils.initComponentDefaultsWithMenu(uiDefaults);
                     }
                     else {
                         XertoMetalUtils.initComponentDefaults(uiDefaults);
                         XertoMetalUtils.initClassDefaults(uiDefaults);
                     }
                     break;
                 case XERTO_STYLE_WITHOUT_MENU:
                     if (SystemInfo.isWindows()) {
                         XertoWindowsUtils.initClassDefaults(uiDefaults);
                         XertoWindowsUtils.initComponentDefaults(uiDefaults);
                     }
                     else {
                         XertoMetalUtils.initComponentDefaults(uiDefaults);
                         XertoMetalUtils.initClassDefaults(uiDefaults);
                     }
                     break;
                 default:
             }
 
             // built in customizer
             if (lnf.getClass().getName().startsWith(SYNTHETICA_LNF_PREFIX) && isSyntheticaLnfInstalled()) {
                 new SyntheticaCustomizer().customize(uiDefaults);
             }
         }
 
         UIManager.put(JIDE_EXTENSION_INSTALLLED, Boolean.TRUE);
 
         UIDefaultsCustomizer[] customizers = getUIDefaultsCustomizers();
         for (UIDefaultsCustomizer customizer : customizers) {
             if (customizer != null) {
                 customizer.customize(uiDefaults);
             }
         }
     }
 
     /**
      * Returns whether or not the Aqua L&F is in classpath.
      *
      * @return <tt>true</tt> if aqua L&F is in classpath, <tt>false</tt> otherwise
      */
     public static boolean isAquaLnfInstalled() {
         try {
             getValidClassLoader().loadClass(AQUA_LNF);
             return true;
         }
         catch (ClassNotFoundException e) {
             return false;
         }
     }
 
     private static ClassLoader getValidClassLoader() {
         ClassLoader classLoader = LookAndFeelFactory.class.getClassLoader();
         if (classLoader == null) {
             classLoader = ClassLoader.getSystemClassLoader();
         }
         return classLoader;
     }
 
     /**
      * Returns whether or not the Quaqua L&F is in classpath.
      *
      * @return <tt>true</tt> if Quaqua L&F is in classpath, <tt>false</tt> otherwise
      */
     public static boolean isQuaquaLnfInstalled() {
         try {
             getValidClassLoader().loadClass(QUAQUA_LNF);
             return true;
         }
         catch (ClassNotFoundException e) {
             return false;
         }
     }
 
     /**
      * Returns whether alloy L&F is in classpath
      *
      * @return <tt>true</tt> alloy L&F is in classpath, <tt>false</tt> otherwise
      */
     public static boolean isAlloyLnfInstalled() {
         try {
             getValidClassLoader().loadClass(ALLOY_LNF);
             return true;
         }
         catch (ClassNotFoundException e) {
             return false;
         }
     }
 
     /**
      * Returns whether GTK L&F is in classpath
      *
      * @return <tt>true</tt> GTK L&F is in classpath, <tt>false</tt> otherwise
      */
     public static boolean isGTKLnfInstalled() {
         try {
             getValidClassLoader().loadClass(GTK_LNF);
             return true;
         }
         catch (ClassNotFoundException e) {
             return false;
         }
     }
 
     /**
      * Returns whether Plastic3D L&F is in classpath
      *
      * @return <tt>true</tt> Plastic3D L&F is in classpath, <tt>false</tt> otherwise
      */
     public static boolean isPlastic3DLnfInstalled() {
         try {
             getValidClassLoader().loadClass(PLASTIC3D_LNF);
             return true;
         }
         catch (ClassNotFoundException e) {
             return false;
         }
     }
 
     /**
      * Returns whether Plastic3D L&F is in classpath
      *
      * @return <tt>true</tt> Plastic3D L&F is in classpath, <tt>false</tt> otherwise
      */
     public static boolean isPlastic3D13LnfInstalled() {
         try {
             getValidClassLoader().loadClass(PLASTIC3D_LNF_1_3);
             return true;
         }
         catch (ClassNotFoundException e) {
             return false;
         }
     }
 
     /**
      * Returns whether PlasticXP L&F is in classpath
      *
      * @return <tt>true</tt> Plastic3D L&F is in classpath, <tt>false</tt> otherwise
      */
     public static boolean isPlasticXPLnfInstalled() {
         try {
             getValidClassLoader().loadClass(PLASTICXP_LNF);
             return true;
         }
         catch (ClassNotFoundException e) {
             return false;
         }
     }
 
     /**
      * Returns whether Tonic L&F is in classpath
      *
      * @return <tt>true</tt> Tonic L&F is in classpath, <tt>false</tt> otherwise
      */
     public static boolean isTonicLnfInstalled() {
         try {
             getValidClassLoader().loadClass(TONIC_LNF);
             return true;
         }
         catch (ClassNotFoundException e) {
             return false;
         }
     }
 
     /**
      * Returns whether A03 L&F is in classpath
      *
      * @return <tt>true</tt> A03 L&F is in classpath, <tt>false</tt> otherwise
      */
     public static boolean isA03LnfInstalled() {
         try {
             getValidClassLoader().loadClass(A03_LNF);
             return true;
         }
         catch (ClassNotFoundException e) {
             return false;
         }
     }
 
     /**
      * Returns whether or not the Pgs L&F is in classpath.
      *
      * @return <tt>true</tt> if pgs L&F is in classpath, <tt>false</tt> otherwise
      */
     public static boolean isPgsLnfInstalled() {
         try {
             getValidClassLoader().loadClass(PGS_LNF);
             return true;
         }
         catch (ClassNotFoundException e) {
             return false;
         }
     }
 
     /**
      * Returns whether or not the Synthetica L&F is in classpath.
      *
      * @return <tt>true</tt> if Synthetica L&F is in classpath, <tt>false</tt> otherwise
      */
     public static boolean isSyntheticaLnfInstalled() {
         try {
             getValidClassLoader().loadClass(SYNTHETICA_LNF);
             return true;
         }
         catch (ClassNotFoundException e) {
             return false;
         }
     }
 
     /**
      * Install the default L&F. In this method, we will look at system property "swing.defaultlaf" first.
      * If the value is set and it's not an instance of Synth L&F, we will use it. Otherwise, we will
      * use Metal L&F is OS is Linux or UNIX and use UIManager.getSystemLookAndFeelClassName() for other OS.
      * In addition, we will add JIDE extension to it.
      */
     public static void installDefaultLookAndFeelAndExtension() {
         installDefaultLookAndFeel();
         // to add attitional UIDefault for JIDE components
         LookAndFeelFactory.installJideExtension();
     }
 
     /**
      * Install the default L&F. In this method, we will look at system property "swing.defaultlaf" first.
      * If the value is set and it's not an instance of Synth L&F, we will use it. Otherwise, we will
      * use Metal L&F is OS is Linux or UNIX and use UIManager.getSystemLookAndFeelClassName() for other OS.
      */
     public static void installDefaultLookAndFeel() {
         try {
             String lnfName = SecurityUtils.getProperty("swing.defaultlaf", null);
             Class<?> lnfClass = null;
             if (lnfName != null) {
                 try {
                     lnfClass = getValidClassLoader().loadClass(lnfName);
                 }
                 catch (ClassNotFoundException e) {
                     // ignore
                 }
             }
 
 //            if (lnfClass != null) {
 //                try {
 //                    Class synthClass = getValidClassLoader().loadClass("javax.swing.plaf.synth.SynthLookAndFeel");
 //                    if (lnfClass.isAssignableFrom(synthClass)) {
 //                        lnfClass = null;
 //                    }
 //                }
 //                catch (ClassNotFoundException e) {
 //                }
 //            }
 
             if (lnfClass == null) {
                 UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
 //                // force to Metal L&F as in JDK1.5, GTK L&F is used as default L&F. We currently don't support GTK L&F.
 //                if (SystemInfo.isLinux() || SystemInfo.isUnix()) {
 //                    UIManager.setLookAndFeel(METAL_LNF);
 //                }
 //                else {
 //                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
 //                }
             }
             else {
                 UIManager.setLookAndFeel(lnfName);
             }
         }
         catch (Exception e) {
             e.printStackTrace();
         }
     }
 
     /**
      * Gets current L&F.
      *
      * @return the current L&F.
      */
     public static LookAndFeel getLookAndFeel() {
         return _lookAndFeel;
     }
 
     /**
      * Gets current style.
      *
      * @return the current style.
      */
     public static int getStyle() {
         return _style;
     }
 
     /**
      * Gets all UIDefaults customizers.
      *
      * @return an array of UIDefaults customizers.
      */
     public static UIDefaultsCustomizer[] getUIDefaultsCustomizers() {
         return _uiDefaultsCustomizers.toArray(new UIDefaultsCustomizer[_uiDefaultsCustomizers.size()]);
     }
 
     /**
      * Adds your own UIDefaults customizer. This customizer will be called
      * after installJideExtension() is called.
      * <code><pre>
      * For example, we use "JideButton.font" as the UIDefault for the JideButton font. If you want to use another font, you can do
      * LookAndFeelFactory.addUIDefaultsCustomizer(new LookAndFeelFactory.UIDefaultsCustomizer() {
      *     public void customize(UIDefaults defaults) {
      *         defaults.put("JideButton.font", whateverFont);
      *     }
      * });
      * </pre></code>
      *
      * @param uiDefaultsCustomizer the UIDefaultsCustomizer
      */
     public static void addUIDefaultsCustomizer(UIDefaultsCustomizer uiDefaultsCustomizer) {
         if (!_uiDefaultsCustomizers.contains(uiDefaultsCustomizer)) {
             _uiDefaultsCustomizers.add(uiDefaultsCustomizer);
         }
     }
 
     /**
      * Removes an existing UIDefaults customizer you added before.
      *
      * @param uiDefaultsCustomizer the UIDefaultsCustomizer
      */
     public static void removeUIDefaultsCustomizer(UIDefaultsCustomizer uiDefaultsCustomizer) {
         _uiDefaultsCustomizers.remove(uiDefaultsCustomizer);
     }
 
     /**
      * Gets all UIDefaults initializers.
      *
      * @return an array of UIDefaults initializers.
      */
     public static UIDefaultsInitializer[] getUIDefaultsInitializers() {
         return _uiDefaultsInitializers.toArray(new UIDefaultsInitializer[_uiDefaultsInitializers.size()]);
     }
 
     /**
      * Adds your own UIDefaults initializer. This initializer will be called
      * before installJideExtension() is called.
      * <p/>
      * Here is how you use it. For example, we use the color of UIDefault "activeCaption" to get the active title color
      * which we will use for active title bar color in JIDE components. If the L&F you are using
      * doesn't set this UIDefault, we might throw NPE later in the code.
      * To avoid this, you call
      * <code><pre>
      * LookAndFeelFactory.addUIDefaultsInitializer(new LookAndFeelFactory.UIDefaultsInitializer() {
      *     public void initialize(UIDefaults defaults) {
      *         defaults.put("activeCaption", whateverColor);
      *     }
      * });
      * UIManager.setLookAndFeel(...); // set whatever L&F
      * LookAndFeelFactory.installJideExtension(); // install the UIDefaults needed by the JIDE components
      * </pre></code>
      *
      * @param uiDefaultsInitializer the UIDefaultsInitializer.
      */
     public static void addUIDefaultsInitializer(UIDefaultsInitializer uiDefaultsInitializer) {
         if (!_uiDefaultsInitializers.contains(uiDefaultsInitializer)) {
             _uiDefaultsInitializers.add(uiDefaultsInitializer);
         }
     }
 
     /**
      * Removes an existing UIDefaults initializer you added before.
      *
      * @param uiDefaultsInitializer the UIDefaultsInitializer
      */
     public static void removeUIDefaultsInitializer(UIDefaultsInitializer uiDefaultsInitializer) {
         _uiDefaultsInitializers.remove(uiDefaultsInitializer);
     }
 
     public static class GTKInitializer implements UIDefaultsInitializer {
         public void initialize(UIDefaults defaults) {
             Object[] uiDefaults = {
                     "activeCaption", defaults.getColor("textHighlight"),
                     "activeCaptionText", defaults.getColor("textHighlightText"),
                     "inactiveCaptionBorder", defaults.getColor("controlShadowtextHighlightText")
             };
             putDefaults(defaults, uiDefaults);
         }
     }
 
 
     public static class SyntheticaInitializer implements UIDefaultsInitializer {
         public void initialize(UIDefaults defaults) {
             Object[] uiDefaults = {
                     "textHighlight", UIDefaultsLookup.getColor("InternalFrame.activeTitleBackground"),
                     "controlText", UIDefaultsLookup.getColor("Label.foreground"),
                     "activeCaptionText", UIDefaultsLookup.getColor("InternalFrame.activeTitleForeground"),
                     "MenuItem.acceleratorFont", UIDefaultsLookup.getFont("Label.font"),
                     "ComboBox.background", new ColorUIResource(Color.WHITE),
                     "ComboBox.disabledForeground", defaults.get("Synthetica.comboBox.disabled.textColor"),
                     "ComboBox.disabledBackground", defaults.get("Synthetica.comboBox.disabled.backgroundColor"),
 
                     "activeCaption", UIDefaultsLookup.getColor("InternalFrame.activeTitleBackground"),
                     "inactiveCaption", UIDefaultsLookup.getColor("InternalFrame.inactiveTitleBackground"),
                     "control", new ColorUIResource(Color.WHITE),
                     "controlLtHighlight", new ColorUIResource(Color.WHITE),
                     "controlHighlight", new ColorUIResource(Color.LIGHT_GRAY),
                     "controlShadow", new ColorUIResource(Color.DARK_GRAY),
                     "controlDkShadow", new ColorUIResource(Color.BLACK),
                     "MenuItem.background", new ColorUIResource(Color.GRAY),
                     "SplitPane.background", UIDefaultsLookup.getColor("Label.background"),
                     "Tree.hash", new ColorUIResource(Color.GRAY),
 
                     "TextField.foreground", UIDefaultsLookup.getColor("Label.foreground"),
                     "TextField.inactiveForeground", UIDefaultsLookup.getColor("Label.foreground"),
                     "TextField.selectionForeground", UIDefaultsLookup.getColor("List.selectionForeground"),
                     "TextField.selectionBackground", UIDefaultsLookup.getColor("List.selectionBackground"),
                     "Table.gridColor", UIDefaultsLookup.getColor("Label.foreground"),
                     "TextField.background", new ColorUIResource(Color.WHITE),
             };
             putDefaults(defaults, uiDefaults);
         }
     }
 
     public static class SyntheticaCustomizer implements UIDefaultsCustomizer {
         public void customize(UIDefaults defaults) {
             Object[] uiDefaults = {
                     "DockableFrame.activeTitleForeground", UIDefaultsLookup.getColor("InternalFrame.activeTitleForeground"),
             };
             overwriteDefaults(defaults, uiDefaults);
         }
     }
 
     public static void verifyDefaults(UIDefaults table, Object[] keyValueList) {
         for (int i = 0, max = keyValueList.length; i < max; i += 2) {
             Object value = keyValueList[i + 1];
             if (value == null) {
                 System.out.println("The value for " + keyValueList[i] + " is null");
             }
             else {
                 Object oldValue = table.get(keyValueList[i]);
                 if (oldValue != null) {
                     System.out.println("The value for " + keyValueList[i] + " exists which is " + oldValue);
                 }
             }
         }
     }
 
     /**
      * Puts a list of UIDefault to the UIDefaults table.
      * The keyValueList is an array with a key and value in pair. If the value is
      * null, this method will remove the key from the table. If the table already has a value for
      * the key, the new value will be ignored. This is the difference from {@link #putDefaults(javax.swing.UIDefaults,Object[])} method.
      * You should use this method in {@link UIDefaultsInitializer} so that it fills in the UIDefault value only when it is missing.
      *
      * @param table         the ui defaults table
      * @param keyValueArray the key value array. It is in the format of a key followed by a value.
      */
     public static void putDefaults(UIDefaults table, Object[] keyValueArray) {
         for (int i = 0, max = keyValueArray.length; i < max; i += 2) {
             Object value = keyValueArray[i + 1];
             if (value == null) {
                 table.remove(keyValueArray[i]);
             }
             else {
                 if (table.get(keyValueArray[i]) == null) {
                     table.put(keyValueArray[i], value);
                 }
             }
         }
     }
 
     /**
      * Puts a list of UIDefault to the UIDefaults table.
      * The keyValueList is an array with a key and value in pair. If the value is
      * null, this method will remove the key from the table. Otherwise, it will put the new value
      * in even if the table already has a value for the key. This is the difference from {@link #putDefaults(javax.swing.UIDefaults,Object[])} method.
      * You should use this method in {@link UIDefaultsCustomizer} because you always want to override the existing value using the new value.
      *
      * @param table         the ui defaults table
      * @param keyValueArray the key value array. It is in the format of a key followed by a value.
      */
     public static void overwriteDefaults(UIDefaults table, Object[] keyValueArray) {
         for (int i = 0, max = keyValueArray.length; i < max; i += 2) {
             Object value = keyValueArray[i + 1];
             if (value == null) {
                 table.remove(keyValueArray[i]);
             }
             else {
                 table.put(keyValueArray[i], value);
             }
         }
     }
 
     private static int _productsUsed = -1;
 
     public static int getProductsUsed() {
         if (_productsUsed == -1) {
             _productsUsed = 0;
             try {
                 Class.forName("com.jidesoft.docking.Product");
                 _productsUsed |= PRODUCT_DOCK;
             }
             catch (ClassNotFoundException e) {
                 //
             }
             try {
                 Class.forName("com.jidesoft.action.Product");
                 _productsUsed |= PRODUCT_ACTION;
             }
             catch (ClassNotFoundException e) {
                 //
             }
             try {
                 Class.forName("com.jidesoft.document.Product");
                 _productsUsed |= PRODUCT_COMPONENTS;
             }
             catch (ClassNotFoundException e) {
                 //
             }
             try {
                 Class.forName("com.jidesoft.grid.Product");
                 _productsUsed |= PRODUCT_GRIDS;
             }
             catch (ClassNotFoundException e) {
                 //
             }
             try {
                 Class.forName("com.jidesoft.wizard.Product");
                 _productsUsed |= PRODUCT_DIALOGS;
             }
             catch (ClassNotFoundException e) {
                 //
             }
             try {
                 Class.forName("com.jidesoft.pivot.Product");
                 _productsUsed |= PRODUCT_PIVOT;
             }
             catch (ClassNotFoundException e) {
                 //
             }
             try {
                 Class.forName("com.jidesoft.shortcut.Product");
                 _productsUsed |= PRODUCT_SHORTCUT;
             }
             catch (ClassNotFoundException e) {
                 //
             }
             try {
                 Class.forName("com.jidesoft.editor.Product");
                 _productsUsed |= PRODUCT_CODE_EDITOR;
             }
             catch (ClassNotFoundException e) {
                 //
             }
             try {
                 Class.forName("com.jidesoft.rss.Product");
                 _productsUsed |= PRODUCT_FEEDREADER;
             }
             catch (ClassNotFoundException e) {
                 //
             }
         }
         return _productsUsed;
     }
 
     /**
      * Sets the products you will use. This is needed so that LookAndFeelFactory knows what UIDefault to initialize.
      * For example, if you use only JIDE Docking Framework and JIDE Grids, you should call
      * <code>setProductUsed(ProductNames.PRODUCT_DOCK | ProductNames.PRODUCT_GRIDS)</code> so that we don't initialize
      * UIDefaults needed by any other products. If you use this class as part of JIDE Common Layer open source
      * project, you should call <code>setProductUsed(ProductNames.PRODUCT_COMMON)</code>. If you want to use all JIDE products,
      * you should call <code>setProductUsed(ProductNames.PRODUCT_ALL)</code>
      *
      * @param productsUsed a bit-wise OR of product values defined in {@link com.jidesoft.utils.ProductNames}.
      */
     public static void setProductsUsed(int productsUsed) {
         _productsUsed = productsUsed;
     }
 }
