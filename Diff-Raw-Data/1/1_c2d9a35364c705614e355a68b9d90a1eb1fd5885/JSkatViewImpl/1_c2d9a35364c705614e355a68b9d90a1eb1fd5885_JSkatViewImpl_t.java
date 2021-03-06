 /**
  * JSkat - A skat program written in Java
  * Copyright by Jan Schäfer and Markus J. Luzius
  *
  * Version: 0.7.0-SNAPSHOT
  * Build date: 2011-04-13 21:42:39
  *
  * This program is free software: you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation, either version 3 of the License, or
  * any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program.  If not, see <http://www.gnu.org/licenses/>.
  */
 
 
 package de.jskat.gui;
 
 import java.awt.BorderLayout;
 import java.awt.Component;
 import java.awt.Dimension;
 import java.awt.event.ActionEvent;
 import java.text.MessageFormat;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 
 import javax.swing.ActionMap;
 import javax.swing.ImageIcon;
 import javax.swing.JFrame;
 import javax.swing.JMenu;
 import javax.swing.JMenuBar;
 import javax.swing.JMenuItem;
 import javax.swing.JOptionPane;
 import javax.swing.JPanel;
 import javax.swing.JSeparator;
 import javax.swing.JTabbedPane;
 import javax.swing.event.ChangeEvent;
 import javax.swing.event.ChangeListener;
 
 import net.miginfocom.swing.MigLayout;
 
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
 
 import de.jskat.control.JSkatMaster;
 import de.jskat.control.SkatTable;
 import de.jskat.control.iss.ChatMessageType;
 import de.jskat.data.GameAnnouncement;
 import de.jskat.data.JSkatOptions;
 import de.jskat.data.SkatGameData;
 import de.jskat.data.SkatGameData.GameState;
 import de.jskat.data.SkatSeriesData.SeriesState;
 import de.jskat.data.Trick;
 import de.jskat.data.iss.ISSChatMessage;
 import de.jskat.data.iss.ISSGameStartInformation;
 import de.jskat.data.iss.ISSMoveInformation;
 import de.jskat.data.iss.ISSTablePanelStatus;
 import de.jskat.data.iss.MovePlayer;
 import de.jskat.gui.action.JSkatAction;
 import de.jskat.gui.action.human.ContinueSkatSeriesAction;
 import de.jskat.gui.action.human.DiscardAction;
 import de.jskat.gui.action.human.GameAnnounceAction;
 import de.jskat.gui.action.human.HoldBidAction;
 import de.jskat.gui.action.human.MakeBidAction;
 import de.jskat.gui.action.human.PassBidAction;
 import de.jskat.gui.action.human.PickUpSkatAction;
 import de.jskat.gui.action.human.PlayCardAction;
 import de.jskat.gui.action.human.PlayHandGameAction;
 import de.jskat.gui.action.human.PutCardIntoSkatAction;
 import de.jskat.gui.action.human.TakeCardFromSkatAction;
 import de.jskat.gui.action.iss.ChangeTableSeatsAction;
 import de.jskat.gui.action.iss.ConnectAction;
 import de.jskat.gui.action.iss.CreateIssTableAction;
 import de.jskat.gui.action.iss.DisconnectAction;
 import de.jskat.gui.action.iss.InvitePlayerAction;
 import de.jskat.gui.action.iss.JoinIssTableAction;
 import de.jskat.gui.action.iss.LeaveIssTableAction;
 import de.jskat.gui.action.iss.ObserveIssTableAction;
 import de.jskat.gui.action.iss.OpenIssHomepageAction;
 import de.jskat.gui.action.iss.ReadyAction;
 import de.jskat.gui.action.iss.RegisterOnIssAction;
 import de.jskat.gui.action.iss.SendChatMessageAction;
 import de.jskat.gui.action.iss.ShowLoginPanelAction;
 import de.jskat.gui.action.iss.TalkEnableAction;
 import de.jskat.gui.action.main.AboutAction;
 import de.jskat.gui.action.main.ChangeActiveTableAction;
 import de.jskat.gui.action.main.CreateTableAction;
 import de.jskat.gui.action.main.ExitAction;
 import de.jskat.gui.action.main.HelpAction;
 import de.jskat.gui.action.main.LicenseAction;
 import de.jskat.gui.action.main.LoadNeuralNetworksAction;
 import de.jskat.gui.action.main.LoadSeriesAction;
 import de.jskat.gui.action.main.PauseSkatSeriesAction;
 import de.jskat.gui.action.main.PreferencesAction;
 import de.jskat.gui.action.main.SaveNeuralNetworksAction;
 import de.jskat.gui.action.main.SaveSeriesAction;
 import de.jskat.gui.action.main.SaveSeriesAsAction;
 import de.jskat.gui.action.main.StartSkatSeriesAction;
 import de.jskat.gui.action.main.TrainNeuralNetworksAction;
 import de.jskat.gui.help.JSkatHelpDialog;
 import de.jskat.gui.img.JSkatGraphicRepository;
 import de.jskat.gui.iss.ISSLobbyPanel;
 import de.jskat.gui.iss.ISSTablePanel;
 import de.jskat.gui.iss.LoginPanel;
 import de.jskat.gui.iss.PlayerInvitationPanel;
 import de.jskat.gui.nn.NeuralNetworkTrainingOverview;
 import de.jskat.gui.table.SkatSeriesStartDialog;
 import de.jskat.gui.table.SkatTablePanel;
 import de.jskat.util.Card;
 import de.jskat.util.CardList;
 import de.jskat.util.GameType;
 import de.jskat.util.JSkatResourceBundle;
 import de.jskat.util.Player;
 import de.jskat.util.SkatConstants;
 
 /**
  * Implementation of JSkatView interface
  */
 public class JSkatViewImpl implements IJSkatView {
 
 	static Log log = LogFactory.getLog(JSkatViewImpl.class);
 
 	private JFrame mainFrame;
 	private SkatSeriesStartDialog skatSeriesStartDialog;
 	private JSkatPreferencesDialog preferencesDialog;
 	private NeuralNetworkTrainingOverview trainingOverview;
 	private JTabbedPane tabs;
 	private Map<String, SkatTablePanel> tables;
 	private JSkatGraphicRepository bitmaps;
 	private JSkatResourceBundle strings;
 	private JSkatMaster jskat;
 	static ActionMap actions;
 	private ISSLobbyPanel issLobby;
 
 	/**
 	 * Constructor
 	 * 
 	 * @param jskat
 	 *            JSkatMaster
 	 */
 	public JSkatViewImpl() {
 
 		bitmaps = JSkatGraphicRepository.instance();
 		strings = JSkatResourceBundle.instance();
 		log.debug("I18n strings loaded: " + strings.getLocale()); //$NON-NLS-1$
 		tables = new HashMap<String, SkatTablePanel>();
 		jskat = JSkatMaster.instance();
 		initActionMap();
 		initGUI();
 
 		skatSeriesStartDialog = new SkatSeriesStartDialog(jskat, mainFrame);
 		// FIXME (jansch 08.12.2010) make this part of the main frame
 		preferencesDialog = new JSkatPreferencesDialog(mainFrame);
 		trainingOverview = new NeuralNetworkTrainingOverview();
 
 		addTabPanel(new WelcomePanel(strings.getString("welcome"), actions),
 				strings.getString("welcome"));
 
 		mainFrame.setVisible(true);
 	}
 
 	private void initActionMap() {
 
 		actions = new ActionMap();
 
 		// common actions
 		actions.put(JSkatAction.LOAD_SERIES, new LoadSeriesAction());
 		actions.put(JSkatAction.SAVE_SERIES, new SaveSeriesAction());
 		actions.put(JSkatAction.SAVE_SERIES_AS, new SaveSeriesAsAction());
 		actions.put(JSkatAction.HELP, new HelpAction());
 		actions.put(JSkatAction.LICENSE, new LicenseAction());
 		actions.put(JSkatAction.EXIT_JSKAT, new ExitAction());
 		actions.put(JSkatAction.PREFERENCES, new PreferencesAction());
 		actions.put(JSkatAction.ABOUT_JSKAT, new AboutAction());
 		actions.put(JSkatAction.CHANGE_ACTIVE_TABLE,
 				new ChangeActiveTableAction());
 		// skat table actions
 		actions.put(JSkatAction.CREATE_LOCAL_TABLE, new CreateTableAction());
 		actions.put(JSkatAction.START_LOCAL_SERIES, new StartSkatSeriesAction());
 		actions.put(JSkatAction.CONTINUE_LOCAL_SERIES,
 				new ContinueSkatSeriesAction());
 		actions.put(JSkatAction.PAUSE_LOCAL_SERIES, new PauseSkatSeriesAction());
 		// ISS actions
 		actions.put(JSkatAction.REGISTER_ON_ISS, new RegisterOnIssAction());
 		actions.put(JSkatAction.OPEN_ISS_HOMEPAGE, new OpenIssHomepageAction());
 		actions.put(JSkatAction.SHOW_ISS_LOGIN, new ShowLoginPanelAction());
 		actions.put(JSkatAction.CONNECT_TO_ISS, new ConnectAction());
 		actions.put(JSkatAction.DISCONNECT_FROM_ISS, new DisconnectAction());
 		actions.put(JSkatAction.SEND_CHAT_MESSAGE, new SendChatMessageAction());
 		actions.put(JSkatAction.CREATE_ISS_TABLE, new CreateIssTableAction());
 		actions.put(JSkatAction.JOIN_ISS_TABLE, new JoinIssTableAction());
 		actions.put(JSkatAction.LEAVE_ISS_TABLE, new LeaveIssTableAction());
 		actions.put(JSkatAction.OBSERVE_ISS_TABLE, new ObserveIssTableAction());
 		actions.put(JSkatAction.READY_TO_PLAY, new ReadyAction());
 		actions.put(JSkatAction.TALK_ENABLED, new TalkEnableAction());
 		actions.put(JSkatAction.CHANGE_TABLE_SEATS,
 				new ChangeTableSeatsAction());
 		actions.put(JSkatAction.INVITE_ISS_PLAYER, new InvitePlayerAction());
 		// Neural network actions
 		actions.put(JSkatAction.TRAIN_NEURAL_NETWORKS,
 				new TrainNeuralNetworksAction());
 		actions.put(JSkatAction.LOAD_NEURAL_NETWORKS,
 				new LoadNeuralNetworksAction());
 		actions.put(JSkatAction.SAVE_NEURAL_NETWORKS,
 				new SaveNeuralNetworksAction());
 		// Human player actions
 		actions.put(JSkatAction.MAKE_BID, new MakeBidAction());
 		actions.put(JSkatAction.HOLD_BID, new HoldBidAction());
 		actions.put(JSkatAction.PASS_BID, new PassBidAction());
 		actions.put(JSkatAction.PICK_UP_SKAT, new PickUpSkatAction());
 		actions.put(JSkatAction.PLAY_HAND_GAME, new PlayHandGameAction());
 		actions.put(JSkatAction.ANNOUNCE_GAME, new GameAnnounceAction());
 		actions.put(JSkatAction.PUT_CARD_INTO_SKAT, new PutCardIntoSkatAction());
 		actions.put(JSkatAction.TAKE_CARD_FROM_SKAT,
 				new TakeCardFromSkatAction());
 		actions.put(JSkatAction.DISCARD_CARDS, new DiscardAction());
 		actions.put(JSkatAction.PLAY_CARD, new PlayCardAction());
 
 		// disable some actions
 		actions.get(JSkatAction.LOAD_SERIES).setEnabled(false);
 		actions.get(JSkatAction.SAVE_SERIES).setEnabled(false);
 		actions.get(JSkatAction.SAVE_SERIES_AS).setEnabled(false);
 		actions.get(JSkatAction.START_LOCAL_SERIES).setEnabled(false);
 		actions.get(JSkatAction.PAUSE_LOCAL_SERIES).setEnabled(false);
 		actions.get(JSkatAction.CONTINUE_LOCAL_SERIES).setEnabled(false);
 		actions.get(JSkatAction.CREATE_ISS_TABLE).setEnabled(false);
 		actions.get(JSkatAction.INVITE_ISS_PLAYER).setEnabled(false);
 	}
 
 	private void initGUI() {
 
 		mainFrame = new JFrame("JSkat"); //$NON-NLS-1$
 
 		mainFrame.setJMenuBar(getMenuBar());
 
 		JPanel mainPanel = new JPanel();
 		mainPanel.setLayout(new BorderLayout());
 
 		// symbol button panel
 		JPanel buttonPanel = new JPanel();
 		buttonPanel.setLayout(new MigLayout());
 		buttonPanel.add(new ToolbarButton(actions
 				.get(JSkatAction.CREATE_LOCAL_TABLE)));
 		buttonPanel.add(new ToolbarButton(actions
 				.get(JSkatAction.START_LOCAL_SERIES)));
 		buttonPanel.add(new ToolbarButton(actions
 				.get(JSkatAction.SHOW_ISS_LOGIN)));
 		buttonPanel.add(new ToolbarButton(actions.get(JSkatAction.HELP)));
 		mainPanel.add(buttonPanel, BorderLayout.NORTH);
 
 		// main area
 		tabs = new JTabbedPane();
 		tabs.setAutoscrolls(true);
 		tabs.addChangeListener(new ChangeListener() {
 			/**
 			 * @see ChangeListener#stateChanged(ChangeEvent)
 			 */
 			@Override
 			public void stateChanged(ChangeEvent e) {
 
 				if (e.getSource() instanceof JTabbedPane) {
 
 					JTabbedPane changedTabs = (JTabbedPane) e.getSource();
 					Component tab = changedTabs.getSelectedComponent();
 
 					if (tab instanceof AbstractTabPanel) {
 
 						AbstractTabPanel panel = (AbstractTabPanel) tab;
 						String tableName = panel.getName();
 						log.debug("showing table panel of table " + tableName); //$NON-NLS-1$
 						panel.setFocus();
 
 						JSkatViewImpl.actions.get(
 								JSkatAction.CHANGE_ACTIVE_TABLE)
 								.actionPerformed(
 										new ActionEvent(e.getSource(), 1,
 												tableName));
 					}
 				}
 			}
 		});
 
 		mainPanel.add(tabs, BorderLayout.CENTER);
 
 		mainFrame.setContentPane(mainPanel);
 		mainFrame.pack();
 
 		mainFrame.setIconImage(bitmaps.getIconImage(
 				JSkatGraphicRepository.Icon.JSKAT,
 				JSkatGraphicRepository.IconSize.BIG));
 		mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		mainFrame.setMinimumSize(new Dimension(800, 600));
 		mainFrame.setPreferredSize(new Dimension(1000, 700));
 		mainFrame.setExtendedState(mainFrame.getExtendedState()
 				| JFrame.MAXIMIZED_BOTH);
 	}
 
 	private JMenuBar getMenuBar() {
 
 		JMenuBar menu = new JMenuBar();
 
 		JMenu fileMenu = new JMenu(strings.getString("file")); //$NON-NLS-1$
 		fileMenu.add(new JMenuItem(actions.get(JSkatAction.LOAD_SERIES)));
 		fileMenu.add(new JMenuItem(actions.get(JSkatAction.SAVE_SERIES)));
 		fileMenu.add(new JMenuItem(actions.get(JSkatAction.SAVE_SERIES_AS)));
 		fileMenu.add(new JSeparator());
 		fileMenu.add(new JMenuItem(actions.get(JSkatAction.EXIT_JSKAT)));
 		menu.add(fileMenu);
 
 		JMenu tableMenu = new JMenu(strings.getString("skat_table")); //$NON-NLS-1$
 		tableMenu
 				.add(new JMenuItem(actions.get(JSkatAction.CREATE_LOCAL_TABLE)));
 		tableMenu.add(new JSeparator());
 		tableMenu
 				.add(new JMenuItem(actions.get(JSkatAction.START_LOCAL_SERIES)));
 		tableMenu
 				.add(new JMenuItem(actions.get(JSkatAction.PAUSE_LOCAL_SERIES)));
 		tableMenu.add(new JMenuItem(actions
 				.get(JSkatAction.CONTINUE_LOCAL_SERIES)));
 		menu.add(tableMenu);
 
 		JMenu neuralNetworkMenu = new JMenu(
 				strings.getString("neural_networks")); //$NON-NLS-1$
 		neuralNetworkMenu.add(new JMenuItem(actions
 				.get(JSkatAction.LOAD_NEURAL_NETWORKS)));
 		neuralNetworkMenu.add(new JMenuItem(actions
 				.get(JSkatAction.SAVE_NEURAL_NETWORKS)));
 		neuralNetworkMenu.add(new JSeparator());
 		neuralNetworkMenu.add(new JMenuItem(actions
 				.get(JSkatAction.TRAIN_NEURAL_NETWORKS)));
 		menu.add(neuralNetworkMenu);
 
 		JMenu issMenu = new JMenu(strings.getString("iss")); //$NON-NLS-1$
 		issMenu.add(new JMenuItem(actions.get(JSkatAction.SHOW_ISS_LOGIN)));
 		issMenu.add(new JSeparator());
 		issMenu.add(new JMenuItem(actions.get(JSkatAction.CREATE_ISS_TABLE)));
 		issMenu.add(new JMenuItem(actions.get(JSkatAction.INVITE_ISS_PLAYER)));
 		menu.add(issMenu);
 
 		JMenu extraMenu = new JMenu(strings.getString("extras")); //$NON-NLS-1$
 		extraMenu.add(new JMenuItem(actions.get(JSkatAction.PREFERENCES)));
 		menu.add(extraMenu);
 
 		JMenu helpMenu = new JMenu(strings.getString("help")); //$NON-NLS-1$
 		helpMenu.add(new JMenuItem(actions.get(JSkatAction.HELP)));
 		helpMenu.add(new JSeparator());
 		helpMenu.add(new JMenuItem(actions.get(JSkatAction.LICENSE)));
 		helpMenu.add(new JMenuItem(actions.get(JSkatAction.ABOUT_JSKAT)));
 		menu.add(helpMenu);
 
 		return menu;
 	}
 
 	/**
 	 * @see IJSkatView#showTable(SkatTable)
 	 */
 	@Override
 	public void showTable(SkatTable table) {
 		// TODO implement it
 	}
 
 	/**
 	 * @see IJSkatView#startSeries(String)
 	 */
 	@Override
 	public void startSeries(String tableName) {
 
 		tables.get(tableName).clearSkatList();
 	}
 
 	/**
 	 * @see IJSkatView#showSeriesResults()
 	 */
 	@Override
 	public void showSeriesResults() {
 		// TODO implement it
 	}
 
 	/**
 	 * @see IJSkatView#startGame(String)
 	 */
 	@Override
 	public void startGame(String tableName) {
 
 		tables.get(tableName).startGame();
 	}
 
 	/**
 	 * @see IJSkatView#startBidding()
 	 */
 	@Override
 	public void startBidding() {
 		// TODO implement it
 	}
 
 	/**
 	 * @see IJSkatView#startDiscarding()
 	 */
 	@Override
 	public void startDiscarding() {
 		// TODO implement it
 	}
 
 	/**
 	 * @see IJSkatView#startPlaying()
 	 */
 	@Override
 	public void startPlaying() {
 		// TODO implement it
 	}
 
 	/**
 	 * @see IJSkatView#showGameResults()
 	 */
 	@Override
 	public void showGameResults() {
 		// TODO implement it
 	}
 
 	/**
 	 * @see IJSkatView#createISSTable(String, String)
 	 */
 	@Override
 	public void createISSTable(String tableName, String loginName) {
 
 		ISSTablePanel newTable = new ISSTablePanel(tableName, actions,
 				loginName);
 		addTabPanel(newTable, "ISS table: " + tableName);
 		tables.put(tableName, newTable);
 	}
 
 	/**
 	 * @see IJSkatView#createSkatTablePanel(String)
 	 */
 	@Override
 	public void createSkatTablePanel(String name) {
 
 		SkatTablePanel newPanel = new SkatTablePanel(name, actions);
 		addTabPanel(newPanel, name);
 		tables.put(name, newPanel);
 
 		actions.get(JSkatAction.START_LOCAL_SERIES).setEnabled(true);
 	}
 
 	/**
 	 * @see IJSkatView#showAboutMessage()
 	 */
 	@Override
 	public void showAboutMessage() {
 		// TODO save text for message centrally
 		JOptionPane
 				.showMessageDialog(
 						mainFrame,
 						"JSkat Version 0.7\n\n"
 								+ "Authors: Jan Schäfer, Markus J. Luzius\n\n"
 								+ "Icons: Gnome Desktop Icons, Tango project, Elementary icons,\nSilvestre Herrera and Alex Roberts\n\n"
 								+ "This program comes with ABSOLUTELY NO WARRANTY; for details see licence dialog\n"
 								+ "This is free software, and you are welcome to redistribute it\n"
 								+ "under certain conditions; see licence dialog for details.",
 						strings.getString("about"), JOptionPane.INFORMATION_MESSAGE, //$NON-NLS-1$
 						new ImageIcon(bitmaps.getJSkatLogoImage()));
 	}
 
 	/**
 	 * @see IJSkatView#showMessage(int, String, String)
 	 */
 	@Override
 	public void showMessage(int messageType, String title, String message) {
 
 		JOptionPane.showMessageDialog(mainFrame, message, title, messageType);
 	}
 
 	/**
 	 * @see IJSkatView#showExitDialog()
 	 */
 	@Override
 	public int showExitDialog() {
 
 		return JOptionPane
 				.showOptionDialog(
 						mainFrame,
 						strings.getString("exit_dialog_message"), strings //$NON-NLS-1$
 								.getString("exit_dialog_title"), JOptionPane.YES_NO_OPTION, //$NON-NLS-1$
 						JOptionPane.QUESTION_MESSAGE, null, null, null);
 	}
 
 	/**
 	 * @see IJSkatView#addCard(String, Player, Card)
 	 */
 	@Override
 	public void addCard(String tableName, Player player, Card card) {
 
 		tables.get(tableName).addCard(player, card);
 	}
 
 	/**
 	 * @see IJSkatView#clearHand(String, Player)
 	 */
 	@Override
 	public void clearHand(String tableName, Player player) {
 
 		tables.get(tableName).clearHand(player);
 	}
 
 	/**
 	 * @see IJSkatView#removeCard(String, Player, Card)
 	 */
 	@Override
 	public void removeCard(String tableName, Player player, Card card) {
 
 		tables.get(tableName).removeCard(player, card);
 	}
 
 	/**
 	 * @see IJSkatView#setPositions(String, Player, Player, Player)
 	 */
 	@Override
 	public void setPositions(String tableName, Player leftPosition,
 			Player rightPosition, Player playerPosition) {
 
 		tables.get(tableName).setPositions(leftPosition, rightPosition,
 				playerPosition);
 	}
 
 	/**
 	 * @see IJSkatView#setTrickCard(String, Player, Card)
 	 */
 	@Override
 	public void setTrickCard(String tableName, Player position, Card card) {
 
 		tables.get(tableName).setTrickCard(position, card);
 	}
 
 	/**
 	 * @see IJSkatView#clearTrickCards(String)
 	 */
 	@Override
 	public void clearTrickCards(String tableName) {
 
 		tables.get(tableName).clearTrickCards();
 	}
 
 	/**
 	 * @see IJSkatView#setGameAnnouncement(String, Player, GameAnnouncement)
 	 */
 	@Override
 	public void setGameAnnouncement(String tableName, Player declarer,
 			GameAnnouncement ann) {
 
 		tables.get(tableName).setGameAnnouncement(declarer, ann);
 	}
 
 	/**
 	 * @see IJSkatView#setGameState(String, GameState)
 	 */
 	@Override
 	public void setGameState(String tableName, GameState state) {
 
 		setActions(state);
 		tables.get(tableName).setGameState(state);
 	}
 
 	void setActions(GameState state) {
 
 		switch (state) {
 		case GAME_START:
 			actions.get(JSkatAction.CONTINUE_LOCAL_SERIES).setEnabled(false);
 			break;
 		case GAME_OVER:
 			actions.get(JSkatAction.CONTINUE_LOCAL_SERIES).setEnabled(true);
 			break;
 		}
 	}
 
 	/**
 	 * @see IJSkatView#addGameResult(String, SkatGameData)
 	 */
 	@Override
 	public void addGameResult(String tableName, SkatGameData data) {
 
 		tables.get(tableName).addGameResult(data);
 	}
 
 	/**
 	 * @see IJSkatView#showHelpDialog()
 	 */
 	@Override
 	public void showHelpDialog() {
 
 		String languageCode = "en";
 		switch (JSkatOptions.instance().getLanguage()) {
 		case GERMAN:
 			languageCode = "de";
 			break;
 		case ENGLISH:
 			languageCode = "en";
 			break;
 		}
 
 		new JSkatHelpDialog(
 				mainFrame,
 				strings.getString("help"), "de/jskat/gui/help/" + languageCode + "/contents.html") //$NON-NLS-1$ 
 				.setVisible(true);
 	}
 
 	/**
 	 * @see IJSkatView#showLicenseDialog()
 	 */
 	@Override
 	public void showLicenseDialog() {
 
 		new JSkatHelpDialog(mainFrame,
 				strings.getString("license"), "de/jskat/gui/help/gpl3.html").setVisible(true); //$NON-NLS-1$ //$NON-NLS-2$
 	}
 
 	/**
 	 * @see IJSkatView#clearTable(String)
 	 */
 	@Override
 	public void clearTable(String tableName) {
 
 		tables.get(tableName).clearTable();
 	}
 
 	/**
 	 * @see IJSkatView#setBidValueToMake(String, int)
 	 */
 	@Override
 	public void setBidValueToMake(String tableName, int bidValue) {
 
 		tables.get(tableName).setBidValueToMake(bidValue);
 	}
 
 	/**
 	 * @see IJSkatView#setBidValueToHold(String, int)
 	 */
 	@Override
 	public void setBidValueToHold(String tableName, int bidValue) {
 
 		tables.get(tableName).setBidValueToHold(bidValue);
 	}
 
 	/**
 	 * @see IJSkatView#setBid(String, Player, int, boolean)
 	 */
 	@Override
 	public void setBid(String tableName, Player player, int bidValue,
 			boolean madeBid) {
 
 		tables.get(tableName).setBid(player, bidValue, madeBid);
 	}
 
 	/**
 	 * @see IJSkatView#setPass(String, Player)
 	 */
 	@Override
 	public void setPass(String tableName, Player player) {
 
 		tables.get(tableName).setPass(player);
 	}
 
 	/**
 	 * @see IJSkatView#setTrickForeHand(String, Player)
 	 */
 	@Override
 	public void setTrickForeHand(String tableName, Player trickForeHand) {
 
 		tables.get(tableName).setTrickForeHand(trickForeHand);
 	}
 
 	/**
 	 * @see IJSkatView#putCardIntoSkat(String, Card)
 	 */
 	@Override
 	public void putCardIntoSkat(String tableName, Card card) {
 
 		tables.get(tableName).putCardIntoSkat(card);
 	}
 
 	/**
 	 * @see IJSkatView#takeCardFromSkat(String, Card)
 	 */
 	@Override
 	public void takeCardFromSkat(String tableName, Card card) {
 
 		tables.get(tableName).takeCardFromSkat(card);
 	}
 
 	/**
 	 * @see IJSkatView#showStartSkatSeriesDialog()
 	 */
 	@Override
 	public void showStartSkatSeriesDialog() {
 
 		skatSeriesStartDialog.setVisible(true);
 	}
 
 	/**
 	 * @see IJSkatView#showISSLogin()
 	 */
 	@Override
 	public void showISSLogin() {
 
 		LoginPanel loginPanel = new LoginPanel("ISS login", actions);
 		addTabPanel(loginPanel, "ISS login");
 	}
 
 	/**
 	 * @see IJSkatView#updateISSLobbyPlayerList(String, String, long, double)
 	 */
 	@Override
 	public void updateISSLobbyPlayerList(String playerName, String language,
 			long gamesPlayed, double strength) {
 
 		issLobby.updatePlayer(playerName, language, gamesPlayed, strength);
 	}
 
 	/**
 	 * @see IJSkatView#removeFromISSLobbyPlayerList(String)
 	 */
 	@Override
 	public void removeFromISSLobbyPlayerList(String playerName) {
 
 		issLobby.removePlayer(playerName);
 	}
 
 	/**
 	 * @see IJSkatView#showISSLobby()
 	 */
 	@Override
 	public void showISSLobby() {
 
 		issLobby = new ISSLobbyPanel("ISS lobby", actions);
 		addTabPanel(issLobby, "ISS lobby");
 	}
 
 	/**
 	 * @see IJSkatView#updateISSLobbyTableList(String, int, long, String,
 	 *      String, String)
 	 */
 	@Override
 	public void updateISSLobbyTableList(String tableName, int maxPlayers,
 			long gamesPlayed, String player1, String player2, String player3) {
 
 		issLobby.updateTable(tableName, maxPlayers, gamesPlayed, player1,
 				player2, player3);
 	}
 
 	/**
 	 * @see IJSkatView#removeFromISSLobbyTableList(String)
 	 */
 	@Override
 	public void removeFromISSLobbyTableList(String tableName) {
 
 		issLobby.removeTable(tableName);
 	}
 
 	/**
 	 * @see IJSkatView#appendISSChatMessage(ChatMessageType, ISSChatMessage)
 	 */
 	@Override
 	public void appendISSChatMessage(ChatMessageType messageType,
 			ISSChatMessage message) {
 
 		log.debug("appendISSChatMessage"); //$NON-NLS-1$
 
 		issLobby.appendChatMessage(message);
 
 		for (SkatTablePanel table : tables.values()) {
 			if (table instanceof ISSTablePanel) {
 				ISSTablePanel issTable = (ISSTablePanel) table;
 				String chatname = message.getChatName();
 				if ("Lobby".equals(chatname)
 						|| issTable.getName().equals(chatname)) {
 					issTable.appendChatMessage(message);
 				}
 			}
 		}
 	}
 
 	/**
 	 * @see de.jskat.gui.IJSkatView#updateISSTable(String, ISSTablePanelStatus)
 	 */
 	@Override
 	public void updateISSTable(String tableName, ISSTablePanelStatus tableStatus) {
 
 		// FIXME (jan 08.11.2010) seems very complicated
 		SkatTablePanel panel = tables.get(tableName);
 
 		if (panel == null) {
 
 			createISSTable(tableName, tableStatus.getLoginName());
 			panel = tables.get(tableName);
 		}
 
 		if (panel instanceof ISSTablePanel) {
 
 			((ISSTablePanel) panel).setTableStatus(tableStatus);
 		}
 	}
 
 	/**
 	 * @see IJSkatView#updateISSTable(String, String, ISSGameStartInformation)
 	 */
 	@Override
 	public void updateISSTable(String tableName, String issLogin,
 			ISSGameStartInformation status) {
 
 		if (issLogin.equals(status.getPlayerName(Player.FOREHAND))) {
 
 			updateISSTable(tableName, Player.MIDDLEHAND, Player.REARHAND,
 					Player.FOREHAND, status);
 		} else if (issLogin.equals(status.getPlayerName(Player.MIDDLEHAND))) {
 
 			updateISSTable(tableName, Player.REARHAND, Player.FOREHAND,
 					Player.MIDDLEHAND, status);
 		} else if (issLogin.equals(status.getPlayerName(Player.REARHAND))) {
 
 			updateISSTable(tableName, Player.FOREHAND, Player.MIDDLEHAND,
 					Player.REARHAND, status);
 		}
 	}
 
 	private void updateISSTable(String tableName, Player leftOpponent,
 			Player rightOpponent, Player player, ISSGameStartInformation status) {
 
 		log.debug("Updating ISS table: " + tableName + " " + leftOpponent + " "
 				+ rightOpponent + " " + player);
 
 		setPositions(tableName, leftOpponent, rightOpponent, player);
 
 		// FIXME (jansch 09.11.2010) this is only done for ISS games
 		SkatTablePanel table = tables.get(tableName);
 		table.setGameState(GameState.GAME_START);
 		table.setPlayerName(leftOpponent, status.getPlayerName(leftOpponent));
 		table.setPlayerTime(leftOpponent, status.getPlayerTime(leftOpponent));
 		table.setPlayerName(rightOpponent, status.getPlayerName(rightOpponent));
 		table.setPlayerTime(rightOpponent, status.getPlayerTime(rightOpponent));
 		table.setPlayerName(player, status.getPlayerName(player));
 		table.setPlayerTime(player, status.getPlayerTime(player));
 	}
 
 	/**
 	 * @see IJSkatView#getNewTableName()
 	 */
 	@Override
 	public String getNewTableName() {
 
 		return JOptionPane.showInputDialog(mainFrame,
 				strings.getString("new_table_dialog_message"), strings //$NON-NLS-1$
 						.getString("table")); //$NON-NLS-1$
 	}
 
 	/**
 	 * @see IJSkatView#updateISSMove(String, SkatGameData, ISSMoveInformation)
 	 */
 	@Override
 	public void updateISSMove(String tableName, SkatGameData gameData,
 			ISSMoveInformation moveInformation) {
 
 		Player movePlayer = moveInformation.getPlayer();
 
 		switch (moveInformation.getType()) {
 		// TODO add other types too
 		case DEAL:
 			setGameState(tableName, GameState.DEALING);
 			addCards(tableName, Player.FOREHAND,
 					moveInformation.getCards(Player.FOREHAND));
 			addCards(tableName, Player.MIDDLEHAND,
 					moveInformation.getCards(Player.MIDDLEHAND));
 			addCards(tableName, Player.REARHAND,
 					moveInformation.getCards(Player.REARHAND));
 			setGameState(tableName, GameState.BIDDING);
 			setActivePlayer(tableName, Player.MIDDLEHAND);
 			break;
 		case BID:
 			setGameState(tableName, GameState.BIDDING);
 			setBid(tableName, movePlayer, moveInformation.getBidValue(), true);
 			setBidValueToHold(tableName, moveInformation.getBidValue());
 			break;
 		case HOLD_BID:
 			setGameState(tableName, GameState.BIDDING);
 			setBid(tableName, movePlayer, gameData.getBidValue(), false);
 			setBidValueToMake(tableName,
 					SkatConstants.getNextBidValue(gameData.getBidValue()));
 			break;
 		case PASS:
 			setGameState(tableName, GameState.BIDDING);
 			setPass(tableName, movePlayer);
 			setBidValueToMake(tableName,
 					SkatConstants.getNextBidValue(gameData.getBidValue()));
 			break;
 		case SKAT_REQUEST:
 			setGameState(tableName, GameState.PICK_UP_SKAT);
 			break;
 		case SKAT_LOOKING:
 			setGameState(tableName, GameState.DISCARDING);
 			if (moveInformation.getSkat().size() == 2) {
 				setSkat(tableName, moveInformation.getSkat());
 			}
 			break;
 		case GAME_ANNOUNCEMENT:
 			setGameState(tableName, GameState.DECLARING);
 			setGameAnnouncement(tableName, movePlayer,
 					moveInformation.getGameAnnouncement());
 			setGameState(tableName, GameState.TRICK_PLAYING);
 			setTrickForeHand(tableName, Player.FOREHAND);
 			break;
 		case CARD_PLAY:
 			setGameState(tableName, GameState.TRICK_PLAYING);
 
 			if (gameData.getTricks().size() > 1) {
 
 				Trick currentTrick = gameData.getCurrentTrick();
 				Trick lastTrick = gameData.getLastTrick();
 
 				if (currentTrick.getFirstCard() != null
 						&& currentTrick.getSecondCard() == null
 						&& currentTrick.getThirdCard() == null) {
 					// first card in new trick
 					clearTrickCards(tableName);
 					setLastTrick(tableName, lastTrick.getForeHand(),
 							lastTrick.getFirstCard(),
 							lastTrick.getSecondCard(), lastTrick.getThirdCard());
 				}
 			}
 
 			playTrickCard(tableName, movePlayer, moveInformation.getCard());
 			break;
 		case TIME_OUT:
 			// TODO show time out message box
 			break;
 		}
 
 		// adjust player times
 		if (moveInformation.getMovePlayer() != MovePlayer.WORLD) {
 			// FIXME dirty hack
 			SkatTablePanel table = tables.get(tableName);
 
 			table.setPlayerTime(Player.FOREHAND,
 					moveInformation.getPlayerTime(Player.FOREHAND));
 			table.setPlayerTime(Player.MIDDLEHAND,
 					moveInformation.getPlayerTime(Player.MIDDLEHAND));
 			table.setPlayerTime(Player.REARHAND,
 					moveInformation.getPlayerTime(Player.REARHAND));
 		}
 	}
 
 	/**
 	 * @see IJSkatView#playTrickCard(String, Player, Card)
 	 */
 	@Override
 	public void playTrickCard(String tableName, Player position, Card card) {
 
 		removeCard(tableName, position, card);
 		setTrickCard(tableName, position, card);
 	}
 
 	/**
 	 * @see IJSkatView#setLastTrick(String, Player, Card, Card, Card)
 	 */
 	@Override
 	public void setLastTrick(String tableName, Player trickForeHand,
 			Card foreHandCard, Card middleHandCard, Card rearHandCard) {
 
 		SkatTablePanel table = tables.get(tableName);
 
 		table.setLastTrick(trickForeHand, foreHandCard, middleHandCard,
 				rearHandCard);
 	}
 
 	/**
 	 * @see IJSkatView#showPreferences()
 	 */
 	@Override
 	public void showPreferences() {
 
 		preferencesDialog.setVisible(true);
 	}
 
 	/**
 	 * @see IJSkatView#showTrainingOverview()
 	 */
 	@Override
 	public void showTrainingOverview() {
 
 		trainingOverview.setVisible(true);
 	}
 
 	/**
 	 * @see IJSkatView#closeTabPanel(java.lang.String)
 	 */
 	@Override
 	public void closeTabPanel(String tabName) {
 
 		AbstractTabPanel panel = (AbstractTabPanel) tabs.getSelectedComponent();
 		if (!tabName.equals(panel.getName())) {
 			for (Component currPanel : tabs.getComponents()) {
 				if (tabName.equals(currPanel.getName())) {
 					panel = (AbstractTabPanel) currPanel;
 				}
 			}
 		}
 
 		if (panel instanceof SkatTablePanel || panel instanceof ISSTablePanel) {
 			// remove from table list
 			tables.remove(panel.getName());
 		}
 
 		tabs.remove(panel);
 	}
 
 	/**
 	 * @see IJSkatView#getPlayerForInvitation(Set)
 	 */
 	@Override
 	public List<String> getPlayerForInvitation(Set<String> playerNames) {
 
 		List<String> result = null;
 
 		PlayerInvitationPanel invitationPanel = new PlayerInvitationPanel(
 				playerNames);
 		int dialogResult = JOptionPane.showConfirmDialog(mainFrame,
 				invitationPanel, "Player invitation",
 				JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
 
 		if (dialogResult == JOptionPane.OK_OPTION) {
 			result = invitationPanel.getPlayer();
 		}
 
 		log.debug("Players to invite: " + result);
 
 		return result;
 	}
 
 	/**
 	 * @see IJSkatView#addCards(String, Player, CardList)
 	 */
 	@Override
 	public void addCards(String tableName, Player player, CardList cards) {
 
 		tables.get(tableName).addCards(player, cards);
 	}
 
 	private void addTabPanel(AbstractTabPanel newPanel, String title) {
 
 		tabs.addTab(title, newPanel);
 		tabs.setTabComponentAt(tabs.indexOfComponent(newPanel),
 				new JSkatTabComponent(tabs, bitmaps));
 		tabs.setSelectedComponent(newPanel);
 		newPanel.setFocus();
 	}
 
 	/**
 	 * @see IJSkatView#setActivePlayer(String, Player)
 	 */
 	@Override
 	public void setActivePlayer(String tableName, Player player) {
 
 		tables.get(tableName).setActivePlayer(player);
 	}
 
 	/**
 	 * @see IJSkatView#setSeriesState(String, SeriesState)
 	 */
 	@Override
 	public void setSeriesState(String tableName, SeriesState state) {
 
 		tables.get(tableName).setSeriesState(state);
 	}
 
 	/**
 	 * @see IJSkatView#setSkat(String, CardList)
 	 */
 	@Override
 	public void setSkat(String tableName, CardList skat) {
 
 		tables.get(tableName).setSkat(skat);
 	}
 
 	/**
 	 * @see IJSkatView#setTrickNumber(String, int)
 	 */
 	@Override
 	public void setTrickNumber(String tableName, int trickNumber) {
 
 		tables.get(tableName).setTrickNumber(trickNumber);
 	}
 
 	/**
 	 * @see IJSkatView#showISSTableInvitation(String, String)
 	 */
 	@Override
 	public boolean showISSTableInvitation(String invitor, String tableName) {
 
 		boolean result = false;
 
 		String question = MessageFormat.format(
 				strings.getString("iss_table_invitation"), //$NON-NLS-1$
 				invitor, tableName);
 
 		int answer = JOptionPane.showConfirmDialog(null, question,
 				strings.getString("iss_table_invitation_title"), //$NON-NLS-1$
 				JOptionPane.YES_NO_OPTION);
 
 		if (answer == JOptionPane.YES_OPTION) {
 
 			result = true;
 		}
 
 		return result;
 	}
 
 	/**
 	 * @see IJSkatView#setGameResult(String, SkatGameData)
 	 */
 	@Override
 	public void setGameResult(String tableName, SkatGameData gameData) {
 
 		tables.get(tableName).addGameResult(gameData);
 	}
 
 	/**
 	 * @see IJSkatView#showCardNotAllowedMessage(Card)
 	 */
 	@Override
 	public void showCardNotAllowedMessage(Card card) {
 
 		String title = strings.getString("card_not_allowed_title"); //$NON-NLS-1$
 
 		String message = MessageFormat.format(
 				strings.getString("card_not_allowed"), //$NON-NLS-1$
 				card != null ? strings.getSuitStringForCardFace(card.getSuit())
 						: "--",
 				card != null ? strings.getRankStringForCardFace(card.getRank())
 						: "--");
 
 		showMessage(JOptionPane.ERROR_MESSAGE, title, message);
 	}
 
 	/**
 	 * @see IJSkatView#addTrainingResult(GameType, long, long, long, double,
 	 *      double)
 	 */
 	@Override
 	public void addTrainingResult(GameType gameType, long episodes,
 			long totalWonGames, long episodeWonGames,
 			double totalDeclarerNetError, double totalOpponentNetError) {
 
 		trainingOverview.addTrainingResult(gameType, episodes, totalWonGames,
 				episodeWonGames, totalDeclarerNetError, totalOpponentNetError);
 	}
 
 	/**
 	 * @see IJSkatView#setGameNumber(String, int)
 	 */
 	@Override
 	public void setGameNumber(String tableName, int gameNumber) {
 
 		tables.get(tableName).setGameNumber(gameNumber);
 	}
 
 	/**
 	 * @see IJSkatView#setPlayerNames(String, String, String, String)
 	 */
 	@Override
 	public void setPlayerNames(String tableName, String upperLeftPlayerName,
 			String upperRightPlayerName, String lowerPlayerName) {
 
 		tables.get(tableName).setPlayerNames(upperLeftPlayerName,
 				upperRightPlayerName, lowerPlayerName);
 	}
 
 	/**
 	 * @see IJSkatView#setDeclarer(String, Player)
 	 */
 	@Override
 	public void setDeclarer(String tableName, Player declarer) {
 
 		tables.get(tableName).setDeclarer(declarer);
 	}
 
 	/**
 	 * @see IJSkatView#closeISSPanels()
 	 */
 	@Override
 	public void closeISSPanels() {
 		for (Component currPanel : tabs.getComponents()) {
 			if (currPanel instanceof ISSLobbyPanel
 					|| currPanel instanceof ISSTablePanel) {
 				closeTabPanel(currPanel.getName());
 			}
 		}
 	}
 
 	@Override
 	public void setGameResultWithoutSkatList(String tableName,
 			SkatGameData gameData) {
 
 		tables.get(tableName).setGameResultWithoutSkatList(gameData);
 	}
 }
