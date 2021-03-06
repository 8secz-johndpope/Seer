 package emcshop.db;
 
 import java.io.IOException;
 import java.io.InputStreamReader;
 import java.sql.Connection;
 import java.sql.DriverManager;
 import java.sql.PreparedStatement;
 import java.sql.ResultSet;
 import java.sql.SQLException;
 import java.sql.Statement;
 import java.sql.Timestamp;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Collection;
 import java.util.Date;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.TreeMap;
 import java.util.logging.Level;
 import java.util.logging.Logger;
 
 import org.apache.commons.io.IOUtils;
 import org.apache.derby.jdbc.EmbeddedDriver;
 
 import emcshop.PaymentTransaction;
 import emcshop.ShopTransaction;
 import emcshop.util.ClasspathUtils;
 
 /**
  * Data access object implementation for embedded Derby database.
  * @author Michael Angstadt
  */
 public abstract class DirbyDbDao implements DbDao {
 	private static final Logger logger = Logger.getLogger(DirbyDbDao.class.getName());
 
 	/**
 	 * The current version of the database schema.
 	 */
	private static final int schemaVersion = 3;
 
 	/**
 	 * The database connection.
 	 */
 	protected Connection conn;
 
 	/**
 	 * The JDBC URL.
 	 */
 	protected String jdbcUrl;
 
 	private Map<Integer, Date[]> firstLastSeenDates = new HashMap<Integer, Date[]>();
 
 	/**
 	 * Connects to the database and creates the database from scratch if it
 	 * doesn't exist.
 	 * @param jdbcUrl the JDBC URL
 	 * @param create true to create the database schema, false not to
 	 * @param listener
 	 * @throws SQLException
 	 */
 	protected void init(String jdbcUrl, boolean create, DbListener listener) throws SQLException {
 		this.jdbcUrl = jdbcUrl;
 		logger.info("Starting database...");
 
 		//shutdown Derby when the program terminates
 		//if the Dirby database is not shutdown, then changes to it will be lost
 		Runtime.getRuntime().addShutdownHook(new Thread() {
 			@Override
 			public void run() {
 				logger.info("Shutting down the database...");
 				try {
 					close();
 				} catch (SQLException e) {
 					logger.log(Level.SEVERE, "Error stopping database.", e);
 				}
 			}
 		});
 
 		//create the connection
 		createConnection(create);
 
 		//create tables if database doesn't exist
 		if (create) {
 			if (listener != null) {
 				listener.onCreate();
 			}
 			logger.info("Database not found.  Creating the database...");
 			createSchema();
 		} else {
 			//update the database schema if it's not up to date
 			int version = selectDbVersion();
 			if (version < schemaVersion) {
 				if (listener != null) {
 					listener.onMigrate(version, schemaVersion);
 				}
 
 				logger.info("Database schema out of date.  Upgrading from version " + version + " to " + schemaVersion + ".");
 				String sql = null;
 				Statement statement = null;
 				try {
 					statement = conn.createStatement();
 					while (version < schemaVersion) {
 						logger.info("Performing schema update from version " + version + " to " + (version + 1) + ".");
 
 						String script = "migrate-" + version + "-" + (version + 1) + ".sql";
 						SQLStatementReader in = null;
 						try {
 							in = new SQLStatementReader(new InputStreamReader(ClasspathUtils.getResourceAsStream(script, getClass())));
 							while ((sql = in.readStatement()) != null) {
 								statement.execute(sql);
 							}
 							sql = null;
 						} finally {
 							IOUtils.closeQuietly(in);
 						}
 
 						version++;
 					}
 					updateDbVersion(schemaVersion);
 				} catch (IOException e) {
 					rollback();
 					throw new SQLException("Error updating database schema.", e);
 				} catch (SQLException e) {
 					rollback();
 					if (sql == null) {
 						throw e;
 					}
 					throw new SQLException("Error executing SQL statement during schema update: " + sql, e);
 				} finally {
 					if (statement != null) {
 						try {
 							statement.close();
 						} catch (SQLException e) {
 							//ignore
 						}
 					}
 				}
 				commit();
 			}
 		}
 	}
 
 	@Override
 	public int selectDbVersion() throws SQLException {
 		PreparedStatement stmt = null;
 		try {
 			stmt = conn.prepareStatement("SELECT db_schema_version FROM meta");
 			ResultSet rs = stmt.executeQuery();
 			return rs.next() ? rs.getInt("db_schema_version") : 0;
 		} finally {
 			closeStatements(stmt);
 		}
 	}
 
 	@Override
 	public void updateDbVersion(int version) throws SQLException {
 		PreparedStatement stmt = null;
 		try {
 			stmt = conn.prepareStatement("UPDATE meta SET db_schema_version = ?");
 			stmt.setInt(1, version);
 			stmt.execute();
 		} finally {
 			closeStatements(stmt);
 		}
 	}
 
 	@Override
 	public void insertDbVersion(int version) throws SQLException {
 		InsertStatement stmt = new InsertStatement("meta");
 		stmt.setInt("db_schema_version", version);
 		stmt.execute(conn);
 	}
 
 	@Override
 	public Player selsertPlayer(String name) throws SQLException {
 		PreparedStatement stmt = null;
 		String nameLowerCase = name.toLowerCase();
 
 		try {
 			stmt = conn.prepareStatement("SELECT * FROM players WHERE Lower(name) = ?");
 			stmt.setString(1, nameLowerCase);
 			ResultSet rs = stmt.executeQuery();
 			Player player = new Player();
 			if (rs.next()) {
 				player.setId(rs.getInt("id"));
 				player.setName(rs.getString("name"));
 				player.setFirstSeen(toDate(rs.getTimestamp("first_seen")));
 				player.setLastSeen(toDate(rs.getTimestamp("last_seen")));
 			} else {
 				player.setId(insertPlayer(name));
 				player.setName(name);
 			}
 			return player;
 		} finally {
 			closeStatements(stmt);
 		}
 	}
 
 	@Override
 	public int insertPlayer(String name) throws SQLException {
 		InsertStatement stmt = new InsertStatement("players");
 		stmt.setString("name", name);
 		return stmt.execute(conn);
 	}
 
 	@Override
 	public Integer getItemId(String name) throws SQLException {
 		PreparedStatement stmt = null;
 		String nameLowerCase = name.toLowerCase();
 
 		try {
 			stmt = conn.prepareStatement("SELECT id FROM items WHERE Lower(name) = ?");
 			stmt.setString(1, nameLowerCase);
 			ResultSet rs = stmt.executeQuery();
 			Integer itemId;
 			if (rs.next()) {
 				itemId = rs.getInt("id");
 			} else {
 				itemId = insertItem(name);
 			}
 			return itemId;
 		} finally {
 			closeStatements(stmt);
 		}
 	}
 
 	public List<String> getItemNames() throws SQLException {
 		PreparedStatement stmt = null;
 
 		try {
 			stmt = conn.prepareStatement("SELECT name FROM items ORDER BY Lower(name)");
 			ResultSet rs = stmt.executeQuery();
 			List<String> names = new ArrayList<String>();
 			while (rs.next()) {
 				names.add(rs.getString("name"));
 			}
 			return names;
 		} finally {
 			closeStatements(stmt);
 		}
 	}
 
 	@Override
 	public int insertItem(String name) throws SQLException {
 		InsertStatement stmt = new InsertStatement("items");
 		stmt.setString("name", name);
 		return stmt.execute(conn);
 	}
 
 	@Override
 	public void insertTransaction(ShopTransaction transaction) throws SQLException {
 		insertTransactions(Arrays.asList(transaction));
 	}
 
 	@Override
 	public void insertTransactions(Collection<ShopTransaction> transactions) throws SQLException {
 		if (transactions.isEmpty()) {
 			return;
 		}
 
 		for (ShopTransaction transaction : transactions) {
 			InsertStatement stmt = new InsertStatement("transactions");
 
 			Player player = selsertPlayer(transaction.getPlayer());
 			Integer itemId = getItemId(transaction.getItem());
 			Date ts = transaction.getTs();
 
 			//keep track of the first/last seen dates so they can be updated
			{
 				Date earliest, latest;
 				Date dates[] = firstLastSeenDates.get(player.getId());
 				if (dates == null) {
 					earliest = null;
 					latest = null;
 				} else {
 					earliest = dates[0];
 					latest = dates[1];
 				}
 
 				if (player.getFirstSeen() == null || (ts.getTime() < player.getFirstSeen().getTime())) {
 					if (earliest == null || ts.getTime() < earliest.getTime()) {
 						earliest = ts;
 					}
 				}
 
 				if (player.getLastSeen() == null || (ts.getTime() > player.getLastSeen().getTime())) {
 					if (latest == null || ts.getTime() > latest.getTime()) {
 						latest = ts;
 					}
 				}
 
 				if (earliest != null || latest != null) {
 					firstLastSeenDates.put(player.getId(), new Date[] { earliest, latest });
 				}
 			}
 
 			stmt.setTimestamp("ts", ts);
 			stmt.setInt("player", player.getId());
 			stmt.setInt("item", itemId);
 			stmt.setInt("quantity", transaction.getQuantity());
 			stmt.setInt("amount", transaction.getAmount());
 			stmt.setInt("balance", transaction.getBalance());
 			int id = stmt.execute(conn);
 			transaction.setId(id);
 		}
 	}
 
 	@Override
 	public void insertPaymentTransactions(Collection<PaymentTransaction> transactions) throws SQLException {
 		if (transactions.isEmpty()) {
 			return;
 		}
 
 		InsertStatement stmt = new InsertStatement("payment_transactions");
 		for (PaymentTransaction transaction : transactions) {
 			Player player = selsertPlayer(transaction.getPlayer());
 			Date ts = transaction.getTs();
 
 			stmt.setTimestamp("ts", ts);
 			stmt.setInt("player", player.getId());
 			stmt.setInt("amount", transaction.getAmount());
 			stmt.setInt("balance", transaction.getBalance());
 			stmt.nextRow();
 		}
 		stmt.execute(conn);
 	}
 
 	@Override
 	public List<ShopTransaction> getTransactions() throws SQLException {
 		return getTransactions(-1);
 	}
 
 	@Override
 	public ShopTransaction getLatestTransaction() throws SQLException {
 		List<ShopTransaction> transactions = getTransactions(1);
 		return transactions.isEmpty() ? null : transactions.get(0);
 	}
 
 	@Override
 	public Date getLatestTransactionDate() throws SQLException {
 		Date shopTs;
 		String sql = "SELECT Max(ts) FROM transactions";
 		PreparedStatement selectStmt = null;
 		try {
 			selectStmt = conn.prepareStatement(sql);
 			ResultSet rs = selectStmt.executeQuery();
 			shopTs = rs.next() ? toDate(rs.getTimestamp(1)) : null;
 		} finally {
 			closeStatements(selectStmt);
 		}
 
 		Date paymentTs;
 		sql = "SELECT Max(ts) FROM payment_transactions";
 		try {
 			selectStmt = conn.prepareStatement(sql);
 			ResultSet rs = selectStmt.executeQuery();
 			paymentTs = rs.next() ? toDate(rs.getTimestamp(1)) : null;
 		} finally {
 			closeStatements(selectStmt);
 		}
 
 		if (shopTs == null && paymentTs == null) {
 			return null;
 		}
 		if (shopTs != null && paymentTs == null) {
 			return shopTs;
 		}
 		if (shopTs == null && paymentTs != null) {
 			return paymentTs;
 		}
 		return (shopTs.compareTo(paymentTs) > 0) ? shopTs : paymentTs;
 	}
 
 	public List<ShopTransaction> getTransactions(int limit) throws SQLException {
 		//@formatter:off
 		String sql =
 		"SELECT t.id, t.ts, t.amount, t.balance, t.quantity, p.name AS playerName, i.name AS itemName " +
 		"FROM transactions t " +
 		"INNER JOIN players p ON t.player = p.id " +
 		"INNER JOIN items i ON t.item = i.id " +
 		"ORDER BY t.ts DESC";
 		//@formatter:on
 
 		PreparedStatement selectStmt = null;
 		try {
 			selectStmt = conn.prepareStatement(sql);
 			if (limit > 0) {
 				selectStmt.setMaxRows(limit);
 			}
 			ResultSet rs = selectStmt.executeQuery();
 			List<ShopTransaction> transactions = new ArrayList<ShopTransaction>();
 			while (rs.next()) {
 				ShopTransaction transaction = new ShopTransaction();
 				transaction.setId(rs.getInt("id"));
 				transaction.setAmount(rs.getInt("amount"));
 				transaction.setBalance(rs.getInt("balance"));
 				transaction.setItem(rs.getString("itemName"));
 				transaction.setPlayer(rs.getString("playerName"));
 				transaction.setQuantity(rs.getInt("quantity"));
 				transaction.setTs(toDate(rs.getTimestamp("ts")));
 
 				transactions.add(transaction);
 			}
 			return transactions;
 		} finally {
 			closeStatements(selectStmt);
 		}
 	}
 
 	@Override
 	public List<PaymentTransaction> getPendingPaymentTransactions() throws SQLException {
 		//@formatter:off
 		String sql =
 		"SELECT pt.id, pt.ts, pt.amount, pt.balance, p.name AS playerName " +
 		"FROM payment_transactions pt " +
 		"INNER JOIN players p ON pt.player = p.id " +
 		"WHERE pt.\"transaction\" IS NULL " +
 		"AND pt.ignore = false " +
 		"ORDER BY pt.ts DESC ";
 		//@formatter:on
 
 		PreparedStatement selectStmt = null;
 		try {
 			selectStmt = conn.prepareStatement(sql);
 			ResultSet rs = selectStmt.executeQuery();
 			List<PaymentTransaction> transactions = new ArrayList<PaymentTransaction>();
 			while (rs.next()) {
 				PaymentTransaction transaction = new PaymentTransaction();
 				transaction.setId(rs.getInt("id"));
 				transaction.setAmount(rs.getInt("amount"));
 				transaction.setBalance(rs.getInt("balance"));
 				transaction.setPlayer(rs.getString("playerName"));
 				transaction.setTs(toDate(rs.getTimestamp("ts")));
 
 				transactions.add(transaction);
 			}
 			return transactions;
 		} finally {
 			closeStatements(selectStmt);
 		}
 	}
 
 	@Override
 	public int countPendingPaymentTransactions() throws SQLException {
 		//@formatter:off
 		String sql =
 		"SELECT Count(*) " +
 		"FROM payment_transactions " +
 		"WHERE \"transaction\" IS NULL " +
 		"AND ignore = false";
 		//@formatter:on
 
 		PreparedStatement selectStmt = null;
 		try {
 			selectStmt = conn.prepareStatement(sql);
 			ResultSet rs = selectStmt.executeQuery();
 			return rs.next() ? rs.getInt(1) : 0;
 		} finally {
 			closeStatements(selectStmt);
 		}
 	}
 
 	@Override
 	public void ignorePaymentTransaction(Integer id) throws SQLException {
 		PreparedStatement stmt = null;
 		try {
 			stmt = conn.prepareStatement("UPDATE payment_transactions SET ignore = ? WHERE id = ?");
 			stmt.setBoolean(1, true);
 			stmt.setInt(2, id);
 			stmt.execute();
 		} finally {
 			closeStatements(stmt);
 		}
 	}
 
 	@Override
 	public void assignPaymentTransaction(Integer paymentId, Integer transactionId) throws SQLException {
 		PreparedStatement stmt = null;
 		try {
 			stmt = conn.prepareStatement("UPDATE payment_transactions SET \"transaction\" = ? WHERE id = ?");
 			stmt.setInt(1, transactionId);
 			stmt.setInt(2, paymentId);
 			stmt.execute();
 		} finally {
 			closeStatements(stmt);
 		}
 	}
 
 	@Override
 	public Map<String, ItemGroup> getItemGroups() throws SQLException {
 		return getItemGroups(null, null);
 	}
 
 	@Override
 	public Map<String, ItemGroup> getItemGroups(Date from, Date to) throws SQLException {
 		PreparedStatement stmt = null;
 		Map<String, ItemGroup> itemGroups = new TreeMap<String, ItemGroup>();
 
 		try {
 			//@formatter:off
 			String sql =
 			"SELECT Sum(t.amount) AS amountSum, Sum(t.quantity) AS quantitySum, i.name AS itemName " + 
 			"FROM transactions t INNER JOIN items i ON t.item = i.id " + 
 			"WHERE t.amount > 0 ";
 			if (from != null) {
 				sql += "AND ts >= ? ";
 			}
 			if (to != null) {
 				sql += "AND ts < ? ";
 			}
 			sql += "GROUP BY i.name";
 			//@formatter:on
 
 			stmt = conn.prepareStatement(sql);
 			int index = 1;
 			if (from != null) {
 				stmt.setTimestamp(index++, toTimestamp(from));
 			}
 			if (to != null) {
 				stmt.setTimestamp(index++, toTimestamp(to));
 			}
 			ResultSet rs = stmt.executeQuery();
 			while (rs.next()) {
 				ItemGroup itemGroup = new ItemGroup();
 				itemGroup.setItem(rs.getString("itemName"));
 				itemGroup.setSoldAmount(rs.getInt("amountSum"));
 				itemGroup.setSoldQuantity(rs.getInt("quantitySum"));
 
 				itemGroups.put(itemGroup.getItem(), itemGroup);
 			}
 		} finally {
 			closeStatements(stmt);
 		}
 
 		try {
 			//@formatter:off
 			String sql =
 			"SELECT Sum(t.amount) AS amountSum, Sum(t.quantity) AS quantitySum, i.name AS itemName " + 
 			"FROM transactions t INNER JOIN items i ON t.item = i.id " + 
 			"WHERE t.amount < 0 ";
 			if (from != null) {
 				sql += "AND ts >= ? ";
 			}
 			if (to != null) {
 				sql += "AND ts < ? ";
 			}
 			sql += "GROUP BY i.name";
 			//@formatter:on
 
 			stmt = conn.prepareStatement(sql);
 			int index = 1;
 			if (from != null) {
 				stmt.setTimestamp(index++, toTimestamp(from));
 			}
 			if (to != null) {
 				stmt.setTimestamp(index++, toTimestamp(to));
 			}
 			ResultSet rs = stmt.executeQuery();
 			while (rs.next()) {
 				String itemName = rs.getString("itemName");
 				ItemGroup itemGroup = itemGroups.get(itemName);
 				if (itemGroup == null) {
 					itemGroup = new ItemGroup();
 					itemGroup.setItem(itemName);
 					itemGroups.put(itemGroup.getItem(), itemGroup);
 				}
 				itemGroup.setBoughtAmount(rs.getInt("amountSum"));
 				itemGroup.setBoughtQuantity(rs.getInt("quantitySum"));
 			}
 		} finally {
 			closeStatements(stmt);
 		}
 
 		return itemGroups;
 	}
 
 	@Override
 	public Map<String, PlayerGroup> getPlayerGroups(Date from, Date to) throws SQLException {
 		Map<String, PlayerGroup> playerGroups = new HashMap<String, PlayerGroup>();
 		PreparedStatement stmt = null;
 
 		try {
 			//@formatter:off
 			String sql =
 			"SELECT t.amount, t.quantity, t.player, p.name AS playerName, p.first_seen, p.last_seen, i.name AS itemName " + 
 			"FROM transactions t " +
 			"INNER JOIN items i ON t.item = i.id " +
 			"INNER JOIN players p ON t.player = p.id ";
 			if (from != null) {
 				sql += "WHERE t.ts >= ? ";
 			}
 			if (to != null) {
 				sql += ((from == null) ? "WHERE" : "AND") + " t.ts < ? ";
 			}
 			//@formatter:on
 
 			stmt = conn.prepareStatement(sql);
 			int index = 1;
 			if (from != null) {
 				stmt.setTimestamp(index++, toTimestamp(from));
 			}
 			if (to != null) {
 				stmt.setTimestamp(index++, toTimestamp(to));
 			}
 			ResultSet rs = stmt.executeQuery();
 			while (rs.next()) {
 				String playerName = rs.getString("playerName");
 				PlayerGroup playerGroup = playerGroups.get(playerName);
 				if (playerGroup == null) {
 					playerGroup = new PlayerGroup();
 
 					Player player = new Player();
 					player.setId(rs.getInt("player"));
 					player.setName(playerName);
 					player.setFirstSeen(toDate(rs.getTimestamp("first_seen")));
 					player.setLastSeen(toDate(rs.getTimestamp("last_seen")));
 					playerGroup.setPlayer(player);
 
 					playerGroups.put(playerName, playerGroup);
 				}
 
 				String itemName = rs.getString("itemName");
 				ItemGroup itemGroup = playerGroup.getItems().get(itemName);
 				if (itemGroup == null) {
 					itemGroup = new ItemGroup();
 					itemGroup.setItem(itemName);
 					playerGroup.getItems().put(itemName, itemGroup);
 				}
 
 				int amount = rs.getInt("amount");
 				if (amount < 0) {
 					itemGroup.setBoughtAmount(itemGroup.getBoughtAmount() + amount);
 				} else {
 					itemGroup.setSoldAmount(itemGroup.getSoldAmount() + amount);
 				}
 
 				int quantity = rs.getInt("quantity");
 				if (quantity < 0) {
 					itemGroup.setSoldQuantity(itemGroup.getSoldQuantity() + quantity);
 				} else {
 					itemGroup.setBoughtQuantity(itemGroup.getBoughtQuantity() + quantity);
 				}
 			}
 		} finally {
 			closeStatements(stmt);
 		}
 
 		//calculate the first and last times the player was seen, if not already calculated
 		for (PlayerGroup playerGroup : playerGroups.values()) {
 			Player player = playerGroup.getPlayer();
 			if (player.getFirstSeen() == null || player.getLastSeen() == null) {
 				calculateFirstLastSeen(player);
 			}
 		}
 		commit();
 
 		return playerGroups;
 	}
 
 	@Override
 	public String getPlayerName(int id) throws SQLException {
 		PreparedStatement stmt = null;
 
 		try {
 			stmt = conn.prepareStatement("SELECT name FROM players WHERE id = ?");
 			stmt.setInt(1, id);
 			ResultSet rs = stmt.executeQuery();
 			return rs.next() ? rs.getString("name") : null;
 		} finally {
 			closeStatements(stmt);
 		}
 	}
 
 	@Override
 	public void wipe() throws SQLException, IOException {
 		logger.info("Wiping transactions...");
 		conn.createStatement().execute("DELETE FROM payment_transactions");
 		conn.createStatement().execute("DELETE FROM transactions");
 		conn.createStatement().execute("DELETE FROM players");
 		conn.createStatement().execute("DELETE FROM items");
 		conn.createStatement().execute("ALTER TABLE transactions ALTER COLUMN id RESTART WITH 1");
 		conn.createStatement().execute("ALTER TABLE players ALTER COLUMN id RESTART WITH 1");
 		conn.createStatement().execute("ALTER TABLE items ALTER COLUMN id RESTART WITH 1");
 		commit();
 	}
 
 	@Override
 	public void commit() throws SQLException {
 		//update first/last seen dates if transactions were inserted
 		for (Map.Entry<Integer, Date[]> entry : firstLastSeenDates.entrySet()) {
 			Integer playerId = entry.getKey();
 			Date firstSeen = entry.getValue()[0];
 			Date lastSeen = entry.getValue()[1];
 
 			updateFirstLastSeen(playerId, firstSeen, lastSeen);
 		}
 
 		conn.commit();
 
 		firstLastSeenDates.clear();
 	}
 
 	@Override
 	public void rollback() {
 		firstLastSeenDates.clear();
 
 		try {
 			conn.rollback();
 		} catch (SQLException e) {
 			//exception is caught here instead of being thrown because ever time I catch the exception for rollback(), I just log the exception and continue on
 			//plus, an exception is unlikely to be thrown here anyway
 			logger.log(Level.WARNING, "Problem rolling back transaction.", e);
 		}
 	}
 
 	@Override
 	public void close() throws SQLException {
 		try {
 			logger.info("Closing database.");
 			DriverManager.getConnection("jdbc:derby:;shutdown=true");
 		} catch (SQLException se) {
 			if (se.getErrorCode() == 50000 && "XJ015".equals(se.getSQLState())) {
 				// we got the expected exception
 			} else if (se.getErrorCode() == 45000 && "08006".equals(se.getSQLState())) {
 				// we got the expected exception for single database shutdown
 			} else {
 				// if the error code or SQLState is different, we have
 				// an unexpected exception (shutdown failed)
 				throw se;
 			}
 		}
 	}
 
 	/**
 	 * Calculates a player's first/last seen dates and updates them.
 	 * @param player the player
 	 * @throws SQLException
 	 */
 	private void calculateFirstLastSeen(Player player) throws SQLException {
 		PreparedStatement stmt = null;
 		try {
 			//@formatter:off
 			String sql =
 			"SELECT Min(ts) AS firstSeen, Max(ts) AS lastSeen " + 
 			"FROM transactions " +
 			"WHERE player = ? ";
 			//@formatter:on
 			stmt = conn.prepareStatement(sql);
 
 			stmt.setInt(1, player.getId());
 
 			ResultSet rs = stmt.executeQuery();
 			if (rs.next()) {
 				Date firstSeen = toDate(rs.getTimestamp("firstSeen"));
 				Date lastSeen = toDate(rs.getTimestamp("lastSeen"));
 
 				player.setFirstSeen(firstSeen);
 				player.setLastSeen(lastSeen);
 				updateFirstLastSeen(player.getId(), firstSeen, lastSeen);
 			}
 		} finally {
 			closeStatements(stmt);
 		}
 	}
 
 	/**
 	 * Updates a player's first/last seen dates.
 	 * @param playerId the player ID
 	 * @param firstSeen the "first seen" date
 	 * @param lastSeen the "last seen" date
 	 * @throws SQLException
 	 */
 	private void updateFirstLastSeen(Integer playerId, Date firstSeen, Date lastSeen) throws SQLException {
 		PreparedStatement stmt = null;
 		try {
 			String sql = "UPDATE players SET ";
 			if (firstSeen != null) {
 				sql += "first_seen = ? ";
 			}
 			if (lastSeen != null) {
 				if (firstSeen != null) {
 					sql += ", ";
 				}
 				sql += "last_seen = ? ";
 			}
 			sql += "WHERE id = ?";
 			stmt = conn.prepareStatement(sql);
 
 			int index = 1;
 			if (firstSeen != null) {
 				stmt.setTimestamp(index++, toTimestamp(firstSeen));
 			}
 			if (lastSeen != null) {
 				stmt.setTimestamp(index++, toTimestamp(lastSeen));
 			}
 			stmt.setInt(index++, playerId);
 
 			stmt.execute();
 		} finally {
 			closeStatements(stmt);
 		}
 	}
 
 	/**
 	 * Converts a {@link Timestamp} to a {@link Date}
 	 * @param timestamp the timestamp
 	 * @return the date
 	 */
 	protected Date toDate(Timestamp timestamp) {
 		return (timestamp == null) ? null : new Date(timestamp.getTime());
 	}
 
 	/**
 	 * Converts a {@link Date} to a {@link Timestamp}.
 	 * @param date the date
 	 * @return the timestamp
 	 */
 	protected Timestamp toTimestamp(Date date) {
 		return (date == null) ? null : new Timestamp(date.getTime());
 	}
 
 	/**
 	 * Closes {@link Statement} objects.
 	 * @param statements the statements to close (nulls are ignored)
 	 */
 	protected void closeStatements(Statement... statements) {
 		for (Statement statement : statements) {
 			if (statement != null) {
 				try {
 					statement.close();
 				} catch (SQLException e) {
 					//ignore
 				}
 			}
 		}
 	}
 
 	/**
 	 * Creates the database connection.
 	 * @param createDb true to create the DB, false to connect to an existing
 	 * one
 	 * @throws SQLException
 	 */
 	protected void createConnection(boolean createDb) throws SQLException {
 		//load the driver
 		try {
 			Class.forName(EmbeddedDriver.class.getName()).newInstance();
 		} catch (Exception e) {
 			throw new SQLException("Database driver not on classpath.", e);
 		}
 
 		//create the connection
 		String jdbcUrl = this.jdbcUrl;
 		if (createDb) {
 			jdbcUrl += ";create=true";
 		}
 		conn = DriverManager.getConnection(jdbcUrl);
 		conn.setAutoCommit(false); // default is true
 	}
 
 	/**
 	 * Creates the database schema.
 	 * @throws SQLException
 	 */
 	protected void createSchema() throws SQLException {
 		String sql = null;
 		SQLStatementReader in = null;
 		String schemaFileName = "schema.sql";
 		Statement statement = null;
 		try {
 			in = new SQLStatementReader(new InputStreamReader(ClasspathUtils.getResourceAsStream(schemaFileName, getClass())));
 			statement = conn.createStatement();
 			while ((sql = in.readStatement()) != null) {
 				statement.execute(sql);
 			}
 			sql = null;
 			insertDbVersion(schemaVersion);
 		} catch (IOException e) {
 			throw new SQLException("Error creating database.", e);
 		} catch (SQLException e) {
 			if (sql == null) {
 				throw e;
 			}
 			throw new SQLException("Error executing SQL statement: " + sql, e);
 		} finally {
 			IOUtils.closeQuietly(in);
 			closeStatements(statement);
 		}
 		commit();
 	}
 }
