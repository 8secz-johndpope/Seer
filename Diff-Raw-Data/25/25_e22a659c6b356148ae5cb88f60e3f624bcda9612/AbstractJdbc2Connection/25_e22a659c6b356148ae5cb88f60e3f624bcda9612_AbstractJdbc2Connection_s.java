 package org.postgresql.jdbc2;
 
 import java.io.IOException;
 import java.io.PrintWriter;
 import java.sql.*;
 import java.util.*;
 import org.postgresql.core.*;
 import org.postgresql.Driver;
 import org.postgresql.PGNotification;
 import org.postgresql.fastpath.Fastpath;
 import org.postgresql.largeobject.LargeObjectManager;
 import org.postgresql.util.PSQLState;
 import org.postgresql.util.PGobject;
 import org.postgresql.util.PSQLException;
 import org.postgresql.util.GT;
 
 
 /* $PostgreSQL: /cvsroot/pgsql-server/src/interfaces/jdbc/org/postgresql/jdbc2/AbstractJdbc2Connection.java,v 1.6 2003/06/30 21:10:55 davec Exp $
  * This class defines methods of the jdbc2 specification.
  * The real Connection class (for jdbc2) is org.postgresql.jdbc2.Jdbc2Connection
  */
 public abstract class AbstractJdbc2Connection implements BaseConnection
 {
 	//
 	// Data initialized on construction:
 	//
 
 	/* URL we were created via */
 	private final String creatingURL;
 
 	/* Actual network handler */
 	private final ProtocolConnection protoConnection;
 	/* Compatibility version */
 	private final String compatible;
 	/* Actual server version */
 	private final String dbVersionNumber;
 	
 	/* Query that runs COMMIT */
 	private final Query commitQuery;	
 	/* Query that runs ROLLBACK */
 	private final Query rollbackQuery;
 
 	// These are used to cache the oids to PGType mappings.
 	private Hashtable oidTypeCache = new Hashtable();   // oid -> PGType
 	private Hashtable typeOidCache = new Hashtable();  // PGType -> oid
 
 	// Default statement prepare threshold.
 	protected int prepareThreshold;
 	// Connection's autocommit state.
 	public boolean autoCommit = true;
 	// Connection's readonly state.
 	public boolean readOnly = false;
 
 	// Current warnings; there might be more on protoConnection too.
 	public SQLWarning firstWarning = null;
 
 	public abstract DatabaseMetaData getMetaData() throws SQLException;
 
 	//
 	// Ctor.
 	//
 	protected AbstractJdbc2Connection(String host, int port, String user, String database, Properties info, String url) throws SQLException
 	{
 		this.creatingURL = url;
 
 		//Read loglevel arg and set the loglevel based on this value
 		//in addition to setting the log level enable output to
 		//standard out if no other printwriter is set
 
 		// XXX revisit: need a debug level *per connection*.
 		
 		int logLevel = 0;
 		try
 		{
 			logLevel = Integer.parseInt(info.getProperty("loglevel", "0"));
 			if (logLevel > Driver.DEBUG || logLevel < Driver.INFO)
 			{
 				logLevel = 0;
 			}
 		}
 		catch (Exception l_e)
 		{
 			// XXX revisit
 			// invalid value for loglevel; ignore it
 		}
 
 		if (logLevel > 0)
 		{
 			Driver.setLogLevel(logLevel);
 			enableDriverManagerLogging();
 		}
 
 		prepareThreshold = 0;
 		try {
 			prepareThreshold = Integer.parseInt(info.getProperty("prepareThreshold", "0"));
 		} catch (Exception e) {}
 		
 		if (prepareThreshold < 0)
 			prepareThreshold = 0;
 		
 		//Print out the driver version number
 		if (Driver.logInfo)
 			Driver.info(Driver.getVersion());
 
 		// Now make the initial connection and set up local state
 		this.protoConnection = ConnectionFactory.openConnection(host, port, user, database, info);
 		this.dbVersionNumber = protoConnection.getServerVersion();
 		this.compatible = info.getProperty("compatible", Driver.MAJORVERSION + "." + Driver.MINORVERSION);
 
 		if (Driver.logDebug) {
 			Driver.debug("    compatible = " + compatible);
 			Driver.debug("    loglevel = " + logLevel);
 			Driver.debug("    prepare threshold = " + prepareThreshold);
 		}
 
 		// Initialize common queries.
 		commitQuery = getQueryExecutor().createSimpleQuery("COMMIT");
 		rollbackQuery = getQueryExecutor().createSimpleQuery("ROLLBACK");
 
 		// Initialize object handling
 		initObjectTypes();
 	}
 
 	/*
 	 * The current type mappings
 	 */
 	protected java.util.Map typemap;
  
 	public java.sql.Statement createStatement() throws SQLException
 	{
 		// We now follow the spec and default to TYPE_FORWARD_ONLY.
 		return createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY, java.sql.ResultSet.CONCUR_READ_ONLY);
 	}
 
 	public abstract java.sql.Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException;
 
 	public java.sql.PreparedStatement prepareStatement(String sql) throws SQLException
 	{
 		return prepareStatement(sql, java.sql.ResultSet.TYPE_FORWARD_ONLY, java.sql.ResultSet.CONCUR_READ_ONLY);
 	}
 
 	public abstract java.sql.PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException;
 
 	public java.sql.CallableStatement prepareCall(String sql) throws SQLException
 	{
 		return prepareCall(sql, java.sql.ResultSet.TYPE_FORWARD_ONLY, java.sql.ResultSet.CONCUR_READ_ONLY);
 	}
 
 	public abstract java.sql.CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException;
 
 	public java.util.Map getTypeMap() throws SQLException
 	{
 		return typemap;
 	}
 
 	// Query executor associated with this connection.
 	public QueryExecutor getQueryExecutor() { 
 		return protoConnection.getQueryExecutor();
 	}
 
 	/*
 	 * This adds a warning to the warning chain.
 	 * @param warn warning to add
 	 */
 	public void addWarning(SQLWarning warn)
 	{
 		// Add the warning to the chain
 		if (firstWarning != null)
 			firstWarning.setNextWarning(warn);
 		else
 			firstWarning = warn;
 
 	}
 
 	/** 
 	 * Simple query execution.
 	 */
 	public ResultSet execSQLQuery(String s) throws SQLException {
 		BaseStatement stat = (BaseStatement) createStatement();
 		boolean hasResultSet = stat.executeWithFlags(s, QueryExecutor.QUERY_SUPPRESS_BEGIN);
 
 		while (!hasResultSet && stat.getUpdateCount() != -1)
 			hasResultSet = stat.getMoreResults();
 
 		if (!hasResultSet)
			throw new PSQLException(GT.tr("No results where returned by the query."), PSQLState.NO_DATA);
 
 		// Transfer warnings to the connection, since the user never
 		// has a chance to see the statement itself.
 		SQLWarning warnings = stat.getWarnings();
 		if (warnings != null)
 			addWarning(warnings);
 
 		return stat.getResultSet();
 	}
 
     public void execSQLUpdate(String s) throws SQLException {
 		BaseStatement stmt = (BaseStatement) createStatement();
 		if (stmt.executeWithFlags(s, QueryExecutor.QUERY_NO_METADATA | QueryExecutor.QUERY_NO_RESULTS | QueryExecutor.QUERY_SUPPRESS_BEGIN))
 			throw new PSQLException(GT.tr("A result was returned when none was expected."));
 
 		// Transfer warnings to the connection, since the user never
 		// has a chance to see the statement itself.
 		SQLWarning warnings = stmt.getWarnings();
 		if (warnings != null)
 			addWarning(warnings);
 
 		stmt.close();
 	}
 
 	/*
 	 * In SQL, a result table can be retrieved through a cursor that
 	 * is named.  The current row of a result can be updated or deleted
 	 * using a positioned update/delete statement that references the
 	 * cursor name.
 	 *
 	 * We do not support positioned update/delete, so this is a no-op.
 	 *
 	 * @param cursor the cursor name
 	 * @exception SQLException if a database access error occurs
 	 */
 	public void setCursorName(String cursor) throws SQLException
 	{
 		// No-op.
 	}
 
 	/*
 	 * getCursorName gets the cursor name.
 	 *
 	 * @return the current cursor name
 	 * @exception SQLException if a database access error occurs
 	 */
 	public String getCursorName() throws SQLException
 	{
 		return null;
 	}
 
 	/*
 	 * We are required to bring back certain information by
 	 * the DatabaseMetaData class.	These functions do that.
 	 *
 	 * Method getURL() brings back the URL (good job we saved it)
 	 *
 	 * @return the url
 	 * @exception SQLException just in case...
 	 */
 	public String getURL() throws SQLException
 	{
 		return creatingURL;
 	}
 
 	/*
 	 * Method getUserName() brings back the User Name (again, we
 	 * saved it)
 	 *
 	 * @return the user name
 	 * @exception SQLException just in case...
 	 */
 	public String getUserName() throws SQLException
 	{
 		return protoConnection.getUser();
 	}
 
 	/*
 	 * This returns the Fastpath API for the current connection.
 	 *
 	 * <p><b>NOTE:</b> This is not part of JDBC, but allows access to
 	 * functions on the org.postgresql backend itself.
 	 *
 	 * <p>It is primarily used by the LargeObject API
 	 *
 	 * <p>The best way to use this is as follows:
 	 *
 	 * <p><pre>
 	 * import org.postgresql.fastpath.*;
 	 * ...
 	 * Fastpath fp = ((org.postgresql.Connection)myconn).getFastpathAPI();
 	 * </pre>
 	 *
 	 * <p>where myconn is an open Connection to org.postgresql.
 	 *
 	 * @return Fastpath object allowing access to functions on the org.postgresql
 	 * backend.
 	 * @exception SQLException by Fastpath when initialising for first time
 	 */
 	public Fastpath getFastpathAPI() throws SQLException
 	{
 		if (fastpath == null)
 			fastpath = new Fastpath(this);
 		return fastpath;
 	}
 
 	// This holds a reference to the Fastpath API if already open
 	private Fastpath fastpath = null;
 
 	/*
 	 * This returns the LargeObject API for the current connection.
 	 *
 	 * <p><b>NOTE:</b> This is not part of JDBC, but allows access to
 	 * functions on the org.postgresql backend itself.
 	 *
 	 * <p>The best way to use this is as follows:
 	 *
 	 * <p><pre>
 	 * import org.postgresql.largeobject.*;
 	 * ...
 	 * LargeObjectManager lo = ((org.postgresql.Connection)myconn).getLargeObjectAPI();
 	 * </pre>
 	 *
 	 * <p>where myconn is an open Connection to org.postgresql.
 	 *
 	 * @return LargeObject object that implements the API
 	 * @exception SQLException by LargeObject when initialising for first time
 	 */
 	public LargeObjectManager getLargeObjectAPI() throws SQLException
 	{
 		if (largeobject == null)
 			largeobject = new LargeObjectManager(this);
 		return largeobject;
 	}
 
 	// This holds a reference to the LargeObject API if already open
 	private LargeObjectManager largeobject = null;
 
 	/*
 	 * This method is used internally to return an object based around
 	 * org.postgresql's more unique data types.
 	 *
 	 * <p>It uses an internal Hashtable to get the handling class. If the
 	 * type is not supported, then an instance of org.postgresql.util.PGobject
 	 * is returned.
 	 *
 	 * You can use the getValue() or setValue() methods to handle the returned
 	 * object. Custom objects can have their own methods.
 	 *
 	 * @return PGobject for this type, and set to value
 	 * @exception SQLException if value is not correct for this type
 	 */
 	public Object getObject(String type, String value) throws SQLException
 	{
 		if (typemap != null)
 		{
 			SQLData d = (SQLData) typemap.get(type);
 			if (d != null)
 			{
 				// Handle the type (requires SQLInput & SQLOutput classes to be implemented)
 				throw org.postgresql.Driver.notImplemented();
 			}
 		}
 
 		PGobject obj = null;
 
 		if (Driver.logDebug)
 			Driver.debug("Constructing object from type=" + type + " value=<" + value + ">");
 
 		try
         {
 			Class klass;
 
 			synchronized (objectTypes) {
 				klass = (Class)objectTypes.get(type);
 			}
 
 			// If className is not null, then try to instantiate it,
 			// It must be basetype PGobject
 
 			// This is used to implement the org.postgresql unique types (like lseg,
 			// point, etc).
 
 			if (klass != null)
 			{
                 obj = (PGobject) (klass.newInstance());
                 obj.setType(type);
                 obj.setValue(value);
 			}
             else
             {
                 // If className is null, then the type is unknown.
                 // so return a PGobject with the type set, and the value set
                obj = new PGobject();
                obj.setType( type );
                obj.setValue( value );
             }
 
             return obj;
 		}
 		catch (SQLException sx)
 		{
 			// rethrow the exception. Done because we capture any others next
 			throw sx;
 		}
 		catch (Exception ex)
 		{
 			throw new PSQLException(GT.tr("Failed to create object for: {0}.",type), PSQLState.CONNECTION_FAILURE, ex);
 		}
 	}
  
 	public void addDataType(String type, String name)
 	{
 		try {
 			addDataType(type, Class.forName(name));
 		} catch (Exception e) {
 			throw new RuntimeException("Cannot register new type: " + e);
 		}
 	}
 
 	public void addDataType(String type, Class klass) throws SQLException
 	{
 		if (!org.postgresql.util.PGobject.class.isAssignableFrom(klass))
 			throw new PSQLException(GT.tr("The class {0} does not implement org.postgresql.util.PGobject.", klass.toString()), PSQLState.INVALID_PARAMETER_TYPE);
 		
 		synchronized (objectTypes) {
 			objectTypes.put(type, klass);
 		}
 	}
 
 	// This holds the available types, a String to Class mapping.
 	private final HashMap objectTypes = new HashMap();
 
 	// This array contains the types that are supported as standard.
 	//
 	// The first entry is the types name on the database, the second
 	// the full class name of the handling class.
 	//
 	private static final Object[][] defaultObjectTypes = {
 		{ "box",      org.postgresql.geometric.PGbox.class     },
 		{ "circle",   org.postgresql.geometric.PGcircle.class  },
 		{ "line",     org.postgresql.geometric.PGline.class    },
 		{ "lseg",     org.postgresql.geometric.PGlseg.class    },
 		{ "path",     org.postgresql.geometric.PGpath.class    },
 		{ "point",    org.postgresql.geometric.PGpoint.class   },
 		{ "polygon",  org.postgresql.geometric.PGpolygon.class },
 		{ "money",    org.postgresql.util.PGmoney.class        },
 		{ "interval", org.postgresql.util.PGInterval.class     }
 	};
 
 	// This initialises the objectTypes hashtable
 	private void initObjectTypes() throws SQLException
 	{
 		for (int i = 0; i < defaultObjectTypes.length; ++i)
 			addDataType((String) defaultObjectTypes[i][0], (Class) defaultObjectTypes[i][1]);
 	}
 
 	/**
 	 * In some cases, it is desirable to immediately release a Connection's
 	 * database and JDBC resources instead of waiting for them to be
 	 * automatically released.
 	 *
 	 * <B>Note:</B> A Connection is automatically closed when it is
 	 * garbage collected.  Certain fatal errors also result in a closed
 	 * connection.
 	 *
 	 * @exception SQLException if a database access error occurs
 	 */
 	public void close()
 	{
 		protoConnection.close();
 	}
 
 	/*
 	 * A driver may convert the JDBC sql grammar into its system's
 	 * native SQL grammar prior to sending it; nativeSQL returns the
 	 * native form of the statement that the driver would have sent.
 	 *
 	 * @param sql a SQL statement that may contain one or more '?'
 	 *	parameter placeholders
 	 * @return the native form of this statement
 	 * @exception SQLException if a database access error occurs
 	 */
 	public String nativeSQL(String sql) throws SQLException
 	{
 		return sql;
 	}
 
 	/*
 	 * The first warning reported by calls on this Connection is
 	 * returned.
 	 *
 	 * <B>Note:</B> Sebsequent warnings will be changed to this
 	 * SQLWarning
 	 *
 	 * @return the first SQLWarning or null
 	 * @exception SQLException if a database access error occurs
 	 */
 	public synchronized SQLWarning getWarnings() throws SQLException
 	{
 		SQLWarning newWarnings = protoConnection.getWarnings(); // NB: also clears them.
 		if (firstWarning == null)
 			firstWarning = newWarnings;
 		else
 			firstWarning.setNextWarning(newWarnings); // Chain them on.
 
 		return firstWarning;
 	}
 
 	/*
 	 * After this call, getWarnings returns null until a new warning
 	 * is reported for this connection.
 	 *
 	 * @exception SQLException if a database access error occurs
 	 */
 	public synchronized void clearWarnings() throws SQLException
 	{
 		protoConnection.getWarnings(); // Clear and discard.
 		firstWarning = null;
 	}
 
 
 	/*
 	 * You can put a connection in read-only mode as a hunt to enable
 	 * database optimizations
 	 *
 	 * <B>Note:</B> setReadOnly cannot be called while in the middle
 	 * of a transaction
 	 *
 	 * @param readOnly - true enables read-only mode; false disables it
 	 * @exception SQLException if a database access error occurs
 	 */
 	public void setReadOnly(boolean readOnly) throws SQLException
 	{
 		if (protoConnection.getTransactionState() != ProtocolConnection.TRANSACTION_IDLE)
 			throw new PSQLException(GT.tr("Cannot change transaction read-only property in the middle of a transaction."));
 
 		if (haveMinimumServerVersion("7.4") && readOnly != this.readOnly) {
 			String readOnlySql = "SET SESSION CHARACTERISTICS AS TRANSACTION " + (readOnly ? "READ ONLY" : "READ WRITE");
 			execSQLUpdate(readOnlySql); // nb: no BEGIN triggered.
 		}
 
 		this.readOnly = readOnly;
 	}
 
 	/*
 	 * Tests to see if the connection is in Read Only Mode.
 	 *
 	 * @return true if the connection is read only
 	 * @exception SQLException if a database access error occurs
 	 */
 	public boolean isReadOnly() throws SQLException
 	{
 		return readOnly;
 	}
 
 	/*
 	 * If a connection is in auto-commit mode, than all its SQL
 	 * statements will be executed and committed as individual
 	 * transactions.  Otherwise, its SQL statements are grouped
 	 * into transactions that are terminated by either commit()
 	 * or rollback().  By default, new connections are in auto-
 	 * commit mode.  The commit occurs when the statement completes
 	 * or the next execute occurs, whichever comes first.  In the
 	 * case of statements returning a ResultSet, the statement
 	 * completes when the last row of the ResultSet has been retrieved
 	 * or the ResultSet has been closed.  In advanced cases, a single
 	 * statement may return multiple results as well as output parameter
 	 * values.	Here the commit occurs when all results and output param
 	 * values have been retrieved.
 	 *
 	 * @param autoCommit - true enables auto-commit; false disables it
 	 * @exception SQLException if a database access error occurs
 	 */
 	public void setAutoCommit(boolean autoCommit) throws SQLException
 	{
 		if (this.autoCommit == autoCommit)
 			return;
 
 		if (!this.autoCommit)
 			commit();
 
 		this.autoCommit = autoCommit;
 	}
 
 	/*
 	 * gets the current auto-commit state
 	 *
 	 * @return Current state of the auto-commit mode
 	 * @see setAutoCommit
 	 */
 	public boolean getAutoCommit()
 	{
 		return this.autoCommit;
 	}
 
 	private void executeTransactionCommand(Query query) throws SQLException {
 		getQueryExecutor().execute(query, null, new TransactionCommandHandler(),
 								   0, 0, QueryExecutor.QUERY_NO_METADATA | QueryExecutor.QUERY_NO_RESULTS | QueryExecutor.QUERY_SUPPRESS_BEGIN);
 	}
 
 	/*
 	 * The method commit() makes all changes made since the previous
 	 * commit/rollback permanent and releases any database locks currently
 	 * held by the Connection.	This method should only be used when
 	 * auto-commit has been disabled.  (If autoCommit == true, then we
 	 * just return anyhow)
 	 *
 	 * @exception SQLException if a database access error occurs
 	 * @see setAutoCommit
 	 */
 	public void commit() throws SQLException
 	{
 		if (autoCommit)
 			return;
 
 		if (protoConnection.getTransactionState() != ProtocolConnection.TRANSACTION_IDLE)
 			executeTransactionCommand(commitQuery);
 	}
 
 	/*
 	 * The method rollback() drops all changes made since the previous
 	 * commit/rollback and releases any database locks currently held by
 	 * the Connection.
 	 *
 	 * @exception SQLException if a database access error occurs
 	 * @see commit
 	 */
 	public void rollback() throws SQLException
 	{
 		if (autoCommit)
 			return;
 
 		if (protoConnection.getTransactionState() != ProtocolConnection.TRANSACTION_IDLE)
 			executeTransactionCommand(rollbackQuery);
 	}
 
 	/*
 	 * Get this Connection's current transaction isolation mode.
 	 *
 	 * @return the current TRANSACTION_* mode value
 	 * @exception SQLException if a database access error occurs
 	 */
 	public int getTransactionIsolation() throws SQLException
 	{
 		String level = null;
 
 		if (haveMinimumServerVersion("7.3")) {			
 			// 7.3+ returns the level as a query result.
 			ResultSet rs = execSQLQuery("SHOW TRANSACTION ISOLATION LEVEL"); // nb: no BEGIN triggered
 			if (rs.next())
 				level = rs.getString(1);
 			rs.close();
 		} else {
 			// 7.2 returns the level as an INFO message. Ew.
 			// We juggle the warning chains a bit here.
 
 			// Swap out current warnings.
 			SQLWarning saveWarnings = getWarnings();
 			clearWarnings();
 
 			// Run the query any examine any resulting warnings.
 			execSQLUpdate("SHOW TRANSACTION ISOLATION LEVEL"); // nb: no BEGIN triggered
 			SQLWarning warning = getWarnings();
 			if (warning != null)
 				level = warning.getMessage();
 
 			// Swap original warnings back.
 			clearWarnings();
 			if (saveWarnings != null)
 				addWarning(saveWarnings);
 		}
 
 		// XXX revisit: throw exception instead of silently eating the error in unkwon cases?
 		if (level == null)
 			return Connection.TRANSACTION_READ_COMMITTED; // Best guess.
 
 		level = level.toUpperCase();
 		if (level.indexOf("READ COMMITTED") != -1)
 			return Connection.TRANSACTION_READ_COMMITTED;
 		if (level.indexOf("READ UNCOMMITTED") != -1)
 			return Connection.TRANSACTION_READ_UNCOMMITTED;
 		if (level.indexOf("REPEATABLE READ") != -1)
 			return Connection.TRANSACTION_REPEATABLE_READ;
 		if (level.indexOf("SERIALIZABLE") != -1)
 			return Connection.TRANSACTION_SERIALIZABLE;
 
 		return Connection.TRANSACTION_READ_COMMITTED; // Best guess.
 	}
 
 	/*
 	 * You can call this method to try to change the transaction
 	 * isolation level using one of the TRANSACTION_* values.
 	 *
 	 * <B>Note:</B> setTransactionIsolation cannot be called while
 	 * in the middle of a transaction
 	 *
 	 * @param level one of the TRANSACTION_* isolation values with
 	 *	the exception of TRANSACTION_NONE; some databases may
 	 *	not support other values
 	 * @exception SQLException if a database access error occurs
 	 * @see java.sql.DatabaseMetaData#supportsTransactionIsolationLevel
 	 */
 	public void setTransactionIsolation(int level) throws SQLException
 	{
 		if (protoConnection.getTransactionState() != ProtocolConnection.TRANSACTION_IDLE)
 			throw new PSQLException(GT.tr("Cannot change transaction isolation level in the middle of a transaction."));
 
 		String isolationLevelName = getIsolationLevelName(level);
 		if (isolationLevelName == null)
 			throw new PSQLException(GT.tr("Transaction isolation level {0} not supported.", new Integer(level)), PSQLState.TRANSACTION_STATE_INVALID);
 
 		String isolationLevelSQL = "SET SESSION CHARACTERISTICS AS TRANSACTION ISOLATION LEVEL " + isolationLevelName;
 		execSQLUpdate(isolationLevelSQL); // nb: no BEGIN triggered
 	}
 
 	protected String getIsolationLevelName(int level)
 	{
 		boolean pg80 = haveMinimumServerVersion("8.0");
 
 		if (level == Connection.TRANSACTION_READ_COMMITTED) {
 			return "READ COMMITTED";
 		} else if (level == Connection.TRANSACTION_SERIALIZABLE) {
 			return "SERIALIZABLE";
 		} else if (pg80 && level == Connection.TRANSACTION_READ_UNCOMMITTED) {
 			return "READ UNCOMMITTED";
 		} else if (pg80 && level == Connection.TRANSACTION_REPEATABLE_READ) {
 			return "REPEATABLE READ";
 		}
 
 		return null;
 	}
 
 	/*
 	 * A sub-space of this Connection's database may be selected by
 	 * setting a catalog name.	If the driver does not support catalogs,
 	 * it will silently ignore this request
 	 *
 	 * @exception SQLException if a database access error occurs
 	 */
 	public void setCatalog(String catalog) throws SQLException
 	{
 		//no-op
 	}
 
 	/*
 	 * Return the connections current catalog name, or null if no
 	 * catalog name is set, or we dont support catalogs.
 	 *
 	 * @return the current catalog name or null
 	 * @exception SQLException if a database access error occurs
 	 */
 	public String getCatalog() throws SQLException
 	{
 		return protoConnection.getDatabase();
 	}
 
 	/*
 	 * Overides finalize(). If called, it closes the connection.
 	 *
 	 * This was done at the request of Rachel Greenham
 	 * <rachel@enlarion.demon.co.uk> who hit a problem where multiple
 	 * clients didn't close the connection, and once a fortnight enough
 	 * clients were open to kill the org.postgres server.
 	 */
 	public void finalize() throws Throwable
 	{
 		close();
 	}
 
 	/*
 	 * Get server version number
 	 */
 	public String getDBVersionNumber()
 	{
 		return dbVersionNumber;
 	}
 
 	// Parse a "dirty" integer surrounded by non-numeric characters
 	private static int integerPart(String dirtyString)
 	{
 		int start, end;
 
 		for (start = 0; start < dirtyString.length() && !Character.isDigit(dirtyString.charAt(start)); ++start)
 			;
 
 		for (end = start; end < dirtyString.length() && Character.isDigit(dirtyString.charAt(end)); ++end)
 			;
 
 		if (start == end)
 			return 0;
 
 		return Integer.parseInt(dirtyString.substring(start, end));
 	}
 
 	/*
 	 * Get server major version
 	 */
 	public int getServerMajorVersion()
 	{
 		try
 		{
 			StringTokenizer versionTokens = new StringTokenizer(dbVersionNumber, ".");  // aaXbb.ccYdd
 			return integerPart(versionTokens.nextToken()); // return X
 		}
 		catch (NoSuchElementException e)
 		{
 			return 0;
 		}
 	}
 
 	/*
 	 * Get server minor version
 	 */
 	public int getServerMinorVersion()
 	{
 		try
 		{
 			StringTokenizer versionTokens = new StringTokenizer(dbVersionNumber, ".");  // aaXbb.ccYdd
 			versionTokens.nextToken(); // Skip aaXbb
 			return integerPart(versionTokens.nextToken()); // return Y
 		}
 		catch (NoSuchElementException e)
 		{
 			return 0;
 		}
 	}
 
 	/**
 	 * Is the server we are connected to running at least this version?
 	 * This comparison method will fail whenever a major or minor version
 	 * goes to two digits (10.3.0) or (7.10.1).
 	 */
 	public boolean haveMinimumServerVersion(String ver)
 	{
 		return (dbVersionNumber.compareTo(ver) >= 0);
 	}
 
 	/*
 	 * This method returns true if the compatible level set in the connection
 	 * (which can be passed into the connection or specified in the URL)
 	 * is at least the value passed to this method.  This is used to toggle
 	 * between different functionality as it changes across different releases
 	 * of the jdbc driver code.  The values here are versions of the jdbc client
 	 * and not server versions.  For example in 7.1 get/setBytes worked on
 	 * LargeObject values, in 7.2 these methods were changed to work on bytea
 	 * values.	This change in functionality could be disabled by setting the
 	 * "compatible" level to be 7.1, in which case the driver will revert to
 	 * the 7.1 functionality.
 	 */
 	public boolean haveMinimumCompatibleVersion(String ver)
 	{
 		return (compatible.compareTo(ver) >= 0);
 	}
 
 
 	public Encoding getEncoding() {
 		return protoConnection.getEncoding();
 	}
 
 	public byte[] encodeString(String str) throws SQLException {
 		try {
 			return getEncoding().encode(str);
 		} catch (IOException ioe) {
 			throw new PSQLException(GT.tr("Unable to translate data into the desired encoding."), PSQLState.DATA_ERROR, ioe);
 		}
 	}
 
 	/*
 	 * This returns the java.sql.Types type for a PG type oid
 	 *
 	 * @param oid PostgreSQL type oid
 	 * @return the java.sql.Types type
 	 * @exception SQLException if a database access error occurs
 	 */
 	public int getSQLType(int oid) throws SQLException
 	{
 		return getSQLType(getPGType(oid));
 	}
 
 	/*
 	 * This returns the oid for a given PG data type
 	 * @param typeName PostgreSQL type name
 	 * @return PostgreSQL oid value for a field of this type, or 0 if not found
 	 */
 	public int getPGType(String typeName) throws SQLException
 	{
 		if (typeName == null)
 			return Oid.INVALID;
 
 		synchronized (this) {
 			Integer oidValue = (Integer) typeOidCache.get(typeName);
 			if (oidValue != null)
 				return oidValue.intValue();
 
 			// it's not in the cache, so perform a query, and add the result to the cache
 			int oid = Oid.INVALID;
 
 			PreparedStatement query;
 			if (haveMinimumServerVersion("7.3"))
 				query = prepareStatement("SELECT oid FROM pg_catalog.pg_type WHERE typname=?");
 			else
 				query = prepareStatement("SELECT oid FROM pg_type WHERE typname=?");
 
 			query.setString(1, typeName);
 
 			if (! ((BaseStatement)query).executeWithFlags(QueryExecutor.QUERY_SUPPRESS_BEGIN) )
				throw new PSQLException(GT.tr("No results where returned by the query."), PSQLState.NO_DATA);
 
 			ResultSet result = query.getResultSet();
 			if (result.next()) {
 				oid = result.getInt(1);
 				oidTypeCache.put(new Integer(oid), typeName);
 			}
 
 			typeOidCache.put(typeName, new Integer(oid));
 			result.close();
 			return oid;
 		}
 	}
 
 	/*
 	 * We also need to get the PG type name as returned by the back end.
 	 *
 	 * @return the String representation of the type, or null if not fould
 	 * @exception SQLException if a database access error occurs
 	 */
 	public String getPGType(int oid) throws SQLException
 	{
 		if (oid == Oid.INVALID)
 			return null;
 
 		synchronized (this) {
 			String cachedValue = (String)oidTypeCache.get(new Integer(oid));
 			if (cachedValue != null)
 				return cachedValue;
 
 			// it's not in the cache, so perform a query, and add the result to the cache
 			String typeName = null;
 
 			PreparedStatement query;
 			if (haveMinimumServerVersion("7.3"))
 				query = prepareStatement("SELECT typname FROM pg_catalog.pg_type WHERE oid=?");
 			else
 				query = prepareStatement("SELECT typname FROM pg_type WHERE oid=?");
 
 			query.setInt(1, oid);
 
 			if (! ((BaseStatement)query).executeWithFlags(QueryExecutor.QUERY_SUPPRESS_BEGIN) )
				throw new PSQLException(GT.tr("No results where returned by the query."), PSQLState.NO_DATA);
 
 			ResultSet result = query.getResultSet();
 			if (result.next()) {
 				typeName = result.getString(1);
 				typeOidCache.put(typeName, new Integer(oid));
 			}
 
 			oidTypeCache.put(new Integer(oid), typeName);
 			result.close();
 			return typeName;
 		}
 	}
 
 	// This is a cache of the DatabaseMetaData instance for this connection
 	protected java.sql.DatabaseMetaData metadata;
 
 	/*
 	 * Tests to see if a Connection is closed
 	 *
 	 * @return the status of the connection
 	 * @exception SQLException (why?)
 	 */
 	public boolean isClosed() throws SQLException
 	{
 		return protoConnection.isClosed();
 	}
 
 	public void cancelQuery() throws SQLException
 	{
 		protoConnection.sendQueryCancel();
 	}
 
 	public PGNotification[] getNotifications()
 	{
 		// Backwards-compatibility hand-holding.
 		PGNotification[] notifications = protoConnection.getNotifications();
 		return (notifications.length == 0 ? null : notifications);
 	}
 
 	//
 	// Handler for transaction queries
 	//
 	private class TransactionCommandHandler implements ResultHandler {
 		private SQLException error;
 		
 		public void handleResultRows(Query fromQuery, Field[] fields, Vector tuples, ResultCursor cursor) {}
 		public void handleCommandStatus(String status, int updateCount, long insertOID) {}
 		
 		public void handleWarning(SQLWarning warning) {
 			AbstractJdbc2Connection.this.addWarning(warning);
 		}
 		
 		public void handleError(SQLException newError) {
 			if (error == null)
 				error = newError;
 			else
 				error.setNextException(newError);
 		}
 		
 		public void handleCompletion() throws SQLException {
 			if (error != null)
 				throw error;
 		}
 	}
 
 	public int getPrepareThreshold() {
 		return prepareThreshold;
 	}
 	
 	public void setPrepareThreshold(int newThreshold) {
 		this.prepareThreshold = (newThreshold <= 0 ? 0 : newThreshold);
 	}	
 
 
 	public void setTypeMapImpl(java.util.Map map) throws SQLException
 	{
 		typemap = map;
 	}
 
 
 
 	//Because the get/setLogStream methods are deprecated in JDBC2
 	//we use the get/setLogWriter methods here for JDBC2 by overriding
 	//the base version of this method
 	protected void enableDriverManagerLogging()
 	{
 		if (DriverManager.getLogWriter() == null)
 		{
 			DriverManager.setLogWriter(new PrintWriter(System.out));
 		}
 	}
 
 
 	/*
 	 * This implemetation uses the jdbc2Types array to support the jdbc2
 	 * datatypes.
 	 */
 	public int getSQLType(String pgTypeName)
 	{
 		if (pgTypeName == null)
 			return Types.OTHER;
 
 		for (int i = 0;i < jdbc2Types.length;i++)
 			if (pgTypeName.equals(jdbc2Types[i]))
 				return jdbc2Typei[i];
 
 		return Types.OTHER;
 	}
 
 	/*
 	 * This table holds the org.postgresql names for the types supported.
 	 * Any types that map to Types.OTHER (eg POINT) don't go into this table.
 	 * They default automatically to Types.OTHER
 	 *
 	 * Note: This must be in the same order as below.
 	 *
 	 * Tip: keep these grouped together by the Types. value
 	 */
     static final String jdbc2Types[] = {
 				"int2",
 				"int4", "oid",
 				"int8",
 				"cash", "money",
 				"numeric",
 				"float4",
 				"float8",
 				"bpchar", "char", "char2", "char4", "char8", "char16",
 				"varchar", "text", "name", "filename",
 				"bytea",
 				"bool",
 				"bit",
 				"date",
 				"time", "timetz",
 				"abstime", "timestamp", "timestamptz",
 				"_bool", "_char", "_int2", "_int4", "_text",
 				"_oid", "_varchar", "_int8", "_float4", "_float8",
 				"_abstime", "_date", "_time", "_timestamp", "_numeric",
 				"_bytea"
 			};
 
 	/*
 	 * This table holds the JDBC type for each entry above.
 	 *
 	 * Note: This must be in the same order as above
 	 *
 	 * Tip: keep these grouped together by the Types. value
 	 */
 	static final int jdbc2Typei[] = {
 												Types.SMALLINT,
 												Types.INTEGER, Types.INTEGER,
 												Types.BIGINT,
 												Types.DOUBLE, Types.DOUBLE,
 												Types.NUMERIC,
 												Types.REAL,
 												Types.DOUBLE,
 												Types.CHAR, Types.CHAR, Types.CHAR, Types.CHAR, Types.CHAR, Types.CHAR,
 												Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR,
 												Types.BINARY,
 												Types.BIT,
 												Types.BIT,
 												Types.DATE,
 												Types.TIME, Types.TIME,
 												Types.TIMESTAMP, Types.TIMESTAMP, Types.TIMESTAMP,
 												Types.ARRAY, Types.ARRAY, Types.ARRAY, Types.ARRAY, Types.ARRAY,
 												Types.ARRAY, Types.ARRAY, Types.ARRAY, Types.ARRAY, Types.ARRAY,
 												Types.ARRAY, Types.ARRAY, Types.ARRAY, Types.ARRAY, Types.ARRAY,
 												Types.ARRAY
 											};
 
 
 
 
 }
 
 
