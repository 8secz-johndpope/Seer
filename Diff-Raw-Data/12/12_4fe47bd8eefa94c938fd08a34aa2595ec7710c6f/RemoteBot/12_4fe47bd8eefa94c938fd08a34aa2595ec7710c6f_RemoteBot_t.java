 package de.fu_berlin.inf.dpp.stf.server.rmi.remotebot.impl;
 
 import java.io.File;
 import java.rmi.RemoteException;
 import java.util.ArrayList;
 import java.util.Calendar;
 import java.util.List;
 
 import org.apache.log4j.Logger;
 import org.eclipse.core.resources.ResourcesPlugin;
 import org.eclipse.swtbot.swt.finder.SWTBot;
 import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
 import org.eclipse.swtbot.swt.finder.waits.DefaultCondition;
 import org.eclipse.swtbot.swt.finder.waits.ICondition;
 import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
 
 import de.fu_berlin.inf.dpp.stf.server.StfRemoteObject;
 import de.fu_berlin.inf.dpp.stf.server.bot.SarosSWTBotPreferences;
 import de.fu_berlin.inf.dpp.stf.server.rmi.remotebot.IRemoteBot;
 import de.fu_berlin.inf.dpp.stf.server.rmi.remotebot.widget.IRemoteBotButton;
 import de.fu_berlin.inf.dpp.stf.server.rmi.remotebot.widget.IRemoteBotCCombo;
 import de.fu_berlin.inf.dpp.stf.server.rmi.remotebot.widget.IRemoteBotCLabel;
 import de.fu_berlin.inf.dpp.stf.server.rmi.remotebot.widget.IRemoteBotCTabItem;
 import de.fu_berlin.inf.dpp.stf.server.rmi.remotebot.widget.IRemoteBotCheckBox;
 import de.fu_berlin.inf.dpp.stf.server.rmi.remotebot.widget.IRemoteBotCombo;
 import de.fu_berlin.inf.dpp.stf.server.rmi.remotebot.widget.IRemoteBotLabel;
 import de.fu_berlin.inf.dpp.stf.server.rmi.remotebot.widget.IRemoteBotList;
 import de.fu_berlin.inf.dpp.stf.server.rmi.remotebot.widget.IRemoteBotMenu;
 import de.fu_berlin.inf.dpp.stf.server.rmi.remotebot.widget.IRemoteBotRadio;
 import de.fu_berlin.inf.dpp.stf.server.rmi.remotebot.widget.IRemoteBotShell;
 import de.fu_berlin.inf.dpp.stf.server.rmi.remotebot.widget.IRemoteBotStyledText;
 import de.fu_berlin.inf.dpp.stf.server.rmi.remotebot.widget.IRemoteBotTable;
 import de.fu_berlin.inf.dpp.stf.server.rmi.remotebot.widget.IRemoteBotText;
 import de.fu_berlin.inf.dpp.stf.server.rmi.remotebot.widget.IRemoteBotToggleButton;
 import de.fu_berlin.inf.dpp.stf.server.rmi.remotebot.widget.IRemoteBotToolbarButton;
 import de.fu_berlin.inf.dpp.stf.server.rmi.remotebot.widget.IRemoteBotTree;
 import de.fu_berlin.inf.dpp.stf.server.rmi.remotebot.widget.impl.RemoteBotButton;
 import de.fu_berlin.inf.dpp.stf.server.rmi.remotebot.widget.impl.RemoteBotCCombo;
 import de.fu_berlin.inf.dpp.stf.server.rmi.remotebot.widget.impl.RemoteBotCLabel;
 import de.fu_berlin.inf.dpp.stf.server.rmi.remotebot.widget.impl.RemoteBotCTabItem;
 import de.fu_berlin.inf.dpp.stf.server.rmi.remotebot.widget.impl.RemoteBotCheckBox;
 import de.fu_berlin.inf.dpp.stf.server.rmi.remotebot.widget.impl.RemoteBotCombo;
 import de.fu_berlin.inf.dpp.stf.server.rmi.remotebot.widget.impl.RemoteBotLabel;
 import de.fu_berlin.inf.dpp.stf.server.rmi.remotebot.widget.impl.RemoteBotList;
 import de.fu_berlin.inf.dpp.stf.server.rmi.remotebot.widget.impl.RemoteBotMenu;
 import de.fu_berlin.inf.dpp.stf.server.rmi.remotebot.widget.impl.RemoteBotRadio;
 import de.fu_berlin.inf.dpp.stf.server.rmi.remotebot.widget.impl.RemoteBotShell;
 import de.fu_berlin.inf.dpp.stf.server.rmi.remotebot.widget.impl.RemoteBotStyledText;
 import de.fu_berlin.inf.dpp.stf.server.rmi.remotebot.widget.impl.RemoteBotTable;
 import de.fu_berlin.inf.dpp.stf.server.rmi.remotebot.widget.impl.RemoteBotText;
 import de.fu_berlin.inf.dpp.stf.server.rmi.remotebot.widget.impl.RemoteBotToggleButton;
 import de.fu_berlin.inf.dpp.stf.server.rmi.remotebot.widget.impl.RemoteBotToolbarButton;
 import de.fu_berlin.inf.dpp.stf.server.rmi.remotebot.widget.impl.RemoteBotTree;
 
 public abstract class RemoteBot extends StfRemoteObject implements IRemoteBot {
 
     private static final Logger log = Logger.getLogger(RemoteBot.class);
     private static final File SCREENSHOT_DIRECTORY;
 
     static {
         Calendar calendar = Calendar.getInstance();
 
         File file = ResourcesPlugin.getWorkspace().getRoot().getLocation()
             .toFile();
         file = new File(file, ".metadata");
         file = new File(file, "saros_screenshots");
         file = new File(file, calendar.get(Calendar.MONTH) + "_"
             + calendar.get(Calendar.DAY_OF_MONTH) + "_"
             + calendar.get(Calendar.YEAR));
 
         SCREENSHOT_DIRECTORY = file;
     }
 
     protected SWTBot swtBot;
 
     protected RemoteBotShell shell;
     protected RemoteBotButton button;
     protected RemoteBotTree tree;
     protected RemoteBotLabel label;
     protected RemoteBotStyledText styledText;
     protected RemoteBotCombo comboBox;
     protected RemoteBotCCombo ccomboBox;
     protected RemoteBotToolbarButton toolbarButton;
     protected RemoteBotText text;
     protected RemoteBotTable table;
     protected RemoteBotMenu menu;
     protected RemoteBotList list;
     protected RemoteBotCheckBox checkbox;
     protected RemoteBotRadio radio;
     protected RemoteBotToggleButton toggleButton;
     protected RemoteBotCTabItem cTabItem;
 
     protected RemoteBotCLabel clabel;
 
     protected RemoteBot() {
         swtBot = new SWTBot();
         shell = RemoteBotShell.getInstance();
         button = RemoteBotButton.getInstance();
         tree = RemoteBotTree.getInstance();
         label = RemoteBotLabel.getInstance();
         styledText = RemoteBotStyledText.getInstance();
         comboBox = RemoteBotCombo.getInstance();
         ccomboBox = RemoteBotCCombo.getInstance();
         toolbarButton = RemoteBotToolbarButton.getInstance();
         text = RemoteBotText.getInstance();
         table = RemoteBotTable.getInstance();
         menu = RemoteBotMenu.getInstance();
         list = RemoteBotList.getInstance();
         checkbox = RemoteBotCheckBox.getInstance();
         radio = RemoteBotRadio.getInstance();
         cTabItem = RemoteBotCTabItem.getInstance();
         toggleButton = RemoteBotToggleButton.getInstance();
 
         clabel = RemoteBotCLabel.getInstance();
 
     }
 
     public void setBot(SWTBot bot) {
         swtBot = bot;
     }
 
     public IRemoteBotTree tree() throws RemoteException {
         tree.setWidget(swtBot.tree());
         return tree;
     }
 
     public IRemoteBotTree treeWithLabel(String label) throws RemoteException {
         tree.setWidget(swtBot.treeWithLabel(label));
         return tree;
     }
 
     public IRemoteBotTree treeWithLabel(String label, int index)
         throws RemoteException {
         tree.setWidget(swtBot.treeWithLabel(label, index));
         return tree;
     }
 
     public IRemoteBotTree treeWithId(String key, String value)
         throws RemoteException {
         tree.setWidget(swtBot.treeWithId(key, value));
         return tree;
     }
 
     public IRemoteBotTree treeWithId(String key, String value, int index)
         throws RemoteException {
         tree.setWidget(swtBot.treeWithId(key, value, index));
         return tree;
     }
 
     public IRemoteBotTree treeWithId(String value) throws RemoteException {
         tree.setWidget(swtBot.treeWithId(value));
         return tree;
     }
 
     public IRemoteBotTree treeWithId(String value, int index)
         throws RemoteException {
         tree.setWidget(swtBot.treeWithId(value, index));
         return tree;
     }
 
     public IRemoteBotTree treeInGroup(String inGroup) throws RemoteException {
         tree.setWidget(swtBot.treeInGroup(inGroup));
         return tree;
     }
 
     public IRemoteBotTree treeInGroup(String inGroup, int index)
         throws RemoteException {
         tree.setWidget(swtBot.treeInGroup(inGroup, index));
         return tree;
     }
 
     public IRemoteBotTree tree(int index) throws RemoteException {
         tree.setWidget(swtBot.tree(index));
         return tree;
     }
 
     public IRemoteBotTree treeWithLabelInGroup(String label, String inGroup)
         throws RemoteException {
         tree.setWidget(swtBot.treeWithLabelInGroup(label, inGroup));
         return tree;
     }
 
     public IRemoteBotTree treeWithLabelInGroup(String label, String inGroup,
         int index) throws RemoteException {
         tree.setWidget(swtBot.treeWithLabelInGroup(label, inGroup, index));
         return tree;
     }
 
     public IRemoteBotShell shell(String title) throws RemoteException {
         return shell.setWidget(swtBot.shell(title));
 
     }
 
     public List<String> getOpenShellNames() throws RemoteException {
         ArrayList<String> list = new ArrayList<String>();
         for (SWTBotShell shell : swtBot.shells())
             list.add(shell.getText());
 
         log.trace("Currently opened shells: " + list.toString());
         return list;
     }
 
     public boolean isShellOpen(String title) throws RemoteException {
         return getOpenShellNames().contains(title);
     }
 
     public void waitLongUntilShellIsClosed(final String title)
         throws RemoteException {
         waitUntil(new DefaultCondition() {
             public boolean test() throws Exception {
                 return !isShellOpen(title);
             }
 
             public String getFailureMessage() {
                 return "waiting for shell '" + title + "' to close";
             }
         });
     }
 
     public void waitUntilShellIsClosed(final String title)
         throws RemoteException {
         waitUntil(new DefaultCondition() {
             public boolean test() throws Exception {
                 return !isShellOpen(title);
             }
 
             public String getFailureMessage() {
                 return "waiting for shell '" + title + "' to close";
             }
         });
     }
 
     public void waitUntilShellIsOpen(final String title) throws RemoteException {
         waitUntil(new DefaultCondition() {
             public boolean test() throws Exception {
                 return isShellOpen(title);
             }
 
             public String getFailureMessage() {
                 return "waiting for shell '" + title + "' to open";
             }
         });
     }
 
     public void waitLongUntilShellIsOpen(final String title)
         throws RemoteException {
         waitLongUntil(new DefaultCondition() {
             public boolean test() throws Exception {
                 return isShellOpen(title);
             }
 
             public String getFailureMessage() {
                 return "waiting for shell '" + title + "' to open";
             }
         });
     }
 
     public IRemoteBotShell activeShell() throws RemoteException {
         return shell.setWidget(swtBot.activeShell());
 
     }
 
     public String getTextOfActiveShell() throws RemoteException {
         final SWTBotShell activeShell = swtBot.activeShell();
         return activeShell == null ? null : activeShell.getText();
     }
 
     public IRemoteBotButton buttonWithLabel(String label)
         throws RemoteException {
         button.setWidget(swtBot.buttonWithLabel(label, 0));
         return button;
     }
 
     public IRemoteBotButton buttonWithLabel(String label, int index)
         throws RemoteException {
         button.setWidget(swtBot.buttonWithLabel(label, index));
         return button;
     }
 
     public IRemoteBotButton button(String mnemonicText) throws RemoteException {
         button.setWidget(swtBot.button(mnemonicText, 0));
         return button;
     }
 
     public IRemoteBotButton button(String mnemonicText, int index)
         throws RemoteException {
         button.setWidget(swtBot.button(mnemonicText, index));
         return button;
     }
 
     public IRemoteBotButton buttonWithTooltip(String tooltip)
         throws RemoteException {
         button.setWidget(swtBot.buttonWithTooltip(tooltip, 0));
         return button;
     }
 
     public IRemoteBotButton buttonWithTooltip(String tooltip, int index)
         throws RemoteException {
         button.setWidget(swtBot.buttonWithTooltip(tooltip, index));
         return button;
     }
 
     public IRemoteBotButton buttonWithId(String key, String value)
         throws RemoteException {
         button.setWidget(swtBot.buttonWithId(key, value));
         return button;
     }
 
     public IRemoteBotButton buttonWithId(String key, String value, int index)
         throws RemoteException {
         button.setWidget(swtBot.buttonWithId(key, value, index));
         return button;
     }
 
     public IRemoteBotButton buttonWithId(String value) throws RemoteException {
         button.setWidget(swtBot.buttonWithId(value));
         return button;
     }
 
     public IRemoteBotButton buttonWithId(String value, int index)
         throws RemoteException {
         button.setWidget(swtBot.buttonWithId(value, index));
         return button;
     }
 
     public IRemoteBotButton buttonInGroup(String inGroup)
         throws RemoteException {
         button.setWidget(swtBot.buttonInGroup(inGroup));
         return button;
     }
 
     public IRemoteBotButton buttonInGroup(String inGroup, int index)
         throws RemoteException {
         button.setWidget(swtBot.buttonInGroup(inGroup, index));
         return button;
     }
 
     public IRemoteBotButton button() throws RemoteException {
         button.setWidget(swtBot.button());
         return button;
     }
 
     public IRemoteBotButton button(int index) throws RemoteException {
         button.setWidget(swtBot.button(index));
         return button;
     }
 
     public IRemoteBotButton buttonWithLabelInGroup(String label, String inGroup)
         throws RemoteException {
         button.setWidget(swtBot.buttonWithLabelInGroup(label, inGroup));
         return button;
     }
 
     public IRemoteBotButton buttonWithLabelInGroup(String label,
         String inGroup, int index) throws RemoteException {
         button.setWidget(swtBot.buttonWithLabelInGroup(label, inGroup, index));
         return button;
     }
 
     public IRemoteBotButton buttonInGroup(String mnemonicText, String inGroup)
         throws RemoteException {
         button.setWidget(swtBot.buttonInGroup(mnemonicText, inGroup));
         return button;
     }
 
     public IRemoteBotButton buttonInGroup(String mnemonicText, String inGroup,
         int index) throws RemoteException {
         button.setWidget(swtBot.buttonInGroup(mnemonicText, inGroup, index));
         return button;
     }
 
     public IRemoteBotButton buttonWithTooltipInGroup(String tooltip,
         String inGroup) throws RemoteException {
         button.setWidget(swtBot.buttonWithTooltipInGroup(tooltip, inGroup));
         return button;
     }
 
     public IRemoteBotButton buttonWithTooltipInGroup(String tooltip,
         String inGroup, int index) throws RemoteException {
         button.setWidget(swtBot.buttonWithTooltipInGroup(tooltip, inGroup,
             index));
         return button;
     }
 
     public IRemoteBotLabel label() throws RemoteException {
         label.setWidget(swtBot.label());
         return label;
     }
 
     public IRemoteBotLabel label(String mnemonicText) throws RemoteException {
         label.setWidget(swtBot.label(mnemonicText));
         return label;
     }
 
     public IRemoteBotLabel label(String mnemonicText, int index)
         throws RemoteException {
         label.setWidget(swtBot.label(mnemonicText, index));
         return label;
 
     }
 
     public IRemoteBotLabel labelWithId(String key, String value)
         throws RemoteException {
         label.setWidget(swtBot.labelWithId(key, value));
         return label;
 
     }
 
     public IRemoteBotLabel labelWithId(String key, String value, int index)
         throws RemoteException {
         label.setWidget(swtBot.labelWithId(key, value, index));
         return label;
 
     }
 
     public IRemoteBotLabel labelWithId(String value) throws RemoteException {
         label.setWidget(swtBot.labelWithId(value));
         return label;
 
     }
 
     public IRemoteBotLabel labelWithId(String value, int index)
         throws RemoteException {
         label.setWidget(swtBot.labelWithId(value, index));
         return label;
 
     }
 
     public IRemoteBotLabel labelInGroup(String inGroup) throws RemoteException {
         label.setWidget(swtBot.labelInGroup(inGroup));
         return label;
 
     }
 
     public IRemoteBotLabel labelInGroup(String inGroup, int index)
         throws RemoteException {
         label.setWidget(swtBot.labelInGroup(inGroup, index));
         return label;
 
     }
 
     public IRemoteBotLabel label(int index) throws RemoteException {
         label.setWidget(swtBot.label(index));
         return label;
 
     }
 
     public IRemoteBotLabel labelInGroup(String mnemonicText, String inGroup)
         throws RemoteException {
         label.setWidget(swtBot.labelInGroup(MENU_CLASS, inGroup));
         return label;
 
     }
 
     public IRemoteBotLabel labelInGroup(String mnemonicText, String inGroup,
         int index) throws RemoteException {
         label.setWidget(swtBot.labelInGroup(mnemonicText, inGroup, index));
         return label;
 
     }
 
     public boolean existsLabel() throws RemoteException {
         try {
             swtBot.label();
             return true;
         } catch (WidgetNotFoundException e) {
             return false;
         }
     }
 
     public boolean existsLabelInGroup(String groupName) throws RemoteException {
 
         try {
             swtBot.labelInGroup(groupName);
             return true;
         } catch (WidgetNotFoundException e) {
             return false;
         }
     }
 
     public boolean existsLabel(String text) throws RemoteException {
 
         try {
             swtBot.label(text);
             return true;
         } catch (WidgetNotFoundException e) {
             return false;
         }
     }
 
     public boolean existsStyledText() throws RemoteException {
         try {
             swtBot.styledText();
             return true;
         } catch (WidgetNotFoundException e) {
             return false;
         }
     }
 
     public IRemoteBotStyledText styledTextWithLabel(String label)
         throws RemoteException {
         styledText.setWidget(swtBot.styledTextWithLabel(label, 0));
         return styledText;
     }
 
     public IRemoteBotStyledText styledTextWithLabel(String label, int index)
         throws RemoteException {
         styledText.setWidget(swtBot.styledTextWithLabel(label, index));
         return styledText;
     }
 
     public IRemoteBotStyledText styledText(String text) throws RemoteException {
         styledText.setWidget(swtBot.styledText(text));
         return styledText;
     }
 
     public IRemoteBotStyledText styledText(String text, int index)
         throws RemoteException {
         styledText.setWidget(swtBot.styledText(text, index));
         return styledText;
     }
 
     public IRemoteBotStyledText styledTextWithId(String key, String value)
         throws RemoteException {
         styledText.setWidget(swtBot.styledTextWithId(key, value));
         return styledText;
     }
 
     public IRemoteBotStyledText styledTextWithId(String key, String value,
         int index) throws RemoteException {
         styledText.setWidget(swtBot.styledTextWithId(key, value, index));
         return styledText;
     }
 
     public IRemoteBotStyledText styledTextWithId(String value)
         throws RemoteException {
         styledText.setWidget(swtBot.styledTextWithId(value));
         return styledText;
     }
 
     public IRemoteBotStyledText styledTextWithId(String value, int index)
         throws RemoteException {
         styledText.setWidget(swtBot.styledTextWithId(value, index));
         return styledText;
     }
 
     public IRemoteBotStyledText styledTextInGroup(String inGroup)
         throws RemoteException {
         styledText.setWidget(swtBot.styledTextInGroup(inGroup));
         return styledText;
     }
 
     public IRemoteBotStyledText styledTextInGroup(String inGroup, int index)
         throws RemoteException {
         styledText.setWidget(swtBot.styledTextInGroup(inGroup, index));
         return styledText;
     }
 
     public IRemoteBotStyledText styledText() throws RemoteException {
         return styledText(0);
     }
 
     public IRemoteBotStyledText styledText(int index) throws RemoteException {
         styledText.setWidget(swtBot.styledText(index));
         return styledText;
     }
 
     public IRemoteBotStyledText styledTextWithLabelInGroup(String label,
         String inGroup) throws RemoteException {
         return styledTextWithLabelInGroup(label, inGroup, 0);
     }
 
     public IRemoteBotStyledText styledTextWithLabelInGroup(String label,
         String inGroup, int index) throws RemoteException {
         styledText.setWidget(swtBot.styledTextWithLabelInGroup(label, inGroup,
             index));
         return styledText;
     }
 
     public IRemoteBotStyledText styledTextInGroup(String text, String inGroup)
         throws RemoteException {
         return styledTextInGroup(text, inGroup, 0);
     }
 
     public IRemoteBotStyledText styledTextInGroup(String text, String inGroup,
         int index) throws RemoteException {
         styledText.setWidget(swtBot.styledTextInGroup(text, inGroup, index));
         return styledText;
     }
 
     public IRemoteBotCombo comboBoxWithLabel(String label)
         throws RemoteException {
         comboBox.setWidget(swtBot.comboBoxWithLabel(label));
         return comboBox;
     }
 
     public IRemoteBotCombo comboBoxWithLabel(String label, int index)
         throws RemoteException {
         comboBox.setWidget(swtBot.comboBoxWithLabel(label, index));
         return comboBox;
     }
 
     public IRemoteBotCombo comboBox(String text) throws RemoteException {
         comboBox.setWidget(swtBot.comboBox(text));
         return comboBox;
     }
 
     public IRemoteBotCombo comboBox(String text, int index)
         throws RemoteException {
         comboBox.setWidget(swtBot.comboBox(text, index));
         return comboBox;
     }
 
     public IRemoteBotCombo comboBoxWithId(String key, String value)
         throws RemoteException {
         comboBox.setWidget(swtBot.comboBoxWithId(key, value));
         return comboBox;
     }
 
     public IRemoteBotCombo comboBoxWithId(String key, String value, int index)
         throws RemoteException {
         comboBox.setWidget(swtBot.comboBoxWithId(key, value, index));
         return comboBox;
     }
 
     public IRemoteBotCombo comboBoxWithId(String value) throws RemoteException {
         comboBox.setWidget(swtBot.comboBoxWithId(value));
         return comboBox;
     }
 
     public IRemoteBotCombo comboBoxWithId(String value, int index)
         throws RemoteException {
         comboBox.setWidget(swtBot.comboBoxWithId(value, index));
         return comboBox;
     }
 
     public IRemoteBotCombo comboBoxInGroup(String inGroup)
         throws RemoteException {
         comboBox.setWidget(swtBot.comboBoxInGroup(inGroup));
         return comboBox;
     }
 
     public IRemoteBotCombo comboBoxInGroup(String inGroup, int index)
         throws RemoteException {
         comboBox.setWidget(swtBot.comboBoxInGroup(inGroup, index));
         return comboBox;
     }
 
     public IRemoteBotCombo comboBox() throws RemoteException {
         comboBox.setWidget(swtBot.comboBox());
         return comboBox;
     }
 
     public IRemoteBotCombo comboBox(int index) throws RemoteException {
         comboBox.setWidget(swtBot.comboBox(index));
         return comboBox;
     }
 
     public IRemoteBotCombo comboBoxWithLabelInGroup(String label, String inGroup)
         throws RemoteException {
         comboBox.setWidget(swtBot.comboBoxWithLabelInGroup(label, inGroup));
         return comboBox;
     }
 
     public IRemoteBotCombo comboBoxWithLabelInGroup(String label,
         String inGroup, int index) throws RemoteException {
         comboBox.setWidget(swtBot.comboBoxWithLabelInGroup(label, inGroup,
             index));
         return comboBox;
     }
 
     public IRemoteBotCombo comboBoxInGroup(String text, String inGroup)
         throws RemoteException {
         comboBox.setWidget(swtBot.comboBoxInGroup(text, inGroup));
         return comboBox;
     }
 
     public IRemoteBotCombo comboBoxInGroup(String text, String inGroup,
         int index) throws RemoteException {
         comboBox.setWidget(swtBot.comboBoxInGroup(text, inGroup, index));
         return comboBox;
     }
 
     /**********************************************
      * 
      * Widget ccomboBox
      * 
      **********************************************/
     public IRemoteBotCCombo ccomboBox(String text) throws RemoteException {
         ccomboBox.setWidget(swtBot.ccomboBox(text));
         return ccomboBox;
     }
 
     public IRemoteBotCCombo ccomboBox(String text, int index)
         throws RemoteException {
         ccomboBox.setWidget(swtBot.ccomboBox(text, index));
         return ccomboBox;
     }
 
     public IRemoteBotCCombo ccomboBoxWithLabel(String label)
         throws RemoteException {
         ccomboBox.setWidget(swtBot.ccomboBoxWithLabel(label));
         return ccomboBox;
     }
 
     public IRemoteBotCCombo ccomboBoxWithLabel(String label, int index)
         throws RemoteException {
         ccomboBox.setWidget(swtBot.ccomboBoxWithLabel(label, index));
         return ccomboBox;
     }
 
     public IRemoteBotCCombo ccomboBoxWithId(String key, String value)
         throws RemoteException {
         ccomboBox.setWidget(swtBot.ccomboBoxWithId(key, value));
         return ccomboBox;
     }
 
     public IRemoteBotCCombo ccomboBoxWithId(String key, String value, int index)
         throws RemoteException {
         ccomboBox.setWidget(swtBot.ccomboBoxWithId(key, value, index));
         return ccomboBox;
     }
 
     public IRemoteBotCCombo ccomboBoxWithId(String value)
         throws RemoteException {
         ccomboBox.setWidget(swtBot.ccomboBoxWithId(value));
         return ccomboBox;
     }
 
     public IRemoteBotCCombo ccomboBoxWithId(String value, int index)
         throws RemoteException {
         ccomboBox.setWidget(swtBot.ccomboBoxWithId(value, index));
         return ccomboBox;
     }
 
     public IRemoteBotCCombo ccomboBoxInGroup(String inGroup)
         throws RemoteException {
         ccomboBox.setWidget(swtBot.ccomboBoxInGroup(inGroup));
         return ccomboBox;
     }
 
     public IRemoteBotCCombo ccomboBoxInGroup(String inGroup, int index)
         throws RemoteException {
         ccomboBox.setWidget(swtBot.ccomboBoxInGroup(inGroup, index));
         return ccomboBox;
     }
 
     public IRemoteBotCCombo ccomboBox() throws RemoteException {
         ccomboBox.setWidget(swtBot.ccomboBox());
         return ccomboBox;
     }
 
     public IRemoteBotCCombo ccomboBox(int index) throws RemoteException {
         ccomboBox.setWidget(swtBot.ccomboBox(index));
         return ccomboBox;
 
     }
 
     public IRemoteBotCCombo ccomboBoxInGroup(String text, String inGroup)
         throws RemoteException {
         ccomboBox.setWidget(swtBot.ccomboBoxInGroup(text, inGroup));
         return ccomboBox;
     }
 
     public IRemoteBotCCombo ccomboBoxInGroup(String text, String inGroup,
         int index) throws RemoteException {
         ccomboBox.setWidget(swtBot.ccomboBoxInGroup(text, inGroup, index));
         return ccomboBox;
     }
 
     public IRemoteBotCCombo ccomboBoxWithLabelInGroup(String label,
         String inGroup) throws RemoteException {
         ccomboBox.setWidget(swtBot.ccomboBoxWithLabelInGroup(label, inGroup));
         return ccomboBox;
     }
 
     public IRemoteBotCCombo ccomboBoxWithLabelInGroup(String label,
         String inGroup, int index) throws RemoteException {
         ccomboBox.setWidget(swtBot.ccomboBoxWithLabelInGroup(label, inGroup,
             index));
         return ccomboBox;
     }
 
     public IRemoteBotToolbarButton toolbarButton() throws RemoteException {
         return toolbarButton.setWidget(swtBot.toolbarButton());
 
     }
 
     public IRemoteBotToolbarButton toolbarButton(int index)
         throws RemoteException {
         return toolbarButton.setWidget(swtBot.toolbarButton(index));
 
     }
 
     public boolean existsToolbarButton() throws RemoteException {
 
         try {
             swtBot.toolbarButton();
             return true;
         } catch (WidgetNotFoundException e) {
             return false;
         }
     }
 
     public IRemoteBotToolbarButton toolbarButton(String mnemonicText)
         throws RemoteException {
         return toolbarButton.setWidget(swtBot.toolbarButton(mnemonicText));
 
     }
 
     public IRemoteBotToolbarButton toolbarButton(String mnemonicText, int index)
         throws RemoteException {
         return toolbarButton.setWidget(swtBot
             .toolbarButton(mnemonicText, index));
 
     }
 
     public IRemoteBotToolbarButton toolbarButtonWithTooltip(String tooltip)
         throws RemoteException {
         return toolbarButton
             .setWidget(swtBot.toolbarButtonWithTooltip(tooltip));
 
     }
 
     public IRemoteBotToolbarButton toolbarButtonWithTooltip(String tooltip,
         int index) throws RemoteException {
         return toolbarButton.setWidget(swtBot.toolbarButtonWithTooltip(tooltip,
             index));
 
     }
 
     public IRemoteBotToolbarButton toolbarButtonWithId(String key, String value)
         throws RemoteException {
         return toolbarButton.setWidget(swtBot.toolbarButtonWithId(key, value));
 
     }
 
     public IRemoteBotToolbarButton toolbarButtonWithId(String key,
         String value, int index) throws RemoteException {
         return toolbarButton.setWidget(swtBot.toolbarButtonWithId(key, value,
             index));
 
     }
 
     public IRemoteBotToolbarButton toolbarButtonWithId(String value)
         throws RemoteException {
         return toolbarButton.setWidget(swtBot.toolbarButtonWithId(value));
 
     }
 
     public IRemoteBotToolbarButton toolbarButtonWithId(String value, int index)
         throws RemoteException {
         return toolbarButton
             .setWidget(swtBot.toolbarButtonWithId(value, index));
 
     }
 
     public IRemoteBotToolbarButton toolbarButtonInGroup(String inGroup)
         throws RemoteException {
         return toolbarButton.setWidget(swtBot.toolbarButtonInGroup(inGroup));
 
     }
 
     public IRemoteBotToolbarButton toolbarButtonInGroup(String inGroup,
         int index) throws RemoteException {
         return toolbarButton.setWidget(swtBot.toolbarButtonInGroup(inGroup,
             index));
 
     }
 
     public IRemoteBotToolbarButton toolbarButtonInGroup(String mnemonicText,
         String inGroup) throws RemoteException {
         return toolbarButton.setWidget(swtBot.toolbarButtonInGroup(
             mnemonicText, inGroup));
 
     }
 
     public IRemoteBotToolbarButton toolbarButtonInGroup(String mnemonicText,
         String inGroup, int index) throws RemoteException {
         return toolbarButton.setWidget(swtBot.toolbarButtonInGroup(
             mnemonicText, inGroup, index));
 
     }
 
     public IRemoteBotToolbarButton toolbarButtonWithTooltipInGroup(
         String tooltip, String inGroup) throws RemoteException {
         return toolbarButton.setWidget(swtBot.toolbarButtonWithTooltipInGroup(
             tooltip, inGroup));
 
     }
 
     public IRemoteBotToolbarButton toolbarButtonWithTooltipInGroup(
         String tooltip, String inGroup, int index) throws RemoteException {
         return toolbarButton.setWidget(swtBot.toolbarButtonWithTooltipInGroup(
             tooltip, inGroup, index));
 
     }
 
     public IRemoteBotText textWithLabel(String label) throws RemoteException {
         text.setWidget(swtBot.textWithLabel(label));
         return text;
     }
 
     public IRemoteBotText textWithLabel(String label, int index)
         throws RemoteException {
         text.setWidget(swtBot.textWithLabel(label, index));
         return text;
     }
 
     public IRemoteBotText text(String txt) throws RemoteException {
         text.setWidget(swtBot.text(txt));
         return text;
     }
 
     public IRemoteBotText text(String txt, int index) throws RemoteException {
         text.setWidget(swtBot.text(txt, index));
         return text;
 
     }
 
     public IRemoteBotText textWithTooltip(String tooltip)
         throws RemoteException {
         text.setWidget(swtBot.textWithTooltip(tooltip));
         return text;
 
     }
 
     public IRemoteBotText textWithTooltip(String tooltip, int index)
         throws RemoteException {
         text.setWidget(swtBot.textWithTooltip(tooltip, index));
         return text;
 
     }
 
     public IRemoteBotText textWithMessage(String message)
         throws RemoteException {
         text.setWidget(swtBot.textWithMessage(message));
         return text;
 
     }
 
     public IRemoteBotText textWithMessage(String message, int index)
         throws RemoteException {
         text.setWidget(swtBot.textWithMessage(message, index));
         return text;
 
     }
 
     public IRemoteBotText textWithId(String key, String value)
         throws RemoteException {
         text.setWidget(swtBot.textWithId(key, value));
         return text;
 
     }
 
     public IRemoteBotText textWithId(String key, String value, int index)
         throws RemoteException {
         text.setWidget(swtBot.textWithId(key, value, index));
         return text;
 
     }
 
     public IRemoteBotText textWithId(String value) throws RemoteException {
         text.setWidget(swtBot.textWithId(value));
         return text;
 
     }
 
     public IRemoteBotText textWithId(String value, int index)
         throws RemoteException {
         text.setWidget(swtBot.textWithId(value, index));
         return text;
 
     }
 
     public IRemoteBotText textInGroup(String inGroup) throws RemoteException {
         text.setWidget(swtBot.textInGroup(inGroup));
         return text;
 
     }
 
     public IRemoteBotText textInGroup(String inGroup, int index)
         throws RemoteException {
         text.setWidget(swtBot.textInGroup(inGroup, index));
         return text;
 
     }
 
     public IRemoteBotText text() throws RemoteException {
         text.setWidget(swtBot.text());
         return text;
 
     }
 
     public IRemoteBotText text(int index) throws RemoteException {
         text.setWidget(swtBot.text(index));
         return text;
 
     }
 
     public IRemoteBotText textWithLabelInGroup(String label, String inGroup)
         throws RemoteException {
         text.setWidget(swtBot.textWithLabel(label));
         return text;
 
     }
 
     public IRemoteBotText textWithLabelInGroup(String label, String inGroup,
         int index) throws RemoteException {
         text.setWidget(swtBot.textWithLabelInGroup(label, inGroup, index));
         return text;
 
     }
 
     public IRemoteBotText textInGroup(String txt, String inGroup)
         throws RemoteException {
         text.setWidget(swtBot.textInGroup(txt, inGroup));
         return text;
 
     }
 
     public IRemoteBotText textInGroup(String txt, String inGroup, int index)
         throws RemoteException {
         text.setWidget(swtBot.textInGroup(txt, inGroup, index));
         return text;
 
     }
 
     public IRemoteBotText textWithTooltipInGroup(String tooltip, String inGroup)
         throws RemoteException {
         text.setWidget(swtBot.textWithTooltipInGroup(tooltip, inGroup));
         return text;
 
     }
 
     public IRemoteBotText textWithTooltipInGroup(String tooltip,
         String inGroup, int index) throws RemoteException {
         text.setWidget(swtBot.textWithTooltipInGroup(tooltip, inGroup, index));
         return text;
     }
 
     public boolean existsTable() throws RemoteException {
         try {
             swtBot.table();
             return true;
         } catch (WidgetNotFoundException e) {
             return false;
         }
     }
 
     public boolean existsTableInGroup(String groupName) throws RemoteException {
         try {
             swtBot.tableInGroup(groupName);
             return true;
         } catch (WidgetNotFoundException e) {
             return false;
         }
     }
 
     public IRemoteBotTable tableWithLabel(String label) throws RemoteException {
         return table.setWidget(swtBot.tableWithLabel(label));
     }
 
     public IRemoteBotTable tableWithLabel(String label, int index)
         throws RemoteException {
         table.setWidget(swtBot.tableWithLabel(label, index));
         return table;
     }
 
     public IRemoteBotTable tableWithId(String key, String value)
         throws RemoteException {
         table.setWidget(swtBot.tableWithId(key, value));
         return table;
     }
 
     public IRemoteBotTable tableWithId(String key, String value, int index)
         throws RemoteException {
         table.setWidget(swtBot.tableWithId(key, value, index));
         return table;
     }
 
     public IRemoteBotTable tableWithId(String value) throws RemoteException {
         table.setWidget(swtBot.tableWithId(value));
         return table;
     }
 
     public IRemoteBotTable tableWithId(String value, int index)
         throws RemoteException {
         return table.setWidget(swtBot.tableWithId(value, index));
 
     }
 
     public IRemoteBotTable tableInGroup(String inGroup) throws RemoteException {
         return table.setWidget(swtBot.tableInGroup(inGroup));
 
     }
 
     public IRemoteBotTable tableInGroup(String inGroup, int index)
         throws RemoteException {
         return table.setWidget(swtBot.tableInGroup(inGroup, index));
 
     }
 
     public IRemoteBotTable table() throws RemoteException {
         return table.setWidget(swtBot.table());
 
     }
 
     public IRemoteBotTable table(int index) throws RemoteException {
         return table.setWidget(swtBot.table(index));
 
     }
 
     public IRemoteBotTable tableWithLabelInGroup(String label, String inGroup)
         throws RemoteException {
         return table.setWidget(swtBot.tableWithLabelInGroup(label, inGroup));
 
     }
 
     public IRemoteBotMenu menu(String text) throws RemoteException {
         return menu.setWidget(swtBot.menu(text));
 
     }
 
     public IRemoteBotMenu menu(String text, int index) throws RemoteException {
         return menu.setWidget(swtBot.menu(text, index));
     }
 
     public IRemoteBotMenu menuWithId(String value) throws RemoteException {
         return menu.setWidget(swtBot.menuWithId(value));
     }
 
     public IRemoteBotMenu menuWithId(String value, int index)
         throws RemoteException {
         return menu.setWidget(swtBot.menuWithId(value, index));
     }
 
     public IRemoteBotMenu menuWithId(String key, String value)
         throws RemoteException {
         return menu.setWidget(swtBot.menuWithId(key, value));
     }
 
     public IRemoteBotMenu menuWithId(String key, String value, int index)
         throws RemoteException {
         return menu.setWidget(swtBot.menuWithId(key, value, index));
 
     }
 
     public IRemoteBotList listWithLabel(String label) throws RemoteException {
         list.setWidget(swtBot.listWithLabel(label));
         return list;
     }
 
     public IRemoteBotList listWithLabel(String label, int index)
         throws RemoteException {
         list.setWidget(swtBot.listWithLabel(label, index));
         return list;
     }
 
     public IRemoteBotList listWithId(String key, String value)
         throws RemoteException {
         list.setWidget(swtBot.listWithId(key, value));
         return list;
     }
 
     public IRemoteBotList listWithId(String key, String value, int index)
         throws RemoteException {
         list.setWidget(swtBot.listWithId(key, value, index));
         return list;
     }
 
     public IRemoteBotList listWithId(String value) throws RemoteException {
         list.setWidget(swtBot.listWithId(value));
         return list;
     }
 
     public IRemoteBotList listWithId(String value, int index)
         throws RemoteException {
         list.setWidget(swtBot.listWithId(value, index));
         return list;
     }
 
     public IRemoteBotList listInGroup(String inGroup) throws RemoteException {
         list.setWidget(swtBot.listInGroup(inGroup));
         return list;
     }
 
     public IRemoteBotList listInGroup(String inGroup, int index)
         throws RemoteException {
         list.setWidget(swtBot.listInGroup(inGroup, index));
         return list;
     }
 
     public IRemoteBotList list() throws RemoteException {
         list.setWidget(swtBot.list());
         return list;
     }
 
     public IRemoteBotList list(int index) throws RemoteException {
         list.setWidget(swtBot.list(index));
         return list;
     }
 
     public IRemoteBotList listWithLabelInGroup(String label, String inGroup)
         throws RemoteException {
         list.setWidget(swtBot.listWithLabelInGroup(label, inGroup));
         return list;
     }
 
     public IRemoteBotList listWithLabelInGroup(String label, String inGroup,
         int index) throws RemoteException {
         list.setWidget(swtBot.listWithLabelInGroup(label, inGroup, index));
         return list;
     }
 
     public IRemoteBotCheckBox checkBoxWithLabel(String label)
         throws RemoteException {
         checkbox.setWidget(swtBot.checkBoxWithLabel(label));
         return checkbox;
     }
 
     public IRemoteBotCheckBox checkBoxWithLabel(String label, int index)
         throws RemoteException {
         checkbox.setWidget(swtBot.checkBoxWithLabel(label, index));
         return checkbox;
     }
 
     public IRemoteBotCheckBox checkBox(String mnemonicText)
         throws RemoteException {
         checkbox.setWidget(swtBot.checkBox(mnemonicText));
         return checkbox;
     }
 
     public IRemoteBotCheckBox checkBox(String mnemonicText, int index)
         throws RemoteException {
         checkbox.setWidget(swtBot.checkBox(mnemonicText, index));
         return checkbox;
     }
 
     public IRemoteBotCheckBox checkBoxWithTooltip(String tooltip)
         throws RemoteException {
         checkbox.setWidget(swtBot.checkBoxWithTooltip(tooltip));
         return checkbox;
     }
 
     public IRemoteBotCheckBox checkBoxWithTooltip(String tooltip, int index)
         throws RemoteException {
         checkbox.setWidget(swtBot.checkBoxWithTooltip(tooltip, index));
         return checkbox;
     }
 
     public IRemoteBotCheckBox checkBoxWithId(String key, String value)
         throws RemoteException {
         checkbox.setWidget(swtBot.checkBoxWithId(key, value));
         return checkbox;
     }
 
     public IRemoteBotCheckBox checkBoxWithId(String key, String value, int index)
         throws RemoteException {
         checkbox.setWidget(swtBot.checkBoxWithId(key, value, index));
         return checkbox;
     }
 
     public IRemoteBotCheckBox checkBoxWithId(String value)
         throws RemoteException {
         checkbox.setWidget(swtBot.checkBoxWithId(value));
         return checkbox;
     }
 
     public IRemoteBotCheckBox checkBoxWithId(String value, int index)
         throws RemoteException {
         checkbox.setWidget(swtBot.checkBoxWithId(value, index));
         return checkbox;
     }
 
     public IRemoteBotCheckBox checkBoxInGroup(String inGroup)
         throws RemoteException {
         checkbox.setWidget(swtBot.checkBoxInGroup(inGroup));
         return checkbox;
     }
 
     public IRemoteBotCheckBox checkBoxInGroup(String inGroup, int index)
         throws RemoteException {
         checkbox.setWidget(swtBot.checkBoxInGroup(inGroup, index));
         return checkbox;
     }
 
     public IRemoteBotCheckBox checkBox() throws RemoteException {
         checkbox.setWidget(swtBot.checkBox());
         return checkbox;
     }
 
     public IRemoteBotCheckBox checkBox(int index) throws RemoteException {
         checkbox.setWidget(swtBot.checkBox(index));
         return checkbox;
     }
 
     public IRemoteBotCheckBox checkBoxWithLabelInGroup(String label,
         String inGroup) throws RemoteException {
         checkbox.setWidget(swtBot.checkBoxWithLabelInGroup(label, inGroup));
         return checkbox;
     }
 
     public IRemoteBotCheckBox checkBoxWithLabelInGroup(String label,
         String inGroup, int index) throws RemoteException {
         checkbox.setWidget(swtBot.checkBoxWithLabelInGroup(label, inGroup,
             index));
         return checkbox;
     }
 
     public IRemoteBotCheckBox checkBoxInGroup(String mnemonicText,
         String inGroup) throws RemoteException {
         checkbox.setWidget(swtBot.checkBoxInGroup(mnemonicText, inGroup));
         return checkbox;
     }
 
     public IRemoteBotCheckBox checkBoxInGroup(String mnemonicText,
         String inGroup, int index) throws RemoteException {
         checkbox
             .setWidget(swtBot.checkBoxInGroup(mnemonicText, inGroup, index));
         return checkbox;
     }
 
     public IRemoteBotCheckBox checkBoxWithTooltipInGroup(String tooltip,
         String inGroup) throws RemoteException {
         checkbox.setWidget(swtBot.checkBoxWithTooltipInGroup(tooltip, inGroup));
         return checkbox;
     }
 
     public IRemoteBotCheckBox checkBoxWithTooltipInGroup(String tooltip,
         String inGroup, int index) throws RemoteException {
         checkbox.setWidget(swtBot.checkBoxWithTooltipInGroup(tooltip, inGroup,
             index));
         return checkbox;
     }
 
     public IRemoteBotRadio radioWithLabel(String label) throws RemoteException {
         radio.setWidget(swtBot.radioWithLabel(label));
         return radio;
     }
 
     public IRemoteBotRadio radioWithLabel(String label, int index)
         throws RemoteException {
         radio.setWidget(swtBot.radioWithLabel(label));
         return radio;
     }
 
     public IRemoteBotRadio radio(String mnemonicText) throws RemoteException {
         radio.setWidget(swtBot.radio(mnemonicText));
         return radio;
     }
 
     public IRemoteBotRadio radio(String mnemonicText, int index)
         throws RemoteException {
         radio.setWidget(swtBot.radio(mnemonicText, index));
         return radio;
     }
 
     public IRemoteBotRadio radioWithTooltip(String tooltip)
         throws RemoteException {
         radio.setWidget(swtBot.radioWithTooltip(tooltip));
         return radio;
     }
 
     public IRemoteBotRadio radioWithTooltip(String tooltip, int index)
         throws RemoteException {
         radio.setWidget(swtBot.radioWithTooltip(tooltip, index));
         return radio;
     }
 
     public IRemoteBotRadio radioWithId(String key, String value)
         throws RemoteException {
         radio.setWidget(swtBot.radioWithId(key, value));
         return radio;
     }
 
     public IRemoteBotRadio radioWithId(String key, String value, int index)
         throws RemoteException {
         radio.setWidget(swtBot.radioWithId(key, value, index));
         return radio;
     }
 
     public IRemoteBotRadio radioWithId(String value) throws RemoteException {
         radio.setWidget(swtBot.radioWithId(value));
         return radio;
     }
 
     public IRemoteBotRadio radioWithId(String value, int index)
         throws RemoteException {
         radio.setWidget(swtBot.radioWithId(value, index));
         return radio;
     }
 
     public IRemoteBotRadio radioInGroup(String inGroup) throws RemoteException {
         radio.setWidget(swtBot.radioInGroup(inGroup));
         return radio;
     }
 
     public IRemoteBotRadio radioInGroup(String inGroup, int index)
         throws RemoteException {
         radio.setWidget(swtBot.radioInGroup(inGroup, index));
         return radio;
     }
 
     public IRemoteBotRadio radio() throws RemoteException {
         radio.setWidget(swtBot.radio());
         return radio;
     }
 
     public IRemoteBotRadio radio(int index) throws RemoteException {
         radio.setWidget(swtBot.radio(index));
         return radio;
     }
 
     public IRemoteBotRadio radioWithLabelInGroup(String label, String inGroup)
         throws RemoteException {
         radio.setWidget(swtBot.radioWithLabelInGroup(label, inGroup));
         return radio;
     }
 
     public IRemoteBotRadio radioWithLabelInGroup(String label, String inGroup,
         int index) throws RemoteException {
         radio.setWidget(swtBot.radioWithLabelInGroup(label, inGroup, index));
         return radio;
     }
 
     public IRemoteBotRadio radioInGroup(String mnemonicText, String inGroup)
         throws RemoteException {
         radio.setWidget(swtBot.radioInGroup(mnemonicText, inGroup));
         return radio;
     }
 
     public IRemoteBotRadio radioInGroup(String mnemonicText, String inGroup,
         int index) throws RemoteException {
         radio.setWidget(swtBot.radioInGroup(mnemonicText, inGroup, index));
         return radio;
     }
 
     public IRemoteBotRadio radioWithTooltipInGroup(String tooltip,
         String inGroup) throws RemoteException {
         radio.setWidget(swtBot.radioWithTooltipInGroup(tooltip, inGroup));
         return radio;
     }
 
     public IRemoteBotRadio radioWithTooltipInGroup(String tooltip,
         String inGroup, int index) throws RemoteException {
         radio
             .setWidget(swtBot.radioWithTooltipInGroup(tooltip, inGroup, index));
         return radio;
     }
 
     public IRemoteBotToggleButton toggleButtonWithLabel(String label)
         throws RemoteException {
         toggleButton.setWidget(swtBot.toggleButtonWithLabel(label));
         return toggleButton;
     }
 
     public IRemoteBotToggleButton toggleButtonWithLabel(String label, int index)
         throws RemoteException {
         toggleButton.setWidget(swtBot.toggleButtonWithLabel(label, index));
         return toggleButton;
     }
 
     public IRemoteBotToggleButton toggleButton(String mnemonicText)
         throws RemoteException {
         toggleButton.setWidget(swtBot.toggleButton(mnemonicText));
         return toggleButton;
     }
 
     public IRemoteBotToggleButton toggleButton(String mnemonicText, int index)
         throws RemoteException {
         toggleButton.setWidget(swtBot.toggleButton(mnemonicText, index));
         return toggleButton;
     }
 
     public IRemoteBotToggleButton toggleButtonWithTooltip(String tooltip)
         throws RemoteException {
         toggleButton.setWidget(swtBot.toggleButtonWithTooltip(tooltip));
         return toggleButton;
     }
 
     public IRemoteBotToggleButton toggleButtonWithTooltip(String tooltip,
         int index) throws RemoteException {
         toggleButton.setWidget(swtBot.toggleButtonWithTooltip(tooltip, index));
         return toggleButton;
     }
 
     public IRemoteBotToggleButton toggleButtonWithId(String key, String value)
         throws RemoteException {
         toggleButton.setWidget(swtBot.toggleButtonWithId(key, value));
         return toggleButton;
     }
 
     public IRemoteBotToggleButton toggleButtonWithId(String key, String value,
         int index) throws RemoteException {
         toggleButton.setWidget(swtBot.toggleButtonWithId(key, value));
         return toggleButton;
     }
 
     public IRemoteBotToggleButton toggleButtonWithId(String value)
         throws RemoteException {
         toggleButton.setWidget(swtBot.toggleButtonWithId(value));
         return toggleButton;
     }
 
     public IRemoteBotToggleButton toggleButtonWithId(String value, int index)
         throws RemoteException {
         toggleButton.setWidget(swtBot.toggleButtonWithId(value, index));
         return toggleButton;
     }
 
     public IRemoteBotToggleButton toggleButtonInGroup(String inGroup)
         throws RemoteException {
         toggleButton.setWidget(swtBot.toggleButtonInGroup(inGroup));
         return toggleButton;
     }
 
     public IRemoteBotToggleButton toggleButtonInGroup(String inGroup, int index)
         throws RemoteException {
         toggleButton.setWidget(swtBot.toggleButtonInGroup(inGroup, index));
         return toggleButton;
     }
 
     public IRemoteBotToggleButton toggleButton() throws RemoteException {
         toggleButton.setWidget(swtBot.toggleButton());
         return toggleButton;
     }
 
     public IRemoteBotToggleButton toggleButton(int index)
         throws RemoteException {
         toggleButton.setWidget(swtBot.toggleButton(index));
         return toggleButton;
     }
 
     public IRemoteBotToggleButton toggleButtonWithLabelInGroup(String label,
         String inGroup) throws RemoteException {
         toggleButton.setWidget(swtBot.toggleButtonWithLabelInGroup(label,
             inGroup));
         return toggleButton;
     }
 
     public IRemoteBotToggleButton toggleButtonWithLabelInGroup(String label,
         String inGroup, int index) throws RemoteException {
         toggleButton.setWidget(swtBot.toggleButtonWithLabelInGroup(label,
             inGroup, index));
         return toggleButton;
     }
 
     public IRemoteBotToggleButton toggleButtonInGroup(String mnemonicText,
         String inGroup) throws RemoteException {
         toggleButton.setWidget(swtBot
             .toggleButtonInGroup(mnemonicText, inGroup));
         return toggleButton;
     }
 
     public IRemoteBotToggleButton toggleButtonInGroup(String mnemonicText,
         String inGroup, int index) throws RemoteException {
         toggleButton.setWidget(swtBot.toggleButtonInGroup(mnemonicText,
             inGroup, index));
         return toggleButton;
     }
 
     public IRemoteBotToggleButton toggleButtonWithTooltipInGroup(
         String tooltip, String inGroup) throws RemoteException {
         toggleButton.setWidget(swtBot.toggleButtonWithTooltipInGroup(tooltip,
             inGroup));
         return toggleButton;
     }
 
     public IRemoteBotToggleButton toggleButtonWithTooltipInGroup(
         String tooltip, String inGroup, int index) throws RemoteException {
         toggleButton.setWidget(swtBot.toggleButtonWithTooltipInGroup(tooltip,
             inGroup, index));
         return toggleButton;
     }
 
     public IRemoteBotCTabItem cTabItem() throws RemoteException {
         cTabItem.setWidget(swtBot.cTabItem());
         return cTabItem;
 
     }
 
     public void waitUntil(ICondition condition) throws RemoteException {
         swtBot
            .waitUntil(condition, SarosSWTBotPreferences.SAROS_DEFAULT_TIMEOUT);
     }
 
     public void waitLongUntil(ICondition condition) throws RemoteException {
         swtBot.waitUntil(condition, SarosSWTBotPreferences.SAROS_LONG_TIMEOUT);
     }
 
     public void waitShortUntil(ICondition condition) throws RemoteException {
         swtBot.waitUntil(condition, SarosSWTBotPreferences.SAROS_SHORT_TIMEOUT);
     }
 
     public void logMessage(String message) throws RemoteException {
         log.info(message);
     }
 
     public void sleep(long millis) throws RemoteException {
         swtBot.sleep(millis);
     }
 
     public void captureScreenshot(String fileName) throws RemoteException {
         log.trace("creating screenshot -> file: "
             + new File(SCREENSHOT_DIRECTORY, fileName).getAbsolutePath());
         swtBot.captureScreenshot(new File(SCREENSHOT_DIRECTORY, fileName)
             .getAbsolutePath());
     }
 
     /**********************************************
      * 
      * Widget cLabel
      * 
      **********************************************/
 
     public boolean existsCLabel() throws RemoteException {
         try {
             swtBot.clabel();
             return true;
         } catch (WidgetNotFoundException e) {
             return false;
         }
     }
 
     public IRemoteBotCLabel clabel() throws RemoteException {
         clabel.setWidget(swtBot.clabel());
         return clabel;
     }
 
     public IRemoteBotCLabel clabel(String text) throws RemoteException {
         clabel.setWidget(swtBot.clabel(text));
         return clabel;
     }
 
     // -------------
 
 }
