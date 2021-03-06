 /*
  * Copyright (c) 2006-2010 Chris Smith, Shane Mc Cormack, Gregory Holmes
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
 
 package com.dmdirc.ui.input;
 
 import com.dmdirc.actions.ActionManager;
 import com.dmdirc.actions.CoreActionType;
 import com.dmdirc.commandparser.CommandArguments;
 import com.dmdirc.commandparser.CommandInfo;
 import com.dmdirc.commandparser.CommandManager;
 import com.dmdirc.commandparser.commands.Command;
 import com.dmdirc.commandparser.commands.ValidatingCommand;
 import com.dmdirc.commandparser.commands.WrappableCommand;
 import com.dmdirc.commandparser.parsers.CommandParser;
 import com.dmdirc.config.prefs.validator.ValidationResponse;
 import com.dmdirc.interfaces.ConfigChangeListener;
 import com.dmdirc.plugins.PluginManager;
 import com.dmdirc.ui.input.tabstyles.TabCompletionResult;
 import com.dmdirc.ui.input.tabstyles.TabCompletionStyle;
 import com.dmdirc.ui.interfaces.InputField;
 import com.dmdirc.ui.interfaces.InputValidationListener;
 import com.dmdirc.ui.interfaces.InputWindow;
 import com.dmdirc.ui.messages.Styliser;
 import com.dmdirc.util.ListenerList;
 import com.dmdirc.util.RollingList;
 
 import java.awt.Toolkit;
 import java.awt.event.KeyEvent;
 import java.util.ArrayList;
 import java.util.List;
 import java.util.Map;
 
 /**
  * Handles events generated by a user typing into a textfield. Allows the user
  * to use shortcut keys for control characters (ctrl+b, etc), to tab complete
  * nicknames/channel names/etc, and to scroll through their previously issued
  * commands.
  *
  * @author chris
  */
 public abstract class InputHandler implements ConfigChangeListener {
 
     /**
      * Indicates that the caret should be moved to the end of a selection when
      * a control code has been inserted.
      */
     private static final int POSITION_END = 1;
     /**
      * Indicates that the caret should be moved to the start of a selection when
      * a control code has been inserted.
      */
     protected static final int POSITION_START = 2;
     /** Flag to indicate that this input handler should handle tab completion. */
     protected static final int HANDLE_TABCOMPLETION = 1;
     /** Flag to indicate that this input handler should maintain a back buffer. */
     protected static final int HANDLE_BACKBUFFER = 2;
     /** Flag to indicate that this input handler should handle formatting. */
     protected static final int HANDLE_FORMATTING = 4;
     /** Flag to indicate that this input handler should handle returns. */
     protected static final int HANDLE_RETURN = 8;
 
     /** A logger for this class. */
     private static final java.util.logging.Logger LOGGER = java.util.logging
             .Logger.getLogger(InputHandler.class.getName());
 
     /** The flags for this particular input handler. */
     protected int flags = HANDLE_TABCOMPLETION | HANDLE_BACKBUFFER
             | HANDLE_FORMATTING | HANDLE_RETURN;
     /** The input buffer. */
     protected RollingList<String> buffer;
     /** The textfield that we're handling input for. */
     protected final InputField target;
     /** The TabCompleter to use for tab completion. */
     protected TabCompleter tabCompleter;
     /** The CommandParser to use for our input. */
     protected final CommandParser commandParser;
     /** The frame that we belong to. */
     protected final InputWindow parentWindow;
     /** The tab completion style. */
     protected TabCompletionStyle style;
     /** Our listener list. */
     private final ListenerList listeners = new ListenerList();
 
     /**
      * Creates a new instance of InputHandler. Adds listeners to the target
      * that we need to operate.
      *
      * @param thisTarget The text field this input handler is dealing with.
      * @param thisCommandParser The command parser to use for this text field.
      * @param thisParentWindow The window that owns this input handler
      */
     public InputHandler(final InputField thisTarget,
             final CommandParser thisCommandParser,
             final InputWindow thisParentWindow) {
 
         buffer = new RollingList<String>(thisParentWindow.getConfigManager().
                 getOptionInt("ui", "inputbuffersize"), "");
 
         this.commandParser = thisCommandParser;
         this.parentWindow = thisParentWindow;
         this.target = thisTarget;
 
         setStyle();
 
         parentWindow.getConfigManager().addChangeListener("tabcompletion",
                 "style", this);
 
         addUpHandler();
         addDownHandler();
         addTabHandler();
         addKeyHandler();
         addEnterHandler();
     }
 
     /**
      * Adds an arrow up key handler.
      */
     protected abstract void addUpHandler();
 
     /**
      * Adds an arrow down key handler.
      */
     protected abstract void addDownHandler();
 
     /**
      * Adds an tab key handler.
      */
     protected abstract void addTabHandler();
 
     /**
      * Adds a key handler.
      */
     protected abstract void addKeyHandler();
     
     /** 
      * Adds an enter key handler.
      */
     protected abstract void addEnterHandler();
 
     /**
      * Indicates which types of input this handler should handle.
      * 
      * @param handleTabCompletion Whether or not to handle tab completion
      * @param handleBackBuffer Whether or not to maintain an input back buffer
      * @param handleFormatting Whether or not to handle formatting
      * @param handleReturn Whether or not to handle returns
      */
     public void setTypes(final boolean handleTabCompletion,
             final boolean handleBackBuffer,
             final boolean handleFormatting, final boolean handleReturn) {
         flags = (handleTabCompletion ? HANDLE_TABCOMPLETION : 0) 
                 | (handleBackBuffer ? HANDLE_BACKBUFFER : 0)
                 | (handleFormatting ? HANDLE_FORMATTING : 0)
                 | (handleReturn ? HANDLE_RETURN : 0);
     }
 
     /**
      * Sets this inputhandler's tab completion style.
      */
     private void setStyle() {
         style = (TabCompletionStyle) PluginManager.getPluginManager()
                 .getServiceProvider("tabcompletion", parentWindow.getConfigManager()
                 .getOption("tabcompletion", "style"))
                 .getExportedService("getCompletionStyle").execute(tabCompleter, parentWindow);
     }
 
     /**
      * Sets this input handler's tab completer.
      *
      * @param newTabCompleter The new tab completer
      */
     public void setTabCompleter(final TabCompleter newTabCompleter) {
         tabCompleter = newTabCompleter;
         setStyle();
     }
 
     /**
      * Handles the pressing of a key. Inserts control chars as appropriate.
      *
      * @param line Text in the target
      * @param keyCode Keycode for the pressed key
      * @param shiftPressed Was shift pressed
      * @param ctrlPressed Was ctrl key pressed
      */
     protected void handleKeyPressed(final String line, final int keyCode,
             final boolean shiftPressed, final boolean ctrlPressed) {
         target.hideColourPicker();
         
         if (ctrlPressed && (flags & HANDLE_FORMATTING) != 0) {
             handleControlKey(line, keyCode, shiftPressed);
         }
         
         validateText();
     }
     
     /**
      * Validates the text currently entered in the text field.
      */
     protected void validateText() {
         final String text = target.getText();
 
         final CommandArguments args = new CommandArguments(text);
 
         if (args.isCommand()) {
             final Map.Entry<CommandInfo, Command> command
                     = CommandManager.getCommand(args.getCommandName());
             
             if (command != null && command.getValue() instanceof ValidatingCommand) {
                 final ValidationResponse vr = ((ValidatingCommand) command.getValue())
                         .validateArguments(parentWindow, args);
                 
                 if (vr.isFailure()) {
                     fireCommandFailure(vr.getFailureReason());
                 } else {
                     fireCommandPassed();
                 }
             }
             
             if (command != null && command.getValue() instanceof WrappableCommand) {
                 final int count = ((WrappableCommand) command.getValue())
                         .getLineCount(parentWindow, args);
                 fireLineWrap(count);
             }
         } else {
             final int lines = parentWindow.getContainer().getNumLines(text);
                 fireLineWrap(lines);
         }
     }
     
     /**
      * Fires the "illegalCommand" method of all validation listeners.
      * 
      * @param reason The reason for the command failure
      */
     private void fireCommandFailure(final String reason) {
         for (InputValidationListener listener : listeners.get(InputValidationListener.class)) {
             listener.illegalCommand(reason);
         }
     }
     
     /**
      * Fires the "legalCommand" method of all validation listeners.
      */
     private void fireCommandPassed() {
         for (InputValidationListener listener : listeners.get(InputValidationListener.class)) {
             listener.legalCommand();
         }
     }
     
     /**
      * Fires the "wrappedText" method of all validation listeners.
      * 
      * @param lines The number of lines that the text will wrap to
      */
     private void fireLineWrap(final int lines) {
         for (InputValidationListener listener : listeners.get(InputValidationListener.class)) {
             listener.wrappedText(lines);
         }        
     }
     
     /**
      * Adds an InputValidationListener to this input handler.
      * 
      * @param listener The listener to be added
      */
     public void addValidationListener(final InputValidationListener listener) {
         listeners.add(InputValidationListener.class, listener);
     }
 
     /**
      * Handles the pressing of a key while the control key is pressed.
      * Inserts control chars as appropriate.
      *
      * @param line Text in the target
      * @param keyCode Keycode for the pressed key
      * @param shiftPressed Was shift pressed
      */
     protected void handleControlKey(final String line, final int keyCode,
             final boolean shiftPressed) {
         switch (keyCode) {
             case KeyEvent.VK_B:
                 addControlCode(Styliser.CODE_BOLD, POSITION_END);
                 break;
 
             case KeyEvent.VK_U:
                 addControlCode(Styliser.CODE_UNDERLINE, POSITION_END);
                 break;
 
             case KeyEvent.VK_O:
                 addControlCode(Styliser.CODE_STOP, POSITION_END);
                 break;
 
             case KeyEvent.VK_I:
                 addControlCode(Styliser.CODE_ITALIC, POSITION_END);
                 break;
 
             case KeyEvent.VK_F:
                 if (shiftPressed) {
                     addControlCode(Styliser.CODE_FIXED, POSITION_END);
                 }
                 break;
 
             case KeyEvent.VK_K:
                 if (shiftPressed) {
                     addControlCode(Styliser.CODE_HEXCOLOUR, POSITION_START);
                     target.showColourPicker(false, true);
                 } else {
                     addControlCode(Styliser.CODE_COLOUR, POSITION_START);
                     target.showColourPicker(true, false);
                 }
                 break;
 
             case KeyEvent.VK_ENTER:
                 if ((flags & HANDLE_RETURN) != 0 && !line.isEmpty()) {
                     commandParser.parseCommandCtrl(parentWindow, line);
                     addToBuffer(line);
                 }
                 break;
 
             default:
                 /* Do nothing. */
                 break;
         }
     }
 
     /**
      * Calls when the user presses the up key.
      * Handles cycling through the input buffer.
      */
     protected void doBufferUp() {
         if ((flags & HANDLE_BACKBUFFER) != 0) {
             if (buffer.hasPrevious()) {
                 target.setText(buffer.getPrevious());
             } else {
                 Toolkit.getDefaultToolkit().beep();
             }
         }
         validateText();
     }
 
     /**
      * Called when the user presses the down key.
      * Handles cycling through the input buffer, and storing incomplete lines.
      */
     protected void doBufferDown() {
         if ((flags & HANDLE_BACKBUFFER) != 0) {
             if (buffer.hasNext()) {
                 target.setText(buffer.getNext());
             } else if (target.getText().isEmpty()) {
                 Toolkit.getDefaultToolkit().beep();
             } else {
                 addToBuffer(target.getText());
                 target.setText("");
             }
         }
         validateText();
     }
     
     /**
      * Retrieves a list of all known entries in the input backbuffer.
      * 
      * @since 0.6
      * @return A copy of the input backbuffer.
      */
     public List<String> getBackBuffer() {
         return new ArrayList<String>(buffer.getList());
     }
 
     /**
      * Handles tab completion of a string. Called when the user presses tab.
      *
      * @deprecated Preferred method is {@link doTabCompletion(boolean)}
      */
     @Deprecated
     protected void doTabCompletion() {
         doTabCompletion(false);
     }
 
     /**
      * Handles tab completion of a string. Called when the user presses
      * (shift) tab.
      *
     * @param shiftPressed True if shift is pressed
      */
     protected void doTabCompletion(final boolean shiftPressed) {
         if (tabCompleter == null || (flags & HANDLE_TABCOMPLETION) == 0) {
             LOGGER.fine("Aborting tab completion. Completer: " + tabCompleter
                     + ", flags: " + flags);
             return;
         }
         
         final String text = target.getText();
 
         LOGGER.finer("Text for tab completion: " + text);
 
         if (text.isEmpty()) {
             doNormalTabCompletion(text, 0, 0, shiftPressed, null);
             return;
         }
 
         final int pos = target.getCaretPosition() - 1;
         int start = (pos < 0) ? 0 : pos;
         int end = (pos < 0) ? 0 : pos;
 
         // Traverse backwards
         while (start > 0 && text.charAt(start) != ' ') {
             start--;
         }
         if (text.charAt(start) == ' ') {
             start++;
         }
 
         // And forwards
         while (end < text.length() && text.charAt(end) != ' ') {
             end++;
         }
 
         if (start > end) {
             end = start;
         }
 
         LOGGER.finer("Offsets: start: " + start + ", end: " + end);
 
         if (start > 0 && text.charAt(0) == CommandManager.getCommandChar()) {
             doCommandTabCompletion(text, start, end, shiftPressed);
         } else {
             doNormalTabCompletion(text, start, end, shiftPressed,  null);
         }
     }
 
     /**
      * Handles potentially intelligent tab completion.
      *
      * @param text The text that is being completed
      * @param start The start index of the word we're completing
      * @param end The end index of the word we're completing
      */
     private void doCommandTabCompletion(final String text, final int start,
             final int end, final boolean shiftPressed) {
         doNormalTabCompletion(text, start, end, shiftPressed,
                 TabCompleter.getIntelligentResults(text.substring(0, start)));
     }
 
     /**
      * Handles normal (non-intelligent-command) tab completion.
      *
      * @param text The text that is being completed
      * @param start The start index of the word we're completing
      * @param end The end index of the word we're completing
      * @param additional A list of additional strings to use
      */
     private void doNormalTabCompletion(final String text, final int start,
             final int end, final boolean shiftPressed,
             final AdditionalTabTargets additional) {
         final TabCompletionResult res = style.getResult(text, start, end,
                 shiftPressed, additional);
 
         if (res != null) {
             target.setText(res.getText());
             target.setCaretPosition(res.getPosition());
         }
     }
 
     /**
      * Called when the user presses return in the text area. The line they
      * typed is added to the buffer for future use.
      * @param line The event that was fired
      */
     public void enterPressed(final String line) {
         if (!line.isEmpty()) {
             final StringBuffer thisBuffer = new StringBuffer(line);
 
             ActionManager.processEvent(CoreActionType.CLIENT_USER_INPUT, null,
                     parentWindow.getContainer(), thisBuffer);
 
             addToBuffer(thisBuffer.toString());
 
             commandParser.parseCommand(parentWindow, thisBuffer.toString());
         }
         fireLineWrap(0);
         fireCommandPassed();
     }
 
     /**
      * Adds the specified control code to the textarea. If the user has a range
      * of text selected, the characters are added before and after, and the
      * caret is positioned based on the position argument.
      * @param code The control code to add
      * @param position The position of the caret after a selection is altered
      */
     protected void addControlCode(final int code, final int position) {
         final String insert = String.valueOf((char) code);
         final int selectionEnd = target.getSelectionEnd();
         final int selectionStart = target.getSelectionStart();
         if (selectionStart < selectionEnd) {
             final String source = target.getText();
             final String before = source.substring(0, selectionStart);
             final String selected = target.getSelectedText();
             final String after =
                     source.substring(selectionEnd, source.length());
             target.setText(before + insert + selected + insert + after);
             if (position == POSITION_START) {
                 target.setCaretPosition(selectionStart + 1);
             } else if (position == POSITION_END) {
                 target.setCaretPosition(selectionEnd + 2);
             }
         } else {
             final int offset = target.getCaretPosition();
             final String source = target.getText();
             final String before = target.getText().substring(0, offset);
             final String after = target.getText().substring(offset,
                     source.length());
             target.setText(before + insert + after);
             target.setCaretPosition(offset + 1);
         }
     }
 
     /**
      * Adds all items in the string array to the buffer.
      *
      * @param lines lines to add to the buffer
      */
     public void addToBuffer(final String[] lines) {
         for (String line : lines) {
             addToBuffer(line);
         }
     }
 
     /**
      * Adds the specified string to the buffer.
      * 
      * @param line The line to be added to the buffer
      */
     public void addToBuffer(final String line) {
         buffer.add(line);
         buffer.seekToEnd();
     }
 
     /** {@inheritDoc} */
     @Override
     public void configChanged(final String domain, final String key) {
         setStyle();
     }
 }
