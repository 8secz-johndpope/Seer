 package kfs.kfsDbi;
 
 import java.sql.*;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Collection;
 import java.util.HashMap;
 import java.util.List;
 import java.util.logging.Level;
 import java.util.logging.Logger;
 
 /**
  *
  * @author pavedrim
  */
 public abstract class kfsADb {
 
     public interface loadCB {
 
         boolean kfsDbAddItem(kfsRowData rd, kfsDbObject inx);
     };
     protected static final Logger l = Logger.getLogger(kfsADb.class.getName());
     private final HashMap<String, PreparedStatement> closingList;
     private final Connection conn;
     private final kfsDbServerType serverType;
     private final String schema_;
 
     protected kfsADb(
             final String schema,///
             final kfsDbServerType serverType, //
             final Connection conn) {
         this.closingList = new HashMap<String, PreparedStatement>();
         this.conn = conn;
         this.serverType = serverType;
         this.schema_ = schema;
     }
 
     protected abstract Collection<kfsDbiTable> getDbObjects();
 
     protected Collection<kfsDbiTable> getFulltextObjects() {
         return Arrays.<kfsDbiTable>asList();
     }
     
     protected kfsDbServerType getServerType() {
         return serverType;
     }
     protected String getSchema() {
         return schema_;
     }
     
     protected PreparedStatement getInsert(kfsDbiTable tab) {
         String sql = tab.getInsertInto();
         if (!closingList.containsKey(sql)) {
             try {
 
                 PreparedStatement ps;
                 if (tab.hasGenerateAutoKeys()) {
                     if (this.serverType == kfsDbServerType.kfsDbiOracle) {
                         ps = conn.prepareStatement(sql, tab.getAutoGeneratedColumnNames());
                     } else {
                         ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                     }
                 } else {
                     ps = conn.prepareStatement(sql);
                 }
                 closingList.put(sql, ps);
                 return ps;
             } catch (SQLException ex) {
                 l.log(Level.SEVERE, "Error in call getInsert for sql: " + sql, ex);
             }
         }
         return closingList.get(sql);
     }
 
     protected PreparedStatement getUpdate(kfsDbiTable tab) {
         String sql = tab.getUpdate();
         if (sql == null) {
             return null;
         }
         try {
             return prepare(sql);
         } catch (SQLException ex) {
             l.log(Level.SEVERE, "Cannot prepare statement for SQL: " + sql, ex);
         }
         return null;
     }
 
     @Deprecated
     protected PreparedStatement getExist(kfsDbiTable tab) {
         String sql = tab.getExistItemSelect();
         if (sql == null) {
             return null;
         }
         try {
             return prepare(sql);
         } catch (SQLException ex) {
             l.log(Level.SEVERE, "Error in create statement for SQL:" + sql, ex);
         }
         return null;
     }
 
     protected PreparedStatement getSelect(kfsDbiTable tab) {
         try {
             return prepare(tab.getSelect());
         } catch (SQLException ex) {
             l.log(Level.SEVERE, "Error in call getSelect for dbName: " + tab.getName(), ex);
         }
         return null;
     }
 
     protected CallableStatement prepareCs(String sql) throws SQLException {
         CallableStatement ps = (CallableStatement) closingList.get(sql);
         if (ps == null) {
             ps = conn.prepareCall(sql);
             closingList.put(sql, ps);
         }
         return ps;
     }
 
     protected PreparedStatement prepare(String sql) throws SQLException {
         PreparedStatement ps = closingList.get(sql);
         if (ps == null) {
             ps = conn.prepareStatement(sql);
             closingList.put(sql, ps);
         }
         return ps;
     }
 
     public void commit() throws SQLException {
         conn.commit();
     }
     
     public void done(boolean commit, boolean rollback) throws SQLException {
         if (conn == null) {
             return;
         }
         if (conn.isClosed()) {
             return;
         }
         if (commit) {
             conn.commit();
         } else {
             if (rollback) {
                 conn.rollback();
             }
         }
         for (PreparedStatement ps : closingList.values()) {
             ps.close();
         }
         conn.close();
     }
 
     private String getExist() {
         switch (serverType) {
             case kfsDbiSqlite:
                 return "SELECT name FROM sqlite_master WHERE type='table' AND name=?";
             case kfsDbiMysql:
                 return "SELECT table_name FROM information_schema.tables WHERE "
                         + "table_schema = ? AND table_name = ?";
             case kfsDbiOracle:
                 return "SELECT table_name FROM all_tables WHERE owner=? AND table_name =?";
             case kfsDbiPostgre:
                 return "SELECT tablename FROM pg_catalog.pg_tables WHERE "
                         + "schemaname=? AND tablename=?";
         }
         return null;
     }
 
     protected void reCreateTables() {
         for (kfsDbiTable ie : getDbObjects()) {
             if (ie == null) {
                 throw new RuntimeException("dbObject cannot be null");
             }
             try {
                 PreparedStatement ps = prepare("DROP TABLE " + ie.getName());
                 ps.execute();
             } catch (SQLException ex) {
                 l.log(Level.SEVERE, "Cannot drop " + ie.getName(), ex);
             }
 
         }
         createTables();
     }
 
     protected void createTables() {
         createTables(schema_);
     }
     protected void createTables(String schema) {
         try {
             PreparedStatement psExistTable = conn.prepareStatement(getExist());
             Statement executeStatement = conn.createStatement();
             for (kfsDbiTable ie : getDbObjects()) {
                 if (ie == null) {
                     throw new RuntimeException("dbObject cannot be null");
                 }
                 String sql = "";
                 try {
                     sql = ie.getCreateTable();
                     if ((sql != null) && (sql.length() > 0)) {
                         if ((schema != null) && (schema.length() > 0)) {
                             psExistTable.setString(1, schema);
                             psExistTable.setString(2, ie.getName());
                         } else {
                             psExistTable.setString(1, ie.getName());
                         }
                         ResultSet rs = psExistTable.executeQuery();
                         boolean ret = rs.next();
                         rs.close();
                         if (!ret) {
                             if (sql != null) {
                                 executeStatement.execute(sql);
                             }
                             for (String ss : ie.getCreateTableAddons()) {
                                 executeStatement.execute(ss);
                             }
                             String ft = ie.createFullTextIndex();
                             if (ft.length() > 0) {
                                 executeStatement.execute(ft);
                             }
                         }
                     }
                 } catch (Exception ex) {
                     l.log(Level.SEVERE, "Error in " + ie.getName() + ".createTable: " + sql, ex);
                 }
             }
             executeStatement.close();
             psExistTable.close();
         } catch (SQLException ex) {
             l.log(Level.SEVERE, "Error in createTable", ex);
         }
     }
 
     protected int loadAll(ArrayList<kfsRowData> data, kfsDbObject inx) {
         int ret = data.size();
         try {
             ResultSet rs = null;
             try {
                 rs = getSelect(inx).executeQuery();
                 while (rs.next()) {
                     kfsRowData r = new kfsRowData(inx);
                     inx.psSelectGetParameters(rs, r);
                     data.add(r);
                 }
             } finally {
                 if (rs != null) {
                     rs.close();
                 }
             }
         } catch (SQLException ex) {
             l.log(Level.SEVERE, "Error in " + inx.getName() + ".loadAll", ex);
 
         }
         return data.size() - ret;
     }
 
     protected int loadAll(loadCB loadCb, kfsDbObject inx) {
         int ret = 0;
         try {
             ResultSet rs = null;
             try {
                 rs = getSelect(inx).executeQuery();
                 while (rs.next()) {
                     kfsRowData r = new kfsRowData(inx);
                     inx.psSelectGetParameters(rs, r);
                     loadCb.kfsDbAddItem(r, inx);
                     ret++;
                 }
             } finally {
                 if (rs != null) {
                     rs.close();
                 }
             }
         } catch (SQLException ex) {
             l.log(Level.SEVERE, "Error in " + inx.getName() + ".loadAll " + getSelect(inx) , ex);
         }
         return ret;
     }
 
     public void sort(kfsDbiColumnComparator[] sColumns, List<kfsRowData> lst, kfsDbObject o) {
         o.sort(sColumns, lst);
     }
 
     protected int loadCust(PreparedStatement ps, ArrayList<kfsRowData> data, kfsDbObject inx) throws SQLException {
         int ret = data.size();
         ResultSet rs = null;
         try {
             rs = ps.executeQuery();
             while (rs.next()) {
                 kfsRowData r = new kfsRowData(inx);
                 inx.psSelectGetParameters(rs, r);
                 data.add(r);
             }
         } finally {
             if (rs != null) {
                 rs.close();
             }
         }
         return data.size() - ret;
     }
 
     protected int loadCust(CallableStatement ps, int resInx, loadCB loadCb, kfsDbObject inx) throws SQLException {
         int ret = 0;
         ResultSet rs = null;
         try {
             ps.registerOutParameter(resInx, -10); //REF CURSOR OracleTypes.CURSOR
             ps.execute();
             rs = (ResultSet) ps.getObject(resInx);
             while (rs.next()) {
                 kfsRowData r = new kfsRowData(inx);
                 inx.psSelectGetParameters(rs, r);
                 loadCb.kfsDbAddItem(r, inx);
                 ret++;
             }
         } finally {
             if (rs != null) {
                 rs.close();
             }
         }
         return ret;
     }
 
     protected int loadCust(PreparedStatement ps, loadCB loadCb, kfsDbObject inx) throws SQLException {
         int ret = 0;
         ResultSet rs = null;
         try {
             rs = ps.executeQuery();
             while (rs.next()) {
                 kfsRowData r = new kfsRowData(inx);
                 inx.psSelectGetParameters(rs, r);
                 loadCb.kfsDbAddItem(r, inx);
                 ret++;
             }
         } finally {
             if (rs != null) {
                 rs.close();
             }
         }
         return ret;
     }
 
     @Deprecated
     protected Boolean exist(kfsDbiTable tab, kfsRowData row) {
         Boolean ret = null;
         try {
             PreparedStatement ps = getExist(tab);
             if (ps != null) {
                 ps.clearParameters();
                 tab.psExistItemSetParameters(ps, row);
                 ResultSet rs = getExist(tab).executeQuery();
                 if (!rs.next()) {
                     ret = Boolean.TRUE;
                 } else {
                     ret = Boolean.FALSE;
                 }
                 rs.close();
 
             }
         } catch (SQLException ex) {
             l.log(Level.SEVERE, "Error in Exist in " + tab.getName(), ex);
         }
         return ret;
     }
 
     protected boolean delete(kfsDbiTable tab, kfsRowData r) {
         String sql = tab.getDelete();
         if (sql != null) {
             try {
                 PreparedStatement ps = prepare(sql);
                 ps.clearParameters();
                 tab.psSetDelete(ps, r);
                 ps.executeUpdate();
                 return true;
             } catch (SQLException ex) {
                l.log(Level.SEVERE, "Cannot delete Edge", ex);
             }
         } else {
             l.log(Level.WARNING, "Try call to Delete for DBI: {0}", tab.getName());
         }
         return false;
     }
 
     protected int insertExc(kfsDbiTable tab, kfsRowData row) throws SQLException {
         int ret = 0;
 
         PreparedStatement ps = getInsert(tab);
         ps.clearParameters();
         tab.psInsertSetParameters(ps, row);
         ps.execute();
         ret++;
         if (tab.hasGenerateAutoKeys()) {
             ResultSet rs = getInsert(tab).getGeneratedKeys();
             if (rs.next()) {
                 tab.psInsertGetAutoKeys(rs, row);
             }
             rs.close();
         }
 
         return ret;
     }
 
     protected int insert(kfsDbiTable tab, kfsRowData row) {
         int ret = 0;
         try {
             PreparedStatement ps = getInsert(tab);
             ps.clearParameters();
             tab.psInsertSetParameters(ps, row);
             ps.execute();
             ret++;
             if (tab.hasGenerateAutoKeys()) {
                 l.log(Level.FINE, "has auto keys");
                 ResultSet rs = getInsert(tab).getGeneratedKeys();
                 if (rs.next()) {
                     tab.psInsertGetAutoKeys(rs, row);
                 }
                 rs.close();
             }
         } catch (SQLException ex) {
             l.log(Level.SEVERE, "Error in INSERT into " + tab.getName(), ex);
         }
         return ret;
     }
 
     protected int update(kfsDbiTable tab, kfsRowData row) {
         int ret = 0;
         try {
             PreparedStatement ps = getUpdate(tab);
             if (ps == null) {
                 l.log(Level.WARNING, "Cannot update {0}", tab.getName());
             } else {
                 ps.clearParameters();
                 tab.psSetUpdate(ps, row);
                 ret = ps.executeUpdate();
             }
         } catch (SQLException ex) {
             l.log(Level.SEVERE, "Error in Update " + tab.getName(), ex);
         }
         return ret;
     }
 
     /**
      * 
      * @param tab
      * @param row
      * @return
      * @deprecated Use insert/update
      */
     @Deprecated
     protected int save(kfsDbiTable tab, kfsRowData row) {
         int ret;
         if (exist(tab, row) != Boolean.TRUE) {
             ret = insert(tab, row);
         } else {
             ret = update(tab, row);
         }
         return ret;
     }
 }
