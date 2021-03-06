 package com.mamehub.client;
 
 import java.awt.Adjustable;
 import java.awt.BorderLayout;
 import java.awt.Dimension;
 import java.awt.EventQueue;
 import java.awt.GridBagConstraints;
 import java.awt.GridBagLayout;
 import java.awt.Insets;
 import java.awt.event.ActionEvent;
 import java.awt.event.ActionListener;
 import java.awt.event.KeyAdapter;
 import java.awt.event.KeyEvent;
 import java.awt.event.MouseAdapter;
 import java.awt.event.MouseEvent;
 import java.awt.event.WindowAdapter;
 import java.awt.event.WindowEvent;
 import java.awt.image.BufferedImage;
 import java.io.BufferedWriter;
 import java.io.File;
 import java.io.FileWriter;
 import java.io.IOException;
 import java.io.StringWriter;
 import java.net.URI;
 import java.net.URISyntaxException;
 import java.net.URL;
 import java.util.ArrayList;
 import java.util.Calendar;
 import java.util.GregorianCalendar;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
 import java.util.TreeMap;
 
 import javax.imageio.ImageIO;
 import javax.swing.AbstractAction;
 import javax.swing.Action;
 import javax.swing.BoxLayout;
 import javax.swing.DefaultComboBoxModel;
 import javax.swing.Icon;
 import javax.swing.ImageIcon;
 import javax.swing.JComboBox;
 import javax.swing.JDialog;
 import javax.swing.JFrame;
 import javax.swing.JLabel;
 import javax.swing.JMenu;
 import javax.swing.JMenuBar;
 import javax.swing.JMenuItem;
 import javax.swing.JOptionPane;
 import javax.swing.JPanel;
 import javax.swing.JPopupMenu;
 import javax.swing.JScrollPane;
 import javax.swing.JSplitPane;
 import javax.swing.JTabbedPane;
 import javax.swing.JTable;
 import javax.swing.JTextArea;
 import javax.swing.JTextField;
 import javax.swing.JTree;
 import javax.swing.ListSelectionModel;
 import javax.swing.ScrollPaneConstants;
 import javax.swing.border.EmptyBorder;
 import javax.swing.event.DocumentEvent;
 import javax.swing.event.DocumentListener;
 import javax.swing.table.AbstractTableModel;
 import javax.swing.table.DefaultTableModel;
 import javax.swing.text.DefaultCaret;
 
 import org.apache.thrift.TException;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import com.mamehub.client.MameHubEngine.EmulatorHandler;
 import com.mamehub.client.audit.DirectorySelector;
 import com.mamehub.client.audit.GameAuditor;
 import com.mamehub.client.audit.GameAuditor.AuditHandler;
 import com.mamehub.client.audit.GameAuditor.RomQueryResult;
 import com.mamehub.client.login.LoginDialog;
 import com.mamehub.client.net.PeerMonitor;
 import com.mamehub.client.net.PeerMonitor.PeerMonitorListener;
 import com.mamehub.client.net.PeerMonitor.RomDownloadState;
 import com.mamehub.client.net.RpcEngine;
 import com.mamehub.client.net.RpcEngine.NetworkHandler;
 import com.mamehub.client.server.ClientHttpServer;
 import com.mamehub.client.server.MameHubClientRpcImpl;
 import com.mamehub.client.utility.ButtonColumn;
 import com.mamehub.client.utility.IpCountryFetcher;
 import com.mamehub.client.utility.OSValidator;
 import com.mamehub.rpc.NotAuthorizedException;
 import com.mamehub.thrift.ApplicationSettings;
 import com.mamehub.thrift.ChatStatus;
 import com.mamehub.thrift.Game;
 import com.mamehub.thrift.IpRangeData;
 import com.mamehub.thrift.Message;
 import com.mamehub.thrift.PeerState;
 import com.mamehub.thrift.Player;
 import com.mamehub.thrift.PlayerProfile;
 import com.mamehub.thrift.PlayerRomProfile;
 import com.mamehub.thrift.PlayerStatus;
 import com.mamehub.thrift.RomInfo;
 import com.mamehub.thrift.ServerState;
 
 public class MainFrame extends JFrame implements AuditHandler, NetworkHandler,
 		EmulatorHandler, PeerMonitorListener {
 	private static final long serialVersionUID = 1L;
 	final Logger logger = LoggerFactory.getLogger(MainFrame.class);
 
 	private JPanel contentPane;
 	private JTextField chatTextField;
 	private JTable playerTable;
 	private PlayerTableModel playerTableModel;
 	private DefaultTableModel downloadsTableModel;
 	private Map<Integer, RomDownloadState> downloadTableRowDownloadStateMap = new HashMap<Integer, RomDownloadState>();
 	private RpcEngine rpcEngine;
 	protected long lastTreeItemClickTime = 0L;
 	protected long lastSystemTreeItemClickTime = 0L;
 	MameHubEngine mameHubEngine;
 	PeerMonitor peerMonitor;
 	private JTextArea chatTextArea;
 	final JTabbedPane mainTabbedPane;
 
 	private Map<String, Player> knownPlayers = new HashMap<String, Player>();
 	private Map<String, Game> knownGames = new HashMap<String, Game>();
 	private DefaultTableModel gameTableModel;
 
 	protected long lastJoinGameListClickTime;
 
 	private JScrollPane chatScroll;
 
 	public IpCountryFetcher ipCountryFetcher;
 
 	public JLabel statusLabel;
 	private JTree systemTree;
 	Map<String, Set<String>> cloudRoms = new HashMap<String, Set<String>>();;
 	private JScrollPane downloadsPanel;
 	private JPanel hostGamePanel;
 	private JTextField gameSearchTextBox;
 	private List<RomInfo> searchResults = new ArrayList<RomInfo>();
 	private JTable joinGameTable;
 	private JComboBox chatStatusComboBox;
 	private ClientHttpServer clientHttpServer;
 	private boolean dying = false;
 	private JTable downloadsTable;
 	protected boolean giveFeedback = true;
 	private List<Game> joinGameList = new ArrayList<Game>();
 	private GameListModel gameListModel;
 	private JTable gameTable;
 
 	public class PlayerTableModel extends AbstractTableModel {
 		private static final long serialVersionUID = 1320567054920404367L;
 
 		public List<List<Object>> cells = new ArrayList<List<Object>>();
 
 		public PlayerTableModel() {
 			super();
 		}
 
 		public void update() {
 			cells.clear();
 			for (Player player : knownPlayers.values()) {
 				if (player.loggedIn == false)
 					continue;
 
 				List<Object> row = new ArrayList<Object>();
 				row.add(player.name);
 
 				row.add(player.status == null ? "" : player.status.chatStatus);
 				row.add(player.status == null ? "" : Utils
 						.osToShortOS(player.status.operatingSystem));
 
 				IpRangeData range = ipCountryFetcher
 						.getRangeData(player.ipAddress);
 				if (range != null && !range.countryCode2.equalsIgnoreCase("ZZ")) {
 					row.add(range.countryCode3);
 					URL flagUrl = Utils.getResource(MainFrame.class,
 							"/images/flags/" + range.countryCode2.toLowerCase()
 									+ ".png");
 					if (flagUrl != null) {
 						row.add(new ImageIcon(flagUrl));
 					} else {
 						row.add(null);
 					}
 				} else {
 					row.add("UNK");
 					row.add(new ImageIcon(Utils.getResource(MainFrame.class,
 							"/images/flags/us.png")));
 				}
 
 				PeerState ps = peerMonitor.getPeer(player);
 				if (ps != null && ps.ping >= 0) {
 					row.add(String.valueOf(ps.ping));
 				} else {
 					row.add("---");
 				}
 
 				cells.add(row);
 			}
 		}
 
 		@Override
 		public int getColumnCount() {
 			return 6;
 		}
 
 		@Override
 		public int getRowCount() {
 			return cells.size();
 		}
 
 		@Override
 		public Class<? extends Object> getColumnClass(int c) {
 			switch (c) {
 			case 0:
 			case 1:
 			case 2:
 			case 3:
 			case 5:
 				return String.class;
 			case 4:
 				return Icon.class;
 			}
 			throw new RuntimeException("OOPS");
 		}
 
 		@Override
 		public String getColumnName(int col) {
 			switch (col) {
 			case 0:
 				return "Name";
 			case 1:
 				return "Status";
 			case 2:
 				return "OS";
 			case 3:
 				return "Loc.";
 			case 4:
 				return "Flag";
 			case 5:
 				return "Lag";
 			default:
 				throw new RuntimeException("Tried to get an invalid column");
 			}
 		}
 
 		@Override
 		public Object getValueAt(int r, int c) {
 			return cells.get(r).get(c);
 		}
 
 		@Override
 		public boolean isCellEditable(int r, int c) {
 			return false;
 		}
 	}
 
 	public class GameListModel extends AbstractTableModel {
 		private static final long serialVersionUID = 1L;
 
 		protected static final int MACHINE_COLUMN = 3;
 
 		public List<List<Object>> rows = new ArrayList<List<Object>>();
 		public List<RomInfo> rowRomMap = new ArrayList<RomInfo>();
 		Icon noErrorsIcon = new ImageIcon(Utils.getResource(MainFrame.class,
 				"/images/emblem-default.png"));
 		Icon downloadIcon = new ImageIcon(Utils.getResource(MainFrame.class,
 				"/images/emblem-downloads.png"));
 		Icon errorsIcon = new ImageIcon(Utils.getResource(MainFrame.class,
 				"/images/emblem-unreadable-2.png"));
 
 		public GameListModel() {
 			super();
 		}
 
 		public void update() {
 			rows.clear();
 		}
 
 		@Override
 		public int getColumnCount() {
 			return 5;
 		}
 
 		@Override
 		public int getRowCount() {
 			return rows.size();
 		}
 
 		@Override
 		public Class<? extends Object> getColumnClass(int c) {
 			switch (c) {
 			case 0:
 				return Integer.class;
 			case 1:
 				return Icon.class;
 			case 2:
 			case MACHINE_COLUMN:
 			case 4:
 				return String.class;
 			}
 			throw new RuntimeException("OOPS");
 		}
 
 		@Override
 		public String getColumnName(int col) {
 			switch (col) {
 			case 0:
 				return "ID";
 			case 1:
 				return "Status";
 			case 2:
 				return "Name";
 			case MACHINE_COLUMN:
 				return "Machine";
 			case 4:
 				return "Rating";
 			default:
 				throw new RuntimeException("Tried to get an invalid column");
 			}
 		}
 
 		@Override
 		public Object getValueAt(int r, int c) {
 			if (c == 0) {
 				return new Integer(r);
 			}
 			return rows.get(r).get(c - 1);
 		}
 
 		@Override
 		public boolean isCellEditable(int r, int c) {
 			return false;
 		}
 
 		public List<Object> romInfoToRow(RomInfo romInfo,
 				PlayerProfile playerProfile) {
 			PlayerRomProfile playerRomProfile = playerProfile.romProfiles
 					.get(romInfo.id);
 
 			String machine = romInfo.system;
 			List<Object> retval = new ArrayList<Object>();
 			if (romInfo.missingReason == null) {
 				retval.add(noErrorsIcon);
 			} else if (cloudRoms.containsKey(machine)
 					&& cloudRoms.get(machine).contains(romInfo.romName)) {
 				retval.add(downloadIcon);
 			} else {
 				retval.add(errorsIcon);
 			}
 			if (romInfo.description == null) {
 				retval.add(romInfo.romName);
 			} else {
 				retval.add(romInfo.description);
 			}
 			retval.add(machine);
 
 			if (playerRomProfile != null) {
 				retval.add(String.valueOf(playerRomProfile.stars));
 			} else {
 				retval.add("---");
 			}
 
 			return retval;
 		}
 	}
 
 	/**
 	 * Create the frame.
 	 * 
 	 * @param rpcThread
 	 * @param rpcEngine
 	 * @throws IOException
 	 * @throws TException
 	 * @throws NotAuthorizedException
 	 */
 	@SuppressWarnings("restriction")
 	// Needed for mac-specific setup
 	public MainFrame(final RpcEngine rpcEngine,
 			final ClientHttpServer clientHttpServer) throws IOException,
 			NotAuthorizedException, TException {
 		URL u = Utils.getResource(MainFrame.class, "/MAMEHub.png");
 		System.out.println(u);
 		BufferedImage bi = ImageIO.read(u);
 		System.out.println("BUFFERED IMAGE: " + bi.getWidth() + " "
 				+ bi.getHeight());
 		this.setIconImage(bi);
 
 		ipCountryFetcher = new IpCountryFetcher(Utils.getResource(
 				MainFrame.class, "/IpToCountry.csv"));
 		this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
 		Utils.windows.add(this);
 		logger.info("Adding mainframe window");
 
 		this.clientHttpServer = clientHttpServer;
 
 		addWindowListener(new WindowAdapter() {
 			@Override
 			public void windowClosed(WindowEvent arg0) {
 				mameHubEngine.cancelAudit();
 				logger.info("Removing mainframe window");
 				if (rpcEngine != null && !rpcEngine.finished) {
 					rpcEngine.logout();
 				}
 				Utils.windows.remove(MainFrame.this);
 				if (Utils.windows.isEmpty()) {
 					logger.info("No windows left");
 					System.exit(0);
 				}
 			}
 		});
 		this.rpcEngine = rpcEngine;
 
 		rpcEngine.setMessageHandler(this);
 
 		setBounds(0, 0, 800, 600);
 		contentPane = new JPanel();
 		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
 		setContentPane(contentPane);
 
 		contentPane.setLayout(new BorderLayout(0, 0));
 
 		JSplitPane splitPane = new JSplitPane();
 		splitPane.setResizeWeight(0.8);
 		splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
 		contentPane.add(splitPane);
 
 		JPanel panel_3 = new JPanel();
 		splitPane.setLeftComponent(panel_3);
 		panel_3.setLayout(new BoxLayout(panel_3, BoxLayout.X_AXIS));
 
 		gameListModel = new GameListModel();
 		gameTableModel = new FirstColumnEditableTableModel(new String[] {
 				"Join", "Host Name", "Player Names", "Machine", "Rom" }, 0);
 		downloadsTableModel = new FirstColumnEditableTableModel(new String[] {
 				"Cancel", "File Name", "Percent Complete" }, 0);
 
 		mainTabbedPane = new JTabbedPane(JTabbedPane.TOP);
 		panel_3.add(mainTabbedPane);
 
 		hostGamePanel = new JPanel();
 		mainTabbedPane.addTab("Games", null, hostGamePanel, null);
 
 		hostGamePanel.setLayout(new BorderLayout(0, 0));
 
 		joinGameTable = new JTable(gameTableModel);
 		joinGameTable.addMouseListener(new MouseAdapter() {
 			@Override
 			public void mousePressed(MouseEvent e) {
 				if (e.isPopupTrigger())
 					doPop(e);
 			}
 
 			@Override
 			public void mouseReleased(MouseEvent e) {
 				if (e.isPopupTrigger())
 					doPop(e);
 			}
 
 			private void doPop(MouseEvent e) {
 				JoinGameListPopup menu = new JoinGameListPopup();
 				menu.show(e.getComponent(), e.getX(), e.getY());
 			}
 		});
 		joinGameTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
 		JScrollPane joinGameTablePanel = new JScrollPane(joinGameTable);
 		joinGameTablePanel.setPreferredSize(new Dimension(454, 100));
 		joinGameTablePanel.setSize(new Dimension(200, 0));
 		hostGamePanel.add(joinGameTablePanel, BorderLayout.NORTH);
 		joinGameTable.setMinimumSize(new Dimension(150, 23));
 
 		updateJoinGameList();
 
 		Action joinGame = new AbstractAction() {
 			private static final long serialVersionUID = 1L;
 
 			public void actionPerformed(ActionEvent e) {
 				int modelRow = Integer.valueOf(e.getActionCommand());
 				joinGameTable.setRowSelectionInterval(modelRow, modelRow);
 				joinSelectedGame();
 			}
 		};
 
 		ButtonColumn buttonColumn = new ButtonColumn(joinGameTable, joinGame, 0);
 
 		JPanel panel_1 = new JPanel();
 		hostGamePanel.add(panel_1, BorderLayout.CENTER);
 
 		class HostGameListPopup extends JPopupMenu {
 			private static final long serialVersionUID = 1L;
 
 			List<JMenuItem> stars = new ArrayList<JMenuItem>();
 
 			public HostGameListPopup() {
 				for (int a = 0; a < 5; a++) {
 					final int starCount = a + 1;
 					final JMenuItem mi = new JMenuItem("Rate " + (a + 1)
 							+ " Star" + (a == 0 ? "" : "s"));
 					stars.add(mi);
 					add(mi);
 					mi.addActionListener(new ActionListener() {
 
 						@Override
 						public void actionPerformed(ActionEvent arg0) {
 							RomInfo gameRomInfo = gameListModel.rowRomMap
 									.get(gameTable.getSelectedRow());
 							System.out.println("Giving " + gameRomInfo + " "
 									+ starCount + " STARS");
 							PlayerProfile playerProfile = Utils
 									.getPlayerProfile(rpcEngine);
 							playerProfile.romProfiles.put(gameRomInfo.id,
 									new PlayerRomProfile(gameRomInfo.id,
 											starCount, null));
 							Utils.commitProfile(rpcEngine);
 							updateGameTree(gameListModel,
 									mameHubEngine.gameAuditor);
 						}
 					});
 				}
 			}
 		}
 
 		gameTable = new JTable(gameListModel);
 		gameTable.setAutoCreateRowSorter(true);
 		gameTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
 		gameTable.setFillsViewportHeight(true);
 		gameTable.getColumnModel().getColumn(0).setMaxWidth(50);
 		gameTable.getColumnModel().getColumn(1).setMaxWidth(50);
 		gameTable.getColumnModel().getColumn(3).setMaxWidth(100);
 		gameTable.getColumnModel().getColumn(4).setMaxWidth(100);
 		gameTable.addMouseListener(new MouseAdapter() {
 
 			@Override
 			public void mousePressed(MouseEvent e) {
 				if (e.isPopupTrigger())
 					doPop(e);
 			}
 
 			@Override
 			public void mouseReleased(MouseEvent e) {
 				if (e.isPopupTrigger())
 					doPop(e);
 			}
 
 			private void doPop(MouseEvent e) {
 				HostGameListPopup menu = new HostGameListPopup();
 				menu.show(e.getComponent(), e.getX(), e.getY());
 			}
 
 			@Override
 			public void mouseClicked(MouseEvent me) {
 				if (gameTable.getSelectedRow() == -1) {
 					return;
 				}
 				int originalRow = gameTable.convertRowIndexToModel(gameTable
 						.getSelectedRow());
 				RomInfo gameRomInfo = gameListModel.rowRomMap.get(originalRow);
 				String systemName = gameRomInfo.system;
 				if (gameRomInfo != null && me.getButton() == MouseEvent.BUTTON1) {
 					// We clicked on a arcade rom or cart
 					if (lastTreeItemClickTime + 1000 > System
 							.currentTimeMillis()) {
 						// We double clicked
 						logger.info("DOUBLE CLICKED ON TREE");
 						if (!readyToEnterGame()) {
 						} else {
 							try {
 								logger.info("Got system " + systemName
 										+ " and rom " + gameRomInfo + "("
 										+ originalRow + ")");
 
 								if (gameRomInfo.missingReason != null) {
 									logger.info("Trying to download");
 									// This is a game we don't own, start the
 									// download process
 									tryToDownload(systemName, gameRomInfo);
 								} else if (mameHubEngine.isGameRunning()) {
 									JOptionPane
 											.showMessageDialog(
 													MainFrame.this,
 													"There is already a game in progress.  Please close that game before starting a new one.");
 								} else {
 									rpcEngine.hostGame(systemName,
 											gameRomInfo.romName, null);
 									boolean success = mameHubEngine.launchGame(
 											rpcEngine.getMyself().name,
 											systemName, gameRomInfo.filename,
 											true, null, 6805, 6805);
 									if (!success) {
 										JOptionPane
 												.showMessageDialog(
 														MainFrame.this,
 														"There is already a game in progress.  Please close that game before starting a new one.");
 									}
 								}
 							} catch (IOException e1) {
 								rpcEngine.leaveGame();
 								handleException(e1);
 							}
 						}
 						lastTreeItemClickTime = 0;
 					} else {
 						lastTreeItemClickTime = System.currentTimeMillis();
 					}
 				}
 			}
 		});
 
 		panel_1.setLayout(new BorderLayout(0, 0));
 		JScrollPane scrollPane2 = new JScrollPane(gameTable);
 		panel_1.add(scrollPane2);
 
 		JPanel panel_6 = new JPanel();
 		panel_1.add(panel_6, BorderLayout.SOUTH);
 		panel_6.setLayout(new BorderLayout(0, 0));
 
 		JLabel lblNewLabel = new JLabel("Search for Game");
 		panel_6.add(lblNewLabel, BorderLayout.WEST);
 
 		gameSearchTextBox = new JTextField();
 		gameSearchTextBox.getDocument().addDocumentListener(
 				new DocumentListener() {
 					public synchronized void changedUpdate(DocumentEvent e) {
 						updateSearch();
 					}
 
 					@Override
 					public synchronized void insertUpdate(DocumentEvent arg0) {
 						updateSearch();
 					}
 
 					@Override
 					public synchronized void removeUpdate(DocumentEvent arg0) {
 						updateSearch();
 					}
 
 					private void updateSearch() {
 						if (gameSearchTextBox.getText().length() == 0) {
 							searchResults.clear();
 							updateGameTree(gameListModel,
 									mameHubEngine.gameAuditor);
 							return;
 						}
 
 						logger.info("Searching for "
 								+ gameSearchTextBox.getText());
 						List<RomQueryResult> result = mameHubEngine.gameAuditor
 								.queryRoms(gameSearchTextBox.getText(),
 										cloudRoms);
 						searchResults.clear();
 						int a = 0;
 						for (RomQueryResult rqr : result) {
 							if (a < 10)
 								logger.info("" + result.size());
 							if (a < 10)
 								logger.info("GOT RESULT " + rqr.score + " "
 										+ rqr.romInfo);
 							searchResults.add(rqr.romInfo);
 							a++;
 						}
 						logger.info("Game Tree Updating");
 						updateGameTree(gameListModel, mameHubEngine.gameAuditor);
 						logger.info("Game Tree Updated");
 					}
 				});
 		panel_6.add(gameSearchTextBox, BorderLayout.CENTER);
 		gameSearchTextBox.setColumns(10);
 
 		downloadsTable = new JTable(downloadsTableModel);
 		downloadsTable.setMinimumSize(new Dimension(150, 23));
 
 		Action cancelDownload = new AbstractAction() {
 			private static final long serialVersionUID = 1L;
 
 			public void actionPerformed(ActionEvent e) {
 				int modelRow = Integer.valueOf(e.getActionCommand());
 				downloadTableRowDownloadStateMap.get(modelRow).cancel = true;
 			}
 		};
 
 		ButtonColumn buttonColumn2 = new ButtonColumn(downloadsTable,
 				cancelDownload, 0);
 
 		downloadsPanel = new JScrollPane(downloadsTable);
 		mainTabbedPane.addTab("Downloads", null, downloadsPanel, null);
 
 		JPanel panel_4 = new JPanel();
 		splitPane.setRightComponent(panel_4);
 		GridBagLayout gbl_panel_4 = new GridBagLayout();
 		gbl_panel_4.columnWidths = new int[] { 206, 300, 0 };
 		gbl_panel_4.rowHeights = new int[] { 146, 0 };
 		gbl_panel_4.columnWeights = new double[] { 1.0, 0.0, Double.MIN_VALUE };
 		gbl_panel_4.rowWeights = new double[] { 1.0, Double.MIN_VALUE };
 		panel_4.setLayout(gbl_panel_4);
 
 		JPanel panel = new JPanel();
 		GridBagConstraints gbc_panel = new GridBagConstraints();
 		gbc_panel.fill = GridBagConstraints.BOTH;
 		gbc_panel.insets = new Insets(0, 0, 0, 5);
 		gbc_panel.gridx = 0;
 		gbc_panel.gridy = 0;
 		panel_4.add(panel, gbc_panel);
 		panel.setLayout(new BorderLayout(0, 0));
 
 		chatTextArea = new JTextArea();
 		chatTextArea.setEditable(false);
 		chatTextArea.setWrapStyleWord(true);
 		chatTextArea.setLineWrap(true);
 		chatTextArea.setText("Welcome to MAMEHub!\n");
 		// We will manually handle advancing chat window
 		DefaultCaret caret = (DefaultCaret) chatTextArea.getCaret();
 		caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
 
 		chatScroll = new JScrollPane(chatTextArea);
 		chatScroll
 				.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
 		chatScroll
 				.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
 		panel.add(chatScroll);
 
 		JPanel panel_2 = new JPanel();
 		panel_2.setSize(new Dimension(100, 0));
 		panel.add(panel_2, BorderLayout.SOUTH);
 		panel_2.setLayout(new BorderLayout(0, 0));
 
 		chatTextField = new JTextField();
 		chatTextField.setFocusTraversalKeysEnabled(false);
 		chatTextField.addKeyListener(new KeyAdapter() {
 			@Override
 			public void keyTyped(KeyEvent arg0) {
 				if (arg0.getKeyChar() == '\n') {
 					rpcEngine.broadcastMessage(new Message()
 							.setChat(chatTextField.getText()));
 					chatTextField.setText("");
 				} else if (arg0.getKeyChar() == '\t') {
 					String tokens[] = chatTextField.getText().split("\\s+");
 					if (tokens.length == 0) {
 						return;
 					}
 					String lastToken = tokens[tokens.length - 1];
 					Player match = null;
 					for (Player p : knownPlayers.values()) {
 						if (p.name.toLowerCase().startsWith(
 								lastToken.toLowerCase())) {
 							if (match != null) {
 								// return;
 							}
 							match = p;
 						}
 					}
 					if (match != null) {
 						// Erase the last token down to the first whitespace and
 						// then add the match
 						while (chatTextField.getText().length() > 0
 								&& chatTextField.getText().charAt(
 										chatTextField.getText().length() - 1) != ' ') {
 							chatTextField
 									.setText(chatTextField.getText()
 											.substring(
 													0,
 													chatTextField.getText()
 															.length() - 1));
 						}
 						if (chatTextField.getText().length() > 0) {
 							chatTextField.setText(chatTextField.getText()
 									+ match.name);
 						} else {
 							chatTextField.setText(match.name);
 						}
 					}
 				}
 			}
 		});
 		chatTextField.setMinimumSize(new Dimension(100, 28));
 		panel_2.add(chatTextField, BorderLayout.CENTER);
 		chatTextField.setColumns(10);
 
 		chatStatusComboBox = new JComboBox();
 		chatStatusComboBox.addActionListener(new ActionListener() {
 			public void actionPerformed(ActionEvent arg0) {
 				ChatStatus newChatStatus = (ChatStatus) chatStatusComboBox
 						.getSelectedItem();
 				System.out.println("CHAT STATUS CHANGED: " + newChatStatus);
 				ApplicationSettings as = Utils.getApplicationSettings();
 				as.chatStatus = newChatStatus;
 				Utils.putApplicationSettings(as);
 				updatePlayerStatus();
 			}
 		});
 		chatStatusComboBox.setModel(new DefaultComboBoxModel(ChatStatus
 				.values()));
 		chatStatusComboBox
 				.setSelectedItem(Utils.getApplicationSettings().chatStatus);
 		panel_2.add(chatStatusComboBox, BorderLayout.EAST);
 
 		JPanel panel_5 = new JPanel();
 		GridBagConstraints gbc_panel_5 = new GridBagConstraints();
 		gbc_panel_5.fill = GridBagConstraints.BOTH;
 		gbc_panel_5.gridx = 1;
 		gbc_panel_5.gridy = 0;
 		panel_4.add(panel_5, gbc_panel_5);
 		panel_5.setLayout(new BorderLayout(0, 0));
 
 		playerTableModel = new PlayerTableModel();
 		playerTable = new JTable(playerTableModel);
 		playerTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
 		playerTable.setFillsViewportHeight(true);
 		playerTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
 		playerTable.getColumnModel().getColumn(0).setPreferredWidth(100);
 		playerTable.getColumnModel().getColumn(1).setPreferredWidth(70);
 		playerTable.getColumnModel().getColumn(2).setPreferredWidth(50);
 		playerTable.getColumnModel().getColumn(3).setPreferredWidth(50);
 		playerTable.getColumnModel().getColumn(4).setPreferredWidth(50);
 		playerTable.getColumnModel().getColumn(5).setPreferredWidth(50);
 		JScrollPane scrollPane = new JScrollPane(playerTable);
 		scrollPane
 				.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
 		panel_5.add(scrollPane, BorderLayout.CENTER);
 
 		JMenuBar menuBar = new JMenuBar();
 		contentPane.add(menuBar, BorderLayout.NORTH);
 
 		JMenu mnFile = new JMenu("File");
 		menuBar.add(mnFile);
 
 		JMenuItem mntmExit = new JMenuItem("Exit");
 		mntmExit.addActionListener(new ActionListener() {
 			public void actionPerformed(ActionEvent e) {
 				MainFrame.this.dispose();
 			}
 		});
 		mnFile.add(mntmExit);
 
 		JMenu mnEdit = new JMenu("Edit");
 		menuBar.add(mnEdit);
 
 		JMenuItem mntmSettings = new JMenuItem("Settings");
 		mntmSettings.addActionListener(new ActionListener() {
 			public void actionPerformed(ActionEvent e) {
 				new UpdateSettingsDialog(MainFrame.this, rpcEngine)
 						.setVisible(true);
 			}
 		});
 		mnEdit.add(mntmSettings);
 
 		JMenuItem mntmUpdateProfile = new JMenuItem("Profile");
 		mntmUpdateProfile.addActionListener(new ActionListener() {
 			public void actionPerformed(ActionEvent e) {
 				new UpdateProfileDialog(MainFrame.this, rpcEngine)
 						.setVisible(true);
 			}
 		});
 		mnEdit.add(mntmUpdateProfile);
 
 		JMenu mnConnection = new JMenu("Connection");
 		menuBar.add(mnConnection);
 
 		JMenuItem checkForwardedPortMenuitem = new JMenuItem(
 				"Check Port Forwarding");
 		checkForwardedPortMenuitem.addActionListener(new ActionListener() {
 			public void actionPerformed(ActionEvent arg0) {
 				try {
 					Utils.openWebpage(new URI(
 							"http://portchecker.net/udp.php?p=6805"));
 					Utils.openWebpage(new URI(
 							"http://www.whatsmyip.org/port-scanner"));
 				} catch (URISyntaxException e) {
 					// TODO Auto-generated catch block
 					e.printStackTrace();
 				}
 			}
 		});
 		mnConnection.add(checkForwardedPortMenuitem);
 
 		JMenuItem mntmTestPing = new JMenuItem("Test Ping");
 		mntmTestPing.addActionListener(new ActionListener() {
 			public void actionPerformed(ActionEvent arg0) {
 				try {
 					Utils.openWebpage(new URI("http://www.pingtest.net/"));
 				} catch (URISyntaxException e) {
 					// TODO Auto-generated catch block
 					e.printStackTrace();
 				}
 			}
 		});
 		mnConnection.add(mntmTestPing);
 
 		JMenu mnAudit = new JMenu("Audit");
 		menuBar.add(mnAudit);
 
 		JMenuItem mntmSelectFolders = new JMenuItem("Select Folders");
 		mntmSelectFolders.addActionListener(new ActionListener() {
 			public void actionPerformed(ActionEvent e) {
 				DirectorySelector ds = new DirectorySelector();
 				ds.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
 				ds.setVisible(true);
 			}
 		});
 		mnAudit.add(mntmSelectFolders);
 
 		JMenuItem mntmRescanFolders = new JMenuItem("Rescan Folders");
 		mntmRescanFolders.addActionListener(new ActionListener() {
 			public void actionPerformed(ActionEvent e) {
 				mameHubEngine.startAudit(true);
 			}
 		});
 		mnAudit.add(mntmRescanFolders);
 
 		statusLabel = new JLabel("Welcome to MAMEHub!");
 		contentPane.add(statusLabel, BorderLayout.SOUTH);
 
 		mameHubEngine = new MameHubEngine(this, this);
 		peerMonitor = new PeerMonitor(this, mameHubEngine);
 
 		ServerState serverState = rpcEngine.getServerState();
 		knownPlayers.putAll(serverState.loggedInPlayers);
 		knownGames.putAll(serverState.games);
 		peerMonitor.insertAll(serverState.loggedInPlayers.values());
 		playerTableModel.update();
 
 		rpcEngine.startThread();
 
 		updateJoinGameList();
 
 		java.awt.EventQueue.invokeLater(new Runnable() {
 			@Override
 			public void run() {
 				MainFrame.this.validate();
 				MainFrame.this.toFront();
 				MainFrame.this.requestFocus();
 				MainFrame.this.repaint();
 				MainFrame.this.setAlwaysOnTop(false);
 			}
 		});
 
 		MainFrame.this.validate();
 		MainFrame.this.toFront();
 		MainFrame.this.requestFocus();
 		MainFrame.this.repaint();
 		MainFrame.this.setAlwaysOnTop(true);
 	}
 
 	protected void updatePlayerStatus() {
 		ChatStatus newChatStatus = (ChatStatus) chatStatusComboBox
 				.getSelectedItem();
 		rpcEngine.updateStatus(new PlayerStatus(newChatStatus, OSValidator
 				.getOperatingSystemStatus()));
 	}
 
 	protected void joinSelectedGame() {
 		if (!readyToEnterGame()) {
 			JOptionPane.showMessageDialog(MainFrame.this,
 					"You cannot enter a game now.");
 		} else if (joinGameTable.getSelectedRow() >= 0) {
 			Game game = joinGameList.get(joinGameTable.getSelectedRow());
 
 			try {
 				String system = game.system;
 				RomInfo systemRomInfo = null;
 				RomInfo gameRomInfo = null;
 
 				if (system.equalsIgnoreCase("arcade")) {
 					systemRomInfo = null;
 					gameRomInfo = mameHubEngine.getMameRomInfo(game.rom);
 				} else {
 					systemRomInfo = mameHubEngine.getMessRomInfo(game.system);
 					System.out.println("GAME " + game);
 					System.out.println("SYSTEM ROM INFO " + systemRomInfo);
 					gameRomInfo = mameHubEngine.getCart(game.system, game.rom);
 					System.out.println("GAME ROM INFO " + gameRomInfo);
 				}
 
 				if (systemRomInfo != null
 						&& systemRomInfo.missingReason != null) {
 					// This is a bios we don't own, start the download process
 					tryToDownload(system, gameRomInfo);
 					return;
 				}
 
 				if (gameRomInfo.missingReason != null) {
 					// This is a game we don't own, start the download process
 					tryToDownload(system, gameRomInfo);
 					return;
 				}
 
 				if (mameHubEngine.isGameRunning()) {
 					JOptionPane
 							.showMessageDialog(
 									MainFrame.this,
 									"There is already a game in progress.  Please close that game before starting a new one.");
 				} else {
 					logger.info("ROM INFO: " + gameRomInfo);
 					String errorMessage = rpcEngine.joinGame(game.id);
 					logger.info("GAME INFO: " + game);
 					if (errorMessage.length() == 0) {
 						boolean success = mameHubEngine.launchGame(
 								rpcEngine.getMyself().name, game.system,
 								gameRomInfo.filename, false,
 								game.hostPlayerIpAddress, 6805,
 								game.hostPlayerPort);
 						if (!success) {
 							JOptionPane
 									.showMessageDialog(
 											MainFrame.this,
 											"There is already a game in progress.  Please close that game before starting a new one.");
 						}
 					} else {
 						JOptionPane.showMessageDialog(MainFrame.this,
 								"Could not join game (reason: " + errorMessage
 										+ ")");
 					}
 				}
 			} catch (IOException e1) {
 				e1.printStackTrace();
 			}
 		}
 	}
 
 	protected boolean readyToEnterGame() {
 		if (mameHubEngine.isAuditing()) {
 			JOptionPane.showMessageDialog(MainFrame.this,
 					"Please wait until audit is finished");
 			return false;
 		}
 		return true;
 	}
 
 	protected void tryToDownload(String systemName, RomInfo gameRomInfo) {
 		if (JOptionPane.showConfirmDialog(MainFrame.this,
 				"Are you legally entitled to own this ROM?", "Consent box.",
 				JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
 			return;
 		}
 
 		RomInfo romInfo = mameHubEngine.getMessRomInfo(systemName);
 		if (romInfo != null && romInfo.missingReason != null) {
 			// This is a bios we don't own, start the download process
 			Set<String> romsNeeded = new HashSet<String>();
 			romsNeeded.add(romInfo.romName);
 			boolean requestGranted = peerMonitor.requestRoms("Bios",
 					romsNeeded, null);
 			mainTabbedPane.setSelectedIndex(2);
 			if (requestGranted) {
 				JOptionPane.showMessageDialog(MainFrame.this,
 						"Downloading BIOS from peers.");
 			} else {
 				JOptionPane.showMessageDialog(MainFrame.this,
 						"Server could not find peers with BIOS.");
 			}
 			return;
 		}
 
 		Set<String> romsNeeded = new HashSet<String>();
 		romsNeeded.add(gameRomInfo.romName);
 		if (gameRomInfo.parentRom != null) {
 			romsNeeded.add(gameRomInfo.parentRom);
 		}
 		if (gameRomInfo.cloneRom != null) {
 			romsNeeded.add(gameRomInfo.cloneRom);
 		}
 		String chdName = null;
 		if (gameRomInfo.chdFilename != null) {
 			chdName = gameRomInfo.id;
 		}
 		logger.info("Reqeusting roms: " + romsNeeded);
 		boolean requestGranted = peerMonitor.requestRoms(systemName,
 				romsNeeded, chdName);
 		mainTabbedPane.setSelectedIndex(2);
 		logger.info("Request granted: " + requestGranted);
 		if (requestGranted) {
 			JOptionPane.showMessageDialog(MainFrame.this,
 					"Downloading rom from peers.");
 		} else {
 			JOptionPane.showMessageDialog(MainFrame.this,
 					"Server could not find peers with roms.");
 		}
 	}
 
 	@Override
 	public void auditFinished(final GameAuditor gameAuditor) {
 		EventQueue.invokeLater(new Runnable() {
 
 			@Override
 			public void run() {
 				Map<String, Set<String>> romList = new HashMap<String, Set<String>>();
 
 				Set<String> arcadeRoms = new HashSet<String>();
 				for (Map.Entry<String, RomInfo> entry : gameAuditor
 						.getMameRomInfoMap().entrySet()) {
 					if (entry.getValue().missingReason == null) {
 						// logger.info("Adding arcade game " + entry.getKey());
 						arcadeRoms.add(entry.getKey());
 					}
 				}
 				romList.put("Arcade", arcadeRoms);
 
 				Set<String> biosRoms = new HashSet<String>();
 				for (Map.Entry<String, RomInfo> entry : gameAuditor
 						.getMessRomInfoMap().entrySet()) {
 					if (entry.getValue().missingReason == null) {
 						// logger.info("Adding console bios " + entry.getKey());
 						biosRoms.add(entry.getKey());
 					}
 				}
 				romList.put("Bios", biosRoms);
 
 				for (String system : gameAuditor.getMessRomInfoMap().keySet()) {
 					Set<String> ownedRomNames = new HashSet<String>();
 					for (Map.Entry<String, RomInfo> entry : gameAuditor
 							.getSystemRomInfoMap(system).entrySet()) {
 						if (entry.getValue().missingReason == null) {
 							// logger.info("Adding cart " + entry.getKey());
 							ownedRomNames.add(entry.getKey());
 						}
 					}
 					romList.put(system, ownedRomNames);
 				}
 
 				MameHubClientRpcImpl.updateRoms(romList);
 
 				// updateSystemTree(gameAuditor);
 				updateGameTree(gameListModel, gameAuditor);
 				statusLabel.setText("Audit finished!");
 			}
 
 		});
 	}
 
 	private void updateGameTree(GameListModel model, GameAuditor gameAuditor) {
 		// logger.info("Updating game tree");
 		model.rowRomMap.clear();
 		model.rows.clear();
 
 		PlayerProfile playerProfile = Utils.getPlayerProfile(rpcEngine);
 
 		if (!gameSearchTextBox.getText().isEmpty()) {
 			// Add items in search relevance order
 			for (RomInfo romInfo : searchResults) {
 				model.rowRomMap.add(romInfo);
 				model.rows.add(model.romInfoToRow(romInfo, playerProfile));
 			}
 		} else {
 			Map<String, RomInfo> gamesFound = new TreeMap<String, RomInfo>();
 			Map<String, RomInfo> gamesCloud = new TreeMap<String, RomInfo>();
 			Map<String, RomInfo> gamesMissing = new TreeMap<String, RomInfo>();
 			for (Map.Entry<String, RomInfo> entry : gameAuditor
 					.getMameRomInfoMap().entrySet()) {
 				RomInfo romInfo = entry.getValue();
 				if (romInfo.missingReason == null) {
 					gamesFound.put(romInfo.description, romInfo);
 				} else if (cloudRoms.containsKey("Arcade")
 						&& cloudRoms.get("Arcade").contains(romInfo.romName)) {
 					gamesCloud.put(romInfo.description, romInfo);
 				} else {
 					gamesMissing.put(romInfo.description, romInfo);
 				}
 			}
 			for (Map.Entry<String, RomInfo> messRomEntry : gameAuditor
 					.getMessRomInfoMap().entrySet()) {
 				String system = messRomEntry.getKey();
 				for (Map.Entry<String, RomInfo> cartEntry : gameAuditor
 						.getSystemRomInfoMap(system).entrySet()) {
 					String cartName = cartEntry.getKey();
 					RomInfo cartRomInfo = cartEntry.getValue();
 
 					if (cartRomInfo.missingReason == null) {
 						gamesFound.put(cartName, cartRomInfo);
 					} else if (cloudRoms.containsKey(system)
 							&& cloudRoms.get(system).contains(cartName)) {
 						gamesCloud.put(cartName, cartRomInfo);
 					} else {
 						gamesMissing.put(cartName, cartRomInfo);
 					}
 				}
 			}
 			// Add items in alphabetical order
 			for (RomInfo romInfo : gamesFound.values()) {
 				model.rowRomMap.add(romInfo);
 				model.rows.add(model.romInfoToRow(romInfo, playerProfile));
 			}
 			for (RomInfo romInfo : gamesCloud.values()) {
 				model.rowRomMap.add(romInfo);
 				model.rows.add(model.romInfoToRow(romInfo, playerProfile));
 			}
 			/*
 			 * for(RomInfo romInfo : gamesMissing.values()) {
 			 * model.rowRomMap.add(romInfo);
 			 * model.rows.add(model.romInfoToRow(romInfo)); }
 			 */
 		}
 
 		model.fireTableDataChanged();
 	}
 
 	@Override
 	public void auditError(Exception e) {
 		handleException(e);
 	}
 
 	@Override
 	public void handleMessage(final Message message) {
 		EventQueue.invokeLater(new Runnable() {
 
 			@Override
 			public void run() {
 
 				// logger.info("GOT MESSAGE: " + message);
 				if (message.chat != null) {
 					Player player = getPlayer(message.sourceId);
 					String playerName = "(Unknown)";
 					if (player != null) {
 						playerName = player.name;
 					}
 					addChat(message.timestamp, "<" + playerName + "> "
 							+ message.chat);
 					if (getChatStatus() == ChatStatus.ONLINE) {
 						SoundEngine.instance.playSoundIfNotActive("ding");
 					}
 				}
 				if (message.playerChanged != null) {
 					Player oldPlayer = knownPlayers
 							.get(message.playerChanged.id);
 					Player newPlayer = message.playerChanged;
 					if (oldPlayer == null || oldPlayer.loggedIn == false) {
 						addChat(message.timestamp, "*"
 								+ message.playerChanged.name + " joins");
 						if (getChatStatus() == ChatStatus.ONLINE) {
 							SoundEngine.instance.playSound("playerjoin");
 						}
 						peerMonitor.insertPeer(message.playerChanged);
 					} else if (oldPlayer != null
 							&& message.playerChanged.loggedIn == false) {
 						addChat(message.timestamp, "*"
 								+ message.playerChanged.name + " leaves");
 						peerMonitor.removePeer(message.playerChanged);
 					} else {
 						if (oldPlayer.inGame == null
 								&& newPlayer.inGame != null
 								&& knownGames.containsKey(newPlayer.inGame)) {
 							Game game = knownGames.get(newPlayer.inGame);
 							if (game != null) {
 								Player host = knownPlayers
 										.get(game.hostPlayerId);
 								if (host != null) {
 									addChat(message.timestamp, "*"
 											+ message.playerChanged.name
 											+ " joins " + host.name
 											+ "'s game of "
 											+ getGameDescription(game));
 								}
 							}
 						}
 						peerMonitor.updatePeer(message.playerChanged);
 					}
 
 					knownPlayers.put(message.playerChanged.id,
 							message.playerChanged);
 					playerTableModel.update();
 					playerTableModel.fireTableDataChanged();
 					updateJoinGameList();
 					logger.info("PLAYER CHANGED");
 				}
 				if (message.gameChanged != null) {
 					for (Game game : knownGames.values()) {
 						if (game.id.equals(message.gameChanged.id)) {
 							knownGames.remove(game);
 							break;
 						}
 					}
 					if (message.gameChanged.endTime > 0) {
 						addChat(message.timestamp, "*"
 								+ getGameDescription(message.gameChanged)
 								+ " has stopped");
 						if (getChatStatus() == ChatStatus.ONLINE) {
 							SoundEngine.instance.playSound("gamestop");
 						}
 					} else {
 						addChat(message.timestamp, "*"
 								+ getGameDescription(message.gameChanged)
 								+ " has started");
 						if (getChatStatus() == ChatStatus.ONLINE) {
 							SoundEngine.instance.playSound("gamestart");
 						}
 					}
 					knownGames.put(message.gameChanged.id, message.gameChanged);
 					logger.info("GAME CHANGED: " + message.gameChanged);
 					updateJoinGameList();
 				}
 			}
 		});
 	}
 
 	protected ChatStatus getChatStatus() {
 		return (ChatStatus) chatStatusComboBox.getSelectedItem();
 	}
 
 	private void updateJoinGameList() {
 		gameTableModel.setRowCount(0);
 		joinGameList.clear();
 		for (Game game : knownGames.values()) {
 			if (game.endTime > 0) {
 				continue;
 			}
 
 			Player hostPlayer = knownPlayers.get(game.hostPlayerId);
 			String playerName = game.hostPlayerId;
 			if (hostPlayer == null) {
 				continue;
 			}
 
 			joinGameList.add(game);
 			playerName = hostPlayer.name;
 			String romname = getGameDescription(game);
 
 			String playersInGame = "";
 			for (Player player : knownPlayers.values()) {
 				if (player.inGame != null && player.inGame.equals(game.id)
 						&& player != hostPlayer) {
 					if (playersInGame.length() > 0) {
 						playersInGame += ", ";
 					}
 					playersInGame += player.name;
 				}
 			}
 			gameTableModel.addRow(new String[] { "Click to Join", playerName,
 					playersInGame, game.system, romname });
 		}
 		gameTableModel.fireTableDataChanged();
 		joinGameTable.revalidate();
 		logger.info("UPDATED WITH " + knownGames.size() + " ("
 				+ gameTableModel.getRowCount() + ") VALUES");
 	}
 
 	private String getGameDescription(Game game) {
 		if (game.system.equalsIgnoreCase("Arcade")) {
 			if (mameHubEngine.getMameRomInfo(game.rom) != null) {
 				return mameHubEngine.getMameRomInfo(game.rom).description;
 			} else {
 				return game.rom;
 			}
 		} else {
 			return game.rom;
 		}
 	}
 
 	private Player getPlayer(String sourceId) {
 		if (knownPlayers.containsKey(sourceId)) {
 			return knownPlayers.get(sourceId);
 		} else {
 			Player player = rpcEngine.getPlayer(sourceId);
 			knownPlayers.put(sourceId, player);
 			return player;
 		}
 	}
 
 	@Override
 	public void gameFinished(int returnCode, final File outputFile) {
 		logger.info("LEAVING GAME");
 		rpcEngine.leaveGame();
 
 		java.awt.EventQueue.invokeLater(new Runnable() {
 			@Override
 			public void run() {
 				if (giveFeedback) {
 					String result = (String) JOptionPane
 							.showInputDialog(
 									MainFrame.this,
 									"If you have any feedback, please enter it here.  Thanks!",
 									"Customized Dialog",
 									JOptionPane.PLAIN_MESSAGE, null, null, "");
 					if (result != null && result.length() > 0) {
 						try {
 							rpcEngine.postUserFeedback(result,
 									Utils.fileToString(outputFile));
 						} catch (IOException e) {
 							e.printStackTrace();
 						}
 					}
 					giveFeedback = false;
 				}
 
 				if (outputFile != null && outputFile.exists()) {
 					try {
 						if (Utils.isWindows()) {
 							Runtime runtime = Runtime.getRuntime();
 							logger.info("Running: "
 									+ "C:\\Windows\\write.exe \""
 									+ outputFile.getCanonicalPath().replace(
 											"/", "\\") + "\"");
 							runtime.exec("C:\\Windows\\write.exe \""
 									+ outputFile.getCanonicalPath().replace(
 											"/", "\\") + "\"");
 						} else {
 							java.awt.Desktop.getDesktop().edit(outputFile);
 						}
 					} catch (IOException e) {
 						// TODO Auto-generated catch block
 						e.printStackTrace();
 					}
 				}
 			}
 		});
 	}
 
 	private void shutdownRpcEngine() {
 		if (rpcEngine != null) {
 			rpcEngine.finished = true;
 		}
 	}
 
 	@Override
 	public void handleSessionExpired() {
 		if (dying)
 			return;
 		dying = true;
 		shutdownRpcEngine();
 		java.awt.EventQueue.invokeLater(new Runnable() {
 
 			@Override
 			public void run() {
 				LoginDialog introDialog;
 				try {
 					introDialog = new LoginDialog(clientHttpServer);
 					introDialog.setVisible(true);
 					JOptionPane
 							.showMessageDialog(introDialog,
 									"Your session has expired or the server has restarted, please re-login");
 				} catch (IOException e) {
 					throw new RuntimeException(e);
 				}
 				dispose();
 			}
 
 		});
 	}
 
 	@Override
 	public void handleException(Exception e) {
 		if (dying)
 			return;
 		dying = true;
 		shutdownRpcEngine();
 		e.printStackTrace();
 		try {
 			BufferedWriter bw = new BufferedWriter(new FileWriter(
 					"ErrorLog.txt"));
 			bw.write(e.getMessage() + "\n");
 			for (StackTraceElement element : e.getStackTrace()) {
 				bw.write(element.toString() + "\n");
 			}
 			bw.close();
 		} catch (IOException e1) {
 			// TODO Auto-generated catch block
 			e1.printStackTrace();
 		}
 		java.awt.EventQueue.invokeLater(new Runnable() {
 
 			@Override
 			public void run() {
 				LoginDialog introDialog;
 				try {
 					introDialog = new LoginDialog(clientHttpServer);
 					introDialog.setVisible(true);
 					JOptionPane
 							.showMessageDialog(introDialog,
 									"An exception has occurred and has been logged. Returning to login screen");
 				} catch (IOException e) {
 					throw new RuntimeException(e);
 				}
 				dispose();
 			}
 
 		});
 	}
 
 	@Override
 	public void handleServerDown(Exception e) {
 		if (dying)
 			return;
 		dying = true;
 		shutdownRpcEngine();
 		e.printStackTrace();
 		java.awt.EventQueue.invokeLater(new Runnable() {
 
 			@Override
 			public void run() {
 				LoginDialog introDialog;
 				try {
 					introDialog = new LoginDialog(clientHttpServer);
 					introDialog.setVisible(true);
 					JOptionPane
 							.showMessageDialog(
 									introDialog,
 									"The server hiccupped or is down, please try to login again or check the blog for details.");
 				} catch (IOException e) {
 					throw new RuntimeException(e);
 				}
 				dispose();
 			}
 
 		});
 	}
 
 	@Override
 	public void updateCloudRoms(final Map<String, Set<String>> downloadableRoms) {
 		java.awt.EventQueue.invokeLater(new Runnable() {
 
 			@Override
 			public void run() {
 				cloudRoms = downloadableRoms;
 				if (mameHubEngine.isAuditing()) {
 					// Audit in progress
 					return;
 				}
 				// updateSystemTree(mameHubEngine.gameAuditor);
 				updateGameTree(gameListModel, mameHubEngine.gameAuditor);
 			}
 		});
 	}
 
 	@Override
 	public synchronized void updateAuditStatus(String status) {
 		statusLabel.setText(status);
 	}
 
 	@Override
 	public void inGameException(Exception e) {
 		try {
 			StringWriter sw = new StringWriter();
 			sw.write(e.getMessage() + "\n");
 			for (StackTraceElement element : e.getStackTrace()) {
 				sw.write(element.toString() + "\n");
 			}
 			sw.close();
 			rpcEngine.sendError(sw.toString());
 		} catch (IOException e1) {
 			// TODO Auto-generated catch block
 			e1.printStackTrace();
 		}
 	}
 
 	class JoinGameListPopup extends JPopupMenu implements ActionListener {
 		private static final long serialVersionUID = 1L;
 
 		JMenuItem joinGame;
 
 		public JoinGameListPopup() {
 			joinGame = new JMenuItem("Join Game");
 			add(joinGame);
 
 			// ...for each JMenuItem instance:
 			joinGame.addActionListener(this);
 		}
 
 		@Override
 		public void actionPerformed(ActionEvent arg0) {
 			if (arg0.getSource() == joinGame) {
 				joinSelectedGame();
 			}
 		}
 	}
 
 	class FirstColumnEditableTableModel extends DefaultTableModel {
 		private static final long serialVersionUID = 1L;
 
 		public FirstColumnEditableTableModel(String[] strings, int i) {
 			super(strings, i);
 		}
 
 		@Override
 		public boolean isCellEditable(int row, int col) {
 			return col == 0;
 		}
 	}
 
 	@Override
 	public void statesUpdated() {
 		EventQueue.invokeLater(new Runnable() {
 
 			@Override
 			public void run() {
 				if (playerTableModel != null) {
 					playerTableModel.update();
 					playerTableModel.fireTableDataChanged();
 				}
 			}
 
 		});
 	}
 
 	@Override
 	public void updateDownloads(
 			final Map<RomDownloadState, String> downloadStatus) {
 		java.awt.EventQueue.invokeLater(new Runnable() {
 
 			@Override
 			public void run() {
 				downloadsTableModel.setRowCount(0);
 				downloadTableRowDownloadStateMap.clear();
 				int r = 0;
 				for (Map.Entry<RomDownloadState, String> ds : downloadStatus
 						.entrySet()) {
 					RomDownloadState state = ds.getKey();
 					downloadsTableModel.addRow(new String[] { "Cancel",
 							state.fileInfo.filename, ds.getValue() });
 					downloadTableRowDownloadStateMap.put(r, state);
 					r++;
 				}
 				downloadsTableModel.fireTableDataChanged();
 				downloadsTable.revalidate();
 			}
 		});
 	}
 
 	private synchronized void addChat(Long timestamp, String line) {
 		boolean atBottom = isAtBottom(chatScroll);
 
 		Calendar calendar = new GregorianCalendar();
 		if (timestamp == null) {
 			timestamp = System.currentTimeMillis();
 		}
 		calendar.setTimeInMillis(timestamp);
 		String hours = String.format("%02d", calendar.get(Calendar.HOUR));
 		String minutes = String.format("%02d", calendar.get(Calendar.MINUTE));
 		String seconds = String.format("%02d", calendar.get(Calendar.SECOND));
 		chatTextArea.append("[" + hours + ":" + minutes + ":" + seconds + "] "
 				+ line + "\n");
 
 		if (atBottom) {
 			scrollToBottom(chatTextArea);
 		}
 	}
 
 	private static boolean isAtBottom(JScrollPane scrollPane) {
 		// Is the last line of text the last line of text visible?
 		Adjustable sb = scrollPane.getVerticalScrollBar();
 
 		int val = sb.getValue();
 		int lowest = val + sb.getVisibleAmount();
 		int maxVal = sb.getMaximum();
 
 		boolean atBottom = (maxVal <= (lowest + 50));
 		return atBottom;
 	}
 
 	private static void scrollToBottom(JTextArea chatArea) {
 		chatArea.setCaretPosition(chatArea.getDocument().getLength());
 	}
 }
