 /*
 
 [The "BSD licence"]
 Copyright (c) 2005 Jean Bovet
 All rights reserved.
 
 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:
 
 1. Redistributions of source code must retain the above copyright
 notice, this list of conditions and the following disclaimer.
 2. Redistributions in binary form must reproduce the above copyright
 notice, this list of conditions and the following disclaimer in the
 documentation and/or other materials provided with the distribution.
 3. The name of the author may not be used to endorse or promote products
 derived from this software without specific prior written permission.
 
 THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 
 */
 
 package org.antlr.works.debugger;
 
 import edu.usfca.xj.appkit.frame.XJDialog;
 import edu.usfca.xj.appkit.gview.GView;
 import edu.usfca.xj.appkit.utils.XJAlert;
 import edu.usfca.xj.foundation.notification.XJNotificationCenter;
 import org.antlr.runtime.ClassicToken;
 import org.antlr.runtime.Token;
 import org.antlr.works.ate.syntax.misc.ATELine;
 import org.antlr.works.components.grammar.CEditorGrammar;
 import org.antlr.works.debugger.events.DBEvent;
 import org.antlr.works.debugger.input.DBInputTextTokenInfo;
 import org.antlr.works.debugger.local.DBLocal;
 import org.antlr.works.debugger.panels.*;
 import org.antlr.works.debugger.remote.DBRemoteConnectDialog;
 import org.antlr.works.debugger.tivo.DBPlayer;
 import org.antlr.works.debugger.tivo.DBPlayerContextInfo;
 import org.antlr.works.debugger.tivo.DBRecorder;
 import org.antlr.works.debugger.tree.DBASTModel;
 import org.antlr.works.debugger.tree.DBASTPanel;
 import org.antlr.works.debugger.tree.DBParseTreeModel;
 import org.antlr.works.debugger.tree.DBParseTreePanel;
 import org.antlr.works.editor.EditorConsole;
 import org.antlr.works.editor.EditorMenu;
 import org.antlr.works.editor.EditorProvider;
 import org.antlr.works.editor.EditorTab;
 import org.antlr.works.generate.DialogGenerate;
 import org.antlr.works.grammar.EngineGrammar;
 import org.antlr.works.menu.ContextualMenuFactory;
 import org.antlr.works.prefs.AWPrefs;
 import org.antlr.works.stats.StatisticsAW;
 
 import javax.swing.*;
 import javax.swing.text.AttributeSet;
 import javax.swing.text.SimpleAttributeSet;
 import javax.swing.text.StyleConstants;
 import javax.swing.text.StyleContext;
 import java.awt.*;
 import java.awt.event.ActionEvent;
 import java.awt.event.ActionListener;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 
 public class Debugger extends EditorTab implements DBDetachablePanelDelegate {
 
     public static final String DEFAULT_LOCAL_ADDRESS = "localhost";
 
     public static final String NOTIF_DEBUG_STARTED = "NOTIF_DEBUG_STARTED";
     public static final String NOTIF_DEBUG_STOPPED = "NOTIF_DEBUG_STOPPED";
 
     public static final boolean BUILD_AND_DEBUG = true;
     public static final boolean DEBUG = false;
 
     public static final float PERCENT_WIDTH_LEFT = 0.2f;
     public static final float PERCENT_WIDTH_MIDDLE = 0.5f;
 
     protected JPanel panel;
 
     protected DBInputPanel inputPanel;
     protected DBOutputPanel outputPanel;
 
     protected DBParseTreePanel parseTreePanel;
     protected DBParseTreeModel parseTreeModel;
 
     protected DBASTPanel astPanel;
     protected DBASTModel astModel;
 
     protected DBStackPanel stackPanel;
     protected DBEventsPanel eventsPanel;
 
     protected DBControlPanel controlPanel;
 
     protected DBSplitPanel splitPanel;
     protected Map components2toggle;
 
     protected CEditorGrammar editor;
     protected AttributeSet previousGrammarAttributeSet;
     protected int previousGrammarPosition;
 
     protected Set breakpoints;
 
     protected DBLocal local;
     protected DBRecorder recorder;
     protected DBPlayer player;
 
     protected boolean running;
     protected long dateOfModificationOnDisk = 0;
 
     public Debugger(CEditorGrammar editor) {
         this.editor = editor;
     }
 
     public void awake() {
         panel = new JPanel(new BorderLayout());
         splitPanel = new DBSplitPanel();
         components2toggle = new HashMap();
 
         controlPanel = new DBControlPanel(this);
 
         inputPanel = new DBInputPanel(this);
         inputPanel.setTag(DBSplitPanel.LEFT_INDEX);
         outputPanel = new DBOutputPanel(this);
         outputPanel.setTag(DBSplitPanel.LEFT_INDEX);
 
         parseTreePanel = new DBParseTreePanel(this);
         parseTreePanel.setTag(DBSplitPanel.MIDDLE_INDEX);
         parseTreeModel = parseTreePanel.getModel();
 
         astPanel = new DBASTPanel(this);
         astPanel.setTag(DBSplitPanel.MIDDLE_INDEX);
         astModel = astPanel.getModel();
 
         stackPanel = new DBStackPanel(this);
         stackPanel.setTag(DBSplitPanel.RIGHT_INDEX);
         eventsPanel = new DBEventsPanel(this);
         eventsPanel.setTag(DBSplitPanel.RIGHT_INDEX);
 
         panel.add(controlPanel, BorderLayout.NORTH);
         panel.add(splitPanel, BorderLayout.CENTER);
         panel.add(createToggleButtons(), BorderLayout.SOUTH);
 
         local = new DBLocal(this);
         recorder = new DBRecorder(this);
         player = new DBPlayer(this);
 
         updateStatusInfo();
     }
 
     public void componentShouldLayout(Dimension size) {
         assemblePanelsIntoSplitPane(size.width);
         astPanel.componentShouldLayout(new Dimension((int) (size.width*PERCENT_WIDTH_MIDDLE), size.height));
     }
 
     public static final int TOGGLE_INPUT = 0;
     public static final int TOGGLE_OUTPUT = 1;
     public static final int TOGGLE_PTREE = 2;
     public static final int TOGGLE_AST = 3;
     public static final int TOGGLE_STACK = 4;
     public static final int TOGGLE_EVENTS = 5;
 
     public Box createToggleButtons() {
         Box b = Box.createHorizontalBox();
         b.add(createToggleButton("Input", TOGGLE_INPUT, inputPanel));
         b.add(createToggleButton("Output", TOGGLE_OUTPUT, outputPanel));
         b.add(Box.createHorizontalStrut(15));
         b.add(createToggleButton("Parse Tree", TOGGLE_PTREE, parseTreePanel));
         b.add(createToggleButton("AST", TOGGLE_AST, astPanel));
         b.add(Box.createHorizontalStrut(15));
         b.add(createToggleButton("Stack", TOGGLE_STACK, stackPanel));
         b.add(createToggleButton("Events", TOGGLE_EVENTS, eventsPanel));
         b.add(Box.createHorizontalGlue());
         return b;
     }
 
     public JToggleButton createToggleButton(String title, int tag, Component c) {
         DBToggleButton b = new DBToggleButton(title);
         b.setTag(tag);
         b.setFocusable(false);
         b.addActionListener(new ActionListener() {
             public void actionPerformed(ActionEvent e) {
                 performToggleButtonAction((DBToggleButton)e.getSource());
             }
 
         });
         components2toggle.put(c, b);
         return b;
     }
 
     public void assemblePanelsIntoSplitPane(int width) {
         setComponentVisible(inputPanel, true);
         setComponentVisible(outputPanel, false);
 
         setComponentVisible(parseTreePanel, true);
         setComponentVisible(astPanel, false);
 
         setComponentVisible(stackPanel, true);
         setComponentVisible(eventsPanel, false);
 
         splitPanel.setComponentWidth(inputPanel, width*PERCENT_WIDTH_LEFT);
         splitPanel.setComponentWidth(outputPanel, width*PERCENT_WIDTH_LEFT);
 
         splitPanel.setComponentWidth(parseTreePanel, width*PERCENT_WIDTH_MIDDLE);
         splitPanel.setComponentWidth(astPanel, width*PERCENT_WIDTH_MIDDLE);
 
         splitPanel.setComponents(inputPanel, parseTreePanel, stackPanel);
     }
 
     public void setComponentVisible(Component c, boolean flag) {
         c.setVisible(flag);
         JToggleButton b = (JToggleButton)components2toggle.get(c);
         b.setSelected(flag);
     }
 
     public void performToggleButtonAction(DBToggleButton button) {
         switch(button.getTag()) {
             case TOGGLE_INPUT:
                 toggleComponents(inputPanel, outputPanel, DBSplitPanel.LEFT_INDEX);
                 break;
             case TOGGLE_OUTPUT:
                 toggleComponents(outputPanel, inputPanel, DBSplitPanel.LEFT_INDEX);
                 break;
 
             case TOGGLE_PTREE:
                 toggleComponents(parseTreePanel, astPanel, DBSplitPanel.MIDDLE_INDEX);
                 break;
             case TOGGLE_AST:
                 toggleComponents(astPanel, parseTreePanel, DBSplitPanel.MIDDLE_INDEX);
                 break;
 
             case TOGGLE_STACK:
                 toggleComponents(stackPanel, eventsPanel, DBSplitPanel.RIGHT_INDEX);
                 break;
             case TOGGLE_EVENTS:
                 toggleComponents(eventsPanel, stackPanel, DBSplitPanel.RIGHT_INDEX);
                 break;
         }
     }
 
     public void toggleComponents(DBDetachablePanel c, DBDetachablePanel other, int index) {
         c.setVisible(!c.isVisible());
         if(c.isVisible()) {
             if(!other.isDetached())
                 setComponentVisible(other, false);
             if(!c.isDetached())
                 splitPanel.setComponent(c, index);
         } else {
             if(other.isVisible() && !other.isDetached())
                 splitPanel.setComponent(other, index);
             else
                 splitPanel.setComponent(null, index);
         }
     }
 
     public void toggleInputTokensBox() {
         inputPanel.toggleInputTokensBox();
     }
 
     public boolean isInputTokenVisible() {
         return inputPanel.isInputTokensBoxVisible();
     }
 
     public void selectConsoleTab() {
         editor.selectConsoleTab();
     }
 
     public DBOutputPanel getOutputPanel() {
         return outputPanel;
     }
 
     public DBRecorder getRecorder() {
         return recorder;
     }
 
     public DBPlayer getPlayer() {
         return player;
     }
 
     public Container getWindowComponent() {
         return editor.getWindowContainer();
     }
 
     public EditorConsole getConsole() {
         return editor.getConsole();
     }
 
     public EditorProvider getProvider() {
         return editor;
     }
 
     public void close() {
         debuggerStop(true);
         inputPanel.close();
         parseTreeModel.close();
     }
 
     public Container getContainer() {
         return panel;
     }
 
     public void updateStatusInfo() {
         controlPanel.updateStatusInfo();
     }
 
     public void breaksOnEvent() {
         inputPanel.updateOnBreakEvent();
         parseTreePanel.updateOnBreakEvent();
         astPanel.updateOnBreakEvent();
         stackPanel.updateOnBreakEvent();
         eventsPanel.updateOnBreakEvent();
     }
 
     public EngineGrammar getGrammar() {
         return editor.getEngineGrammar();
     }
 
     public boolean needsToGenerateGrammar() {
         return dateOfModificationOnDisk != editor.getDocument().getDateOfModificationOnDisk()
                 || editor.getDocument().isDirty();
     }
 
     public void grammarGenerated() {
         editor.getDocument().performAutoSave();
         dateOfModificationOnDisk = editor.getDocument().getDateOfModificationOnDisk();
     }
 
     public void queryGrammarBreakpoints() {
         this.breakpoints = editor.breakpointManager.getBreakpoints();
     }
 
     public boolean isBreakpointAtLine(int line) {
         if(breakpoints == null)
             return false;
         else
             return breakpoints.contains(new Integer(line));
     }
 
     public boolean isBreakpointAtToken(Token token) {
         return inputPanel.isBreakpointAtToken(token);
     }
 
     public void selectToken(Token token, int line, int pos) {
         if(token != null) {
             /** If token is not null, ask the input text object the
              * line and character number.
              */
 
             DBInputTextTokenInfo info = inputPanel.getTokenInfoForToken(token);
             if(info == null)
                 setGrammarPosition(line, pos);
             else
                 setGrammarPosition(info.line, info.charInLine);
         } else {
             /** If token is null, the line and pos will be provided as parameters */
             setGrammarPosition(line, pos);
         }
 
         inputPanel.selectToken(token);
         parseTreePanel.selectToken(token);
         astPanel.selectToken(token);
     }
 
     public int grammarIndex;
 
     public void setGrammarPosition(int line, int pos) {
         grammarIndex = computeAbsoluteGrammarIndex(line, pos);
         if(grammarIndex >= 0) {
             if(editor.getTextPane().hasFocus()) {
                 // If the text pane will lose its focus,
                 // delay the text selection otherwise
                 // the selection will be hidden
                 SwingUtilities.invokeLater(new Runnable() {
                     public void run() {
                         editor.selectTextRange(grammarIndex, grammarIndex+1);
                     }
                 });
             } else
                 editor.selectTextRange(grammarIndex, grammarIndex+1);
         }
     }
 
     public void markLocationInGrammar(int index) {
         try {
             editor.setCaretPosition(index);
             storeGrammarAttributeSet(index);
 
             StyleContext sc = StyleContext.getDefaultStyleContext();
             AttributeSet attr = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Background, Color.red);
             editor.getTextPane().getStyledDocument().setCharacterAttributes(index, 1, attr, false);
         } catch(Exception e) {
             getConsole().print(e);
         }
     }
 
     public List getRules() {
         return editor.getRules();
     }
 
     public List getSortedRules() {
         return editor.getSortedRules();
     }
 
     public String getEventsAsString() {
         return eventsPanel.getEventsAsString();
     }
 
     public void launchLocalDebugger(boolean buildAndDebug) {
         // If the grammar is dirty, build it anyway
 
         if(needsToGenerateGrammar())
             buildAndDebug = true;
 
         if(buildAndDebug && !editor.ensureDocumentSaved())
             return;
 
         if(buildAndDebug || !local.isRequiredFilesExisting()) {
             DialogGenerate dialog = new DialogGenerate(getWindowComponent());
             dialog.setDebugOnly();
             if(dialog.runModal() == XJDialog.BUTTON_OK) {
                 local.setOutputPath(dialog.getOutputPath());
                 local.prepareAndLaunch(BUILD_AND_DEBUG);
 
                 grammarGenerated();
             }
         } else {
             local.prepareAndLaunch(DEBUG);
         }
     }
 
     public void debuggerLocalDidRun(boolean builtAndDebug) {
         if(builtAndDebug)
             StatisticsAW.shared().recordEvent(StatisticsAW.EVENT_LOCAL_DEBUGGER_BUILD);
         else
             StatisticsAW.shared().recordEvent(StatisticsAW.EVENT_LOCAL_DEBUGGER);
         debuggerLaunch(DEFAULT_LOCAL_ADDRESS, AWPrefs.getDebugDefaultLocalPort(), false);
     }
 
     public void launchRemoteDebugger() {
         StatisticsAW.shared().recordEvent(StatisticsAW.EVENT_REMOTE_DEBUGGER);
         DBRemoteConnectDialog dialog = new DBRemoteConnectDialog(getWindowComponent());
         if(dialog.runModal() == XJDialog.BUTTON_OK) {
             debuggerLaunch(dialog.getAddress(), dialog.getPort(), true);
         }
     }
 
     public void debuggerLaunch(String address, int port, boolean remote) {
         if(remote && !debuggerLaunchGrammar()) {
             XJAlert.display(editor.getWindowContainer(), "Error",
                     "Cannot launch the debugger.\nException while parsing grammar.");
             return;
         }
 
         queryGrammarBreakpoints();
 
         inputPanel.prepareForGrammar(getGrammar());
         player.setInputBuffer(inputPanel.getInputBuffer());
 
         recorder.connect(address, port);
     }
 
     public void connectionSuccess() {
         // First set the flag to true before doing anything else
         // (don't send the notification before for example)
         running = true;
 
         XJNotificationCenter.defaultCenter().postNotification(this, NOTIF_DEBUG_STARTED);
         editor.selectDebuggerTab();
 
         editor.console.makeCurrent();
 
         editor.getTextPane().setEditable(false);
         editor.getTextPane().requestFocus(false);
         
         previousGrammarAttributeSet = null;
         player.resetPlayEvents(true);
     }
 
     public void connectionFailed() {
         XJAlert.display(editor.getWindowContainer(), "Connection Error",
                 "Cannot launch the debugger.\nTime-out waiting to connect to the remote parser.");
     }
 
     public void connectionCancelled() {
     }
 
     public boolean debuggerLaunchGrammar() {
         try {
             getGrammar().analyze();
         } catch (Exception e) {
             editor.getConsole().print(e);
             return false;
         }
         return true;
     }
 
     public void debuggerStop(boolean force) {
         if(recorder.getStatus() == DBRecorder.STATUS_STOPPING) {
             if(force || XJAlert.displayAlertYESNO(editor.getWindowContainer(), "Stopping", "The debugger is currently stopping. Do you want to force stop it ?") == XJAlert.YES) {
                 local.forceStop();
                 recorder.stop();
             }
         } else
             recorder.requestStop();
     }
 
     public boolean isRunning() {
         return running;
     }
 
     public void resetGUI() {
         stackPanel.clear();
         eventsPanel.clear();
         parseTreePanel.clear();
         astPanel.clear();
     }
 
     public void storeGrammarAttributeSet(int index) {
         previousGrammarPosition = index;
         previousGrammarAttributeSet = editor.getTextPane().getStyledDocument().getCharacterElement(index+1).getAttributes();
     }
 
     public void restorePreviousGrammarAttributeSet() {
         if(previousGrammarAttributeSet != null) {
             editor.getTextPane().getStyledDocument().setCharacterAttributes(previousGrammarPosition, 1, previousGrammarAttributeSet, true);
             previousGrammarAttributeSet = null;
         }
     }
 
     public int computeAbsoluteGrammarIndex(int lineIndex, int pos) {
         List lines = editor.getLines();
         if(lineIndex-1<0 || lineIndex-1 >= lines.size())
             return -1;
 
         ATELine line = (ATELine)lines.get(lineIndex-1);
         return line.position+pos-1;
     }
 
     public void addEvent(DBEvent event, DBPlayerContextInfo info) {
         eventsPanel.addEvent(event, info);
     }
 
     public void playEvents(List events, boolean reset) {
         player.playEvents(events, reset);
         breaksOnEvent();
     }
 
     public void playerSetLocation(int line, int pos) {
         parseTreeModel.setLocation(line, pos);
     }
 
     public void playerPushRule(String ruleName) {
         stackPanel.pushRule(ruleName);
         parseTreeModel.pushRule(ruleName);
         astModel.pushRule(ruleName);
     }
 
     public void playerPopRule(String ruleName) {
         stackPanel.popRule();
         parseTreeModel.popRule();
         astModel.popRule();
     }
 
     public void playerConsumeToken(Token token) {
         parseTreeModel.addToken(token);
     }
 
     public void playerRecognitionException(Exception e) {
         parseTreeModel.addException(e);
     }
 
     public void playerBeginBacktrack(int level) {
         parseTreeModel.beginBacktrack(level);
     }
 
     public void playerEndBacktrack(int level, boolean success) {
         parseTreeModel.endBacktrack(level, success);
     }
 
     public void playerNilNode(int id) {
         astModel.nilNode(id);
     }
 
     public void playerCreateNode(int id, Token token) {
         astModel.createNode(id, token);
     }
 
     public void playerCreateNode(int id, String text, int type) {
         astModel.createNode(id, new ClassicToken(type, text));
     }
 
     public void playerBecomeRoot(int newRootID, int oldRootID) {
         astModel.becomeRoot(newRootID, oldRootID);
     }
 
     public void playerAddChild(int rootID, int childID) {
         astModel.addChild(rootID, childID);
     }
 
     public void playerSetTokenBoundaries(int id, int startIndex, int stopIndex) {
         /** Currently ignored */
     }
 
     public void recorderStatusDidChange() {
         SwingUtilities.invokeLater(new Runnable() {
             public void run() {
                 updateStatusInfo();
             }
         });
     }
 
     public void recorderDidStop() {
         SwingUtilities.invokeLater(new Runnable() {
             public void run() {
                 restorePreviousGrammarAttributeSet();
                 editor.getTextPane().setEditable(true);
                 editor.getTextPane().requestFocus();
                 inputPanel.stop();
                 running = false;
                 editor.refreshMainMenuBar();
                 XJNotificationCenter.defaultCenter().postNotification(this, NOTIF_DEBUG_STOPPED);
             }
         });
     }
 
     public boolean canExportToBitmap() {
         return true;
     }
 
     public boolean canExportToEPS() {
         return true;
     }
 
     public GView getExportableGView() {
         // @todo finish
         /*if(treeTabbedPane.getSelectedComponent() == parseTreePanel)
             return parseTreePanel.getGraphView();
         else
             return astPanel.getGraphView();*/
         return null;
     }
 
     public String getTabName() {
         return "Debugger";
     }
 
     public Component getTabComponent() {
         return getContainer();
     }
 
     public JPopupMenu treeGetContextualMenu() {
         ContextualMenuFactory factory = new ContextualMenuFactory(editor.editorMenu);
         factory.addItem(EditorMenu.MI_EXPORT_AS_EPS);
         factory.addItem(EditorMenu.MI_EXPORT_AS_IMAGE);
         return factory.menu;
     }
 
     public static final String KEY_SPLITPANE_A = "KEY_SPLITPANE_A";
     public static final String KEY_SPLITPANE_B = "KEY_SPLITPANE_B";
     public static final String KEY_SPLITPANE_C = "KEY_SPLITPANE_C";
 
     public void setPersistentData(Map data) {
         if(data == null)
             return;
 
 /*        Integer i = (Integer)data.get(KEY_SPLITPANE_A);
         if(i != null)
             ioSplitPane.setDividerLocation(i.intValue());
 
         i = (Integer)data.get(KEY_SPLITPANE_B);
         if(i != null)
             ioTreeSplitPane.setDividerLocation(i.intValue());
 
         i = (Integer)data.get(KEY_SPLITPANE_C);
         if(i != null)
             treeInfoPanelSplitPane.setDividerLocation(i.intValue());*/
     }
 
     public Map getPersistentData() {
         /*Map data = new HashMap();
         data.put(KEY_SPLITPANE_A, new Integer(ioSplitPane.getDividerLocation()));
         data.put(KEY_SPLITPANE_B, new Integer(ioTreeSplitPane.getDividerLocation()));
         data.put(KEY_SPLITPANE_C, new Integer(treeInfoPanelSplitPane.getDividerLocation()));
         return data;*/
         return new HashMap();
     }
 
     public void panelDoDetach(DBDetachablePanel panel) {
         splitPanel.setComponent(null, panel.getTag());
     }
 
     public void panelDoAttach(DBDetachablePanel panel) {
        Component c = splitPanel.getComponentAtIndex(panel.getTag());
        if(c != null) {
            c.setVisible(false);
            splitPanel.setComponent(null, panel.getTag());

            DBToggleButton button = (DBToggleButton) components2toggle.get(c);
            button.setSelected(false);
        }
         splitPanel.setComponent(panel, panel.getTag());
     }
 
     public void panelDoClose(DBDetachablePanel panel) {
         DBToggleButton button = (DBToggleButton) components2toggle.get(panel);
         button.setSelected(false);
     }
 
     public Container panelParentContainer() {
         return editor.getJavaContainer();
     }
 
 }
