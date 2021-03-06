 /*
  * Copyright (c) 2006-2007 Chris Smith, Shane Mc Cormack, Gregory Holmes
  *
  * Permission is hereby granted, free of charge, to any person obtaining a copy
  * of this software and associated documentation files (the "Software"), to deal
  * in the Software without restriction, including without limitation the rights
  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  * copies of the Software, and to permit persons to whom the Software is
  * furnished to do so, subject to the following conditions:
  *
  * The above copyright notice and this permission notice shall be included in
  * all copies or substantial portions of the Software.
  *
  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  * SOFTWARE.
  */
 
 package uk.org.ownage.dmdirc.ui.messages;
 
 import java.io.File;
 import java.io.FileInputStream;
 import java.io.FileNotFoundException;
 import java.io.FileOutputStream;
 import java.io.IOException;
import java.util.IllegalFormatConversionException;
 import java.util.InvalidPropertiesFormatException;
 import java.util.Properties;
 import java.util.Set;
 
 import uk.org.ownage.dmdirc.Config;
 import uk.org.ownage.dmdirc.logger.ErrorLevel;
 import uk.org.ownage.dmdirc.logger.Logger;
 
 /**
  * The Formatter provides a standard way to format messages for display.
  */
 public final class Formatter {
     
     /**
      * The format strings used by the formatter.
      */
     private static Properties properties;
     
     /**
      * The default properties we fall back to if the user hasn't defined their
      * own. */
     private static Properties defaultProperties;
     
     /**
      * Creates a new instance of Formatter.
      */
     private Formatter() {
     }
     
     /**
      * Inserts the supplied arguments into a format string for the specified
      * message type.
      * @param messageType The message type that the arguments should be formatted as
      * @param arguments The arguments to this message type
      * @return A formatted string
      */
     public static String formatMessage(final String messageType,
             final Object... arguments) {
         if (properties == null) {
             initialise();
         }
         
         final String res = properties.getProperty(messageType);
         
         if (res == null) {
             Logger.error(ErrorLevel.ERROR, "Format string not found: " + messageType);
             return "<No format string for message type " + messageType + ">";
         } else {
            try {
                return String.format(res, arguments);
            } catch (IllegalFormatConversionException ex) {
                return "<Invalid format string for message type " + messageType + ">";
            }
         }
     }
     
     
     public static Set<String> getFormats() {
         if (properties == null) {
             initialise();
         }
         
         return properties.stringPropertyNames();
     }
     /**
      * Determines whether the formatter knows of a specific message type.
      * @param messageType the message type to check
      * @return True iff there is a matching format, false otherwise
      */
     public static boolean hasFormat(final String messageType) {
         if (properties == null) {
             initialise();
         }
         
         return properties.getProperty(messageType) != null;
     }
     
     /**
      * Returns the default format strings for the client.
      */
     private static void loadDefaults() {
         defaultProperties = new Properties();
         
         final char colour = Styliser.CODE_COLOUR;
         final char stop = Styliser.CODE_STOP;
         final char fixed = Styliser.CODE_FIXED;
         
         // Type: Timestamp
         //    1: Current timestamp
         defaultProperties.setProperty("timestamp", "%1$tH:%1$tM:%1$tS | ");
         
         // Type: Channel Message
         //    1: User mode prefixes
         //    2: User nickname
         //    3: User ident
         //    4: User host
         //    5: Message content
         //    6: Channel name
         defaultProperties.setProperty("channelMessage", "<%1$s%2$s> %5$s");
         defaultProperties.setProperty("channelHighlight", colour + "4<%1$s%2$s> %5$s");
         defaultProperties.setProperty("channelAction", colour + "6* %1$s%2$s %5$s");
         defaultProperties.setProperty("channelSelfMessage", "<%1$s%2$s> %5$s");
         defaultProperties.setProperty("channelSelfAction", colour + "6* %1$s%2$s %5$s");
         defaultProperties.setProperty("channelSelfExternalMessage", "<%1$s%2$s> %5$s");
         defaultProperties.setProperty("channelSelfExternalAction", colour + "6* %1$s%2$s %5$s");
         
         // Type: Channel CTCP
         //    1: User mode prefixes
         //    2: User nickname
         //    3: User ident
         //    4: User host
         //    5: CTCP type
         //    6: CTCP content
         //    7: Channel name
         defaultProperties.setProperty("channelCTCP", colour + "4-!- CTCP %5$S from %1$s%2$s");
         
         // Type: Channel Event
         //    1: User mode prefixes
         //    2: User nickname
         //    3: User ident
         //    4: User host
         //    5: Channel name
         defaultProperties.setProperty("channelJoin", colour + "3* %2$s (%3$s@%4$s) has joined %5$s.");
         defaultProperties.setProperty("channelPart", colour + "3* %1$s%2$s (%3$s@%4$s) has left %5$s.");
         defaultProperties.setProperty("channelQuit", colour + "2* %1$s%2$s (%3$s@%4$s) has quit IRC.");
         defaultProperties.setProperty("channelSelfJoin", colour + "3* You are now talking in %5$s.");
         defaultProperties.setProperty("channelSelfPart", colour + "3* You have left the channel.");
         
         // Type: Channel Event with content
         //    1: User mode prefixes
         //    2: User nickname
         //    3: User ident
         //    4: User host
         //    5: Channel name
         //    6: Content
         defaultProperties.setProperty("channelPartReason", colour + "3* %1$s%2$s (%3$s@%4$s) has left %5$s (%6$s" + stop + ").");
         defaultProperties.setProperty("channelQuitReason", colour + "2* %1$s%2$s (%3$s@%4$s) has quit IRC (%6$s" + stop + ").");
         defaultProperties.setProperty("channelTopicChange", colour + "3* %1$s%2$s has changed the topic to '%6$s" + stop + "'.");
         defaultProperties.setProperty("channelNickChange", colour + "3* %1$s%2$s is now know as %6$s.");
         defaultProperties.setProperty("channelModeChange", colour + "3* %1$s%2$s sets mode: %6$s.");
         defaultProperties.setProperty("channelSelfNickChange", colour + "3* You are now know as %6$s.");
         defaultProperties.setProperty("channelSelfModeChange", colour + "3* You set mode: %6$s.");
         defaultProperties.setProperty("channelSelfPartReason", colour + "3* You have left the channel.");
         
         // Type: Binary Channel Event
         //    1: Source user mode prefixes
         //    2: Source user nickname
         //    3: Source user ident
         //    4: Source user host
         //    5: Target user mode prefixes
         //    6: Target user nickname
         //    7: Target user ident
         //    8: Target user host
         //    9: Channel name
         defaultProperties.setProperty("channelKick", colour + "3* %1$s%2$s has kicked %5$s%6$s from %9$s.");
         
         // Type: Binary Channel Event with content
         //    1: Source user mode prefixes
         //    2: Source user nickname
         //    3: Source user ident
         //    4: Source user host
         //    5: Target user mode prefixes
         //    6: Target user nickname
         //    7: Target user ident
         //    8: Target user host
         //    9: Channel name
        //   10: Content
         defaultProperties.setProperty("channelKickReason", colour + "3* %1$s%2$s has kicked %5$s%6$s from %9$s (%10$s" + stop + ").");
         defaultProperties.setProperty("channelUserMode_default", colour + "3* %1$s%2$s sets mode %10$s on %6$s.");
         
         // Type: Channel topic sync
         //    1: Topic
         //    2: User responsible
         //    3: Time last changed
         //    4: Channel name
         defaultProperties.setProperty("channelJoinTopic", colour + "3* The topic for %4$s is '%1$s" + stop + "'.\n" + colour + "3* Topic was set by %2$s.");
         
         // Type: Channel mode discovery
         //     1: Channel modes
         //     2: Channel name
         defaultProperties.setProperty("channelModeDiscovered", colour + "3* Channel modes for %2$s are: %1$s.");
         
         // Type: Private CTCP
         //    1: User nickname
         //    2: User ident
         //    3: User host
         //    4: CTCP type
         //    5: CTCP content
         defaultProperties.setProperty("privateCTCP", colour + "4-!- CTCP %4$S from %1$s");
         defaultProperties.setProperty("privateCTCPreply", colour + "4-!- CTCP %4$S reply from %1$s: %5$s");
         
         // Type: Private communications
         //    1: User nickname
         //    2: User ident
         //    3: User host
         //    4: Message content
         defaultProperties.setProperty("privateNotice", colour + "5-%1$s- %4$s");
         defaultProperties.setProperty("queryMessage", "<%1$s> %4$s");
         defaultProperties.setProperty("queryAction", colour + "6* %1$s %4$s");
         defaultProperties.setProperty("querySelfMessage", "<%1$s> %4$s");
         defaultProperties.setProperty("querySelfAction", colour + "6* %1$s %4$s");
         defaultProperties.setProperty("queryNickChanged", colour + "3* %1$s is now know as %4$s.");
         
         // Type: Outgoing message
         //    1: Target
         //    2: Message
         defaultProperties.setProperty("selfCTCP", colour + "4->- [%1$s] %2$s");
         defaultProperties.setProperty("selfNotice", colour + "5>%1$s> %2$s");
         defaultProperties.setProperty("selfMessage", ">[%1$s]> %2$s");
         
         // Type: Miscellaneous server
         //    1: Server name
         //    2: Miscellaneous argument
         defaultProperties.setProperty("connectError", colour + "2Error connecting: %2$s");
         defaultProperties.setProperty("connectRetry", colour + "2Reconnecting in %2$s seconds...");
         defaultProperties.setProperty("serverConnecting", "Connecting to %1$s:%2$s...");
         
         // Type: Miscellaneous
         //    1: Miscellaneous data
         defaultProperties.setProperty("channelNoTopic", colour + "3* There is no topic set for %1$s.");
         defaultProperties.setProperty("rawCommand", colour + "10>>> %1$s");
         defaultProperties.setProperty("unknownCommand", colour + "14Unknown command %1$s.");
         defaultProperties.setProperty("socketClosed", colour + "2-!- You have been disconnected from the server.");
         defaultProperties.setProperty("stonedServer", colour + "2-!- Disconnected from a non-responsive server.");
         defaultProperties.setProperty("motdStart", colour + "10%1$s");
         defaultProperties.setProperty("motdLine", colour + "10" + fixed + "%1$s");
         defaultProperties.setProperty("motdEnd", colour + "10%1$s");
         defaultProperties.setProperty("rawIn", "<< %1$s");
         defaultProperties.setProperty("rawOut", ">> %1$s");
         defaultProperties.setProperty("commandOutput", "%1$s");
         defaultProperties.setProperty("commandError", colour + "7%1$s");
         defaultProperties.setProperty("actionTooLong", "Warning: action too long to be sent");
         
         // Type: Command usage
         //    1: Command char
         //    2: Command name
         //    3: Arguments
         defaultProperties.setProperty("commandUsage", colour + "7Usage: %1$s%2$s %3$s");
         
         // Type: Numerical data
         defaultProperties.setProperty("numeric_301", "%4$s is away: %5$s");
         defaultProperties.setProperty("numeric_311", "-\n%4$s is %5$s@%6$s (%8$s).");
         defaultProperties.setProperty("numeric_312", "%4$s is connected to %5$s (%6$s).");
         defaultProperties.setProperty("numeric_313", "%4$s %5$s.");
         defaultProperties.setProperty("numeric_318", "End of WHOIS info for %4$s.\n-");
         defaultProperties.setProperty("numeric_319", "%4$s is on: %5$s");
         defaultProperties.setProperty("numeric_330", "%4$s %6$s %5$s.");
         defaultProperties.setProperty("numeric_343", "%4$s %6$s %5$s.");
     }
     
     /**
      * Allows plugins (etc) to register new default formats.
      * @param name The name of the format
      * @param format The actual format itself
      */
     public static void registerDefault(final String name, final String format) {
         if (defaultProperties == null) {
             loadDefaults();
         }
         
         defaultProperties.setProperty(name, format);
     }
     
     /**
      * Reads the format strings from disk (if available), and initialises the
      * properties object.
      */
     private static void initialise() {
         if (defaultProperties == null) {
             loadDefaults();
         }
         
         properties = new Properties(defaultProperties);
         
         if (Config.hasOption("general", "formatters")) {
             for (String file : Config.getOption("general", "formatters").split("\n")) {
                 loadFile(file);
             }
         }
     }
     
     /**
      * Loads the specified file into the formatter.
      * @param file File to be loaded
      * @return True iff the operation succeeeded, false otherwise
      */
     public static boolean loadFile(final String file) {
         final File myFile = new File(Config.getConfigDir() + file);
         if (myFile.exists()) {
             try {
                 properties.load(new FileInputStream(myFile));
             } catch (InvalidPropertiesFormatException ex) {
                 Logger.error(ErrorLevel.TRIVIAL, "Unable to load formatter", ex);
                 return false;
             } catch (FileNotFoundException ex) {
                 return false;
             } catch (IOException ex) {
                 Logger.error(ErrorLevel.WARNING, "unable to load formatter", ex);
                 return false;
             }
             
             return true;
         } else {
             return false;
         }
     }
     
     /**
      * Saves the current formatter into the specified file.
      * @param target The target file
      * @return True iff the operation succeeded, false otherwise
      */
     public static boolean saveAs(final String target) {
         if (properties == null) {
             initialise();
         }
         
         final File myFile = new File(Config.getConfigDir() + target);
         
         try {
             properties.store(new FileOutputStream(myFile), null);
         } catch (FileNotFoundException ex) {
             Logger.error(ErrorLevel.TRIVIAL, "Error saving formatter", ex);
             return false;
         } catch (IOException ex) {
             Logger.error(ErrorLevel.WARNING, "Error saving formatter", ex);
             return false;
         }
         
         return true;
     }
     
     /**
      * Reloads the formatter.
      */
     public static void reload() {
         initialise();
     }
 }
