 /*
  * java-gnome, a UI library for writing GTK and GNOME programs from Java!
  *
  * Copyright © 2006-2010 Operational Dynamics Consulting, Pty Ltd and Others
  *
  * The code in this file, and the program it is a part of, is made available
  * to you by its authors as open source software: you can redistribute it
  * and/or modify it under the terms of the GNU General Public License version
  * 2 ("GPL") as published by the Free Software Foundation.
  *
  * This program is distributed in the hope that it will be useful, but WITHOUT
  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
  * FITNESS FOR A PARTICULAR PURPOSE. See the GPL for more details.
  *
  * You should have received a copy of the GPL along with this program. If not,
  * see http://www.gnu.org/licenses/. The authors of this program may be
  * contacted through http://java-gnome.sourceforge.net/.
  *
  * Linking this library statically or dynamically with other modules is making
  * a combined work based on this library. Thus, the terms and conditions of
  * the GPL cover the whole combination. As a special exception (the
  * "Claspath Exception"), the copyright holders of this library give you
  * permission to link this library with independent modules to produce an
  * executable, regardless of the license terms of these independent modules,
  * and to copy and distribute the resulting executable under terms of your
  * choice, provided that you also meet, for each linked independent module,
  * the terms and conditions of the license of that module. An independent
  * module is a module which is not derived from or based on this library. If
  * you modify this library, you may extend the Classpath Exception to your
  * version of the library, but you are not obligated to do so. If you do not
  * wish to do so, delete this exception statement from your version.
  */
 package org.gnome.glib;
 
 /**
  * Static methods to initialize the Java bindings around GLib.
  * 
  * @author Andrew Cowie
  * @author Serkan Kaba
  * @author Guillaume Mazoyer
  * @since 4.0.0
  * 
  * @see <a
  *      href="http://www.freedesktop.org/wiki/Specifications/basedir-spec?action=show&redirect=Standards%2Fbasedir-spec">
  *      XDG Base Directory Specification</a>
  */
 public class Glib
 {
     /**
      * No instantiation. Static methods only!
      */
     protected Glib() {}
 
     /**
      * Change the internal program name used by GLib and GTK for internal
      * error messages. Occasionally (especially as we develop new
      * functionality) you or we will do something wrong, and GLib will
      * complain loudly about it to the console, for example:
      * 
      * <pre>
      * (gnome-panel:5581): Gtk-WARNING **: gtk_widget_size_allocate(): attempt to...
      * </pre>
      * 
      * where "<code>gnome-panel</code>" was the name set by that program with
      * this method call, and <code>5581</code> was the process id originating
      * the message. As you can see, the whole thing is pretty ugly (not to
      * mention having no context), which is why one of the design goals of
      * java-gnome is to fully proxy the entire underlying library and have
      * none of the internals from GLib or GTK be exposed to the Java
      * developer. If we do our job right, your users should never see a
      * message like that; at <i>worst</i> it would be reported as a Java stack
      * trace.
      * 
      * <p>
      * You don't really need to call this, but it's here if you want to make
      * it clearer in the <code>.xsession-errors</code> log what the culprit
      * application is.
      * 
      * <p>
      * <b>Warning</b><br>
      * If you wish to use this, it <b>must</b> be called before anything else.
      * This is the <i>only</i> method in java-gnome that can be called before
      * {@link org.gnome.gtk.Gtk#init(String[]) Gtk.init()}.
      * 
      * @since 4.0.14
      */
     /*
      * Another one to potentially move to a GtkApplication class.
      */
     public static void setProgramName(String name) {
         GlibMisc.setPrgname(name);
     }
 
     /**
      * Get the XDG user specific configuration directory. In all likelihood
      * this will be <code>~/.config</code>.
      * 
      * @since 4.0.15
      */
     public static String getUserConfigDir() {
         return GlibMisc.getUserConfigDir();
     }
 
     /**
      * Returns the username (i.e Linux login name) running the application.<br>
      * 
      * <p>
      * <b>WARNING:</b><br>
      * This method assumes that your system uses UTF-8 as encoding. Please
      * file a bug if this assumption is not valid for your system.</b>
      * 
      * @since 4.0.15
      */
     public static String getUserName() {
         return GlibMisc.getUserName();
     }
 
     /**
      * Returns the real name of the user running the application from
      * <code>/etc/passwd</code> file. If it can't be determined
      * <code>"Unknown"</code> is returned.
      * 
      * <p>
      * The warning about encoding in {@link #getUserName() getUserName()} also
      * applies.
      * 
      * @since 4.0.15
      */
     public static String getRealName() {
         return GlibMisc.getRealName();
     }
 
     /**
      * Get the XDG user specific cache directory. In all likelihood this will
      * be <code>~/.cache</code>.
      * 
      * @since 4.0.15
      */
     public static String getUserCacheDir() {
         return GlibMisc.getUserCacheDir();
     }
 
     /**
      * Get the XDG user specific data directory. In all likelihood this will
      * be <code>~/.local/share</code>.
      * 
      * @since 4.0.15
      */
     public static String getUserDataDir() {
         return GlibMisc.getUserDataDir();
     }
 
     /**
      * Get the XDG user specific special directory. Directory constants are
      * defined in {@link UserDirectory}. System wide defaults are defined in
      * <code>/etc/xdg/user-dirs.defaults</code> and can be overridden in
      * <code>~/.config/user-dir.dirs</code>.
      * 
      * <p>
      * If you've already queried the "special" directories then those values
      * are cached; they certainly don't change often. If you're writing a
      * program that absolutely needs to be aware if those settings have
      * changed after you're already used this, then you can force up to date
      * information by calling {@link #reloadUserSpecialDirsCache()
      * Glib.reloadUserSpecialDirsCache()}.
      * 
      * @since 4.0.15
      */
     public static String getUserSpecialDir(UserDirectory directory) {
         return GlibMisc.getUserSpecialDir(directory);
     }
 
     /**
      * Reset the cache used for {@link #getUserSpecialDir(UserDirectory)
      * getUserSpecialDir()}.
      * 
      * <p>
      * <b>WARNING:</b><br>
      * This may cause memory leaks if the return values change between calls.
      * 
      * @since 4.0.15
      */
     public static void reloadUserSpecialDirsCache() {
         GlibMisc.reloadUserSpecialDirsCache();
     }
 
     /**
      * Get a list of system-wide XDG data directories.
      * 
      * @since 4.0.15
      */
     public static String[] getSystemDataDirs() {
         return GlibMisc.getSystemDataDirs();
     }
 
     /**
      * Get a list of system-wide XDG configuration directories.
      * 
      * @since 4.0.15
      */
     public static String[] getSystemConfigDirs() {
         return GlibMisc.getSystemConfigDirs();
     }
 
     /**
      * Format a size into a human readable <code>String</code>.
      * 
      * <p>
      * Sizes use the nearest prefix (KB, MB, GB). The prefix units base is
      * 1024 so 1 MB is 1024 * 1024 bytes.
      * 
      * <p>
      * Note that the returned <code>String</code> depends of the localization.
     * E.g. if the system is configured to use French, the formatted size will
     * use French size prefix.
      * 
      * @since 4.0.16
      */
     public static String formatSizeForDisplay(long size) {
         return GlibMisc.formatSizeForDisplay(size);
     }
 }
