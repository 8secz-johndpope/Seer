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
 
 package uk.org.ownage.dmdirc.commandparser.commands.server;
 
 import uk.org.ownage.dmdirc.Server;
 import uk.org.ownage.dmdirc.commandparser.CommandManager;
 import uk.org.ownage.dmdirc.commandparser.CommandWindow;
 import uk.org.ownage.dmdirc.commandparser.ServerCommand;
 import uk.org.ownage.dmdirc.plugins.PluginManager;
 import uk.org.ownage.dmdirc.ui.messages.Formatter;
 
 /**
  * Allows the user to load a plugin.
  * @author chris
  */
 public final class LoadPlugin extends ServerCommand {
     
     /**
      * Creates a new instance of LoadPlugin.
      */
     public LoadPlugin() {
         super();
         
         CommandManager.registerCommand(this);
     }
     
     /**
      * Executes this command.
      * @param origin The frame in which this command was issued
      * @param server The server object that this command is associated with
      * @param args The user supplied arguments
      */
     public void execute(final CommandWindow origin, final Server server,
             final String... args) {
        if (PluginManager.getPluginManager().addPlugin(args[0], args[0]) {
             PluginManager.getPluginManager().getPlugin(args[0]).onActivate();
             origin.addLine("Plugin loaded.");
         } else {
             origin.addLine("Load failed: " + ex.getMessage());
         }
     }
     
     
     /** {@inheritDoc}. */
     public String getName() {
         return "loadplugin";
     }
     
     /** {@inheritDoc}. */
     public boolean showInHelp() {
         return true;
     }
     
     /** {@inheritDoc}. */
     public boolean isPolyadic() {
         return false;
     }
     
     /** {@inheritDoc}. */
     public int getArity() {
         return 1;
     }
     
     /** {@inheritDoc}. */
     public String getHelp() {
         return "loadplugin <class> - loads the specified class as a plugin";
     }
     
 }
