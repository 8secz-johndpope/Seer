 package com.flexive.core.storage.h2;
 
 import com.flexive.core.Database;
 import com.flexive.core.storage.SequencerStorage;
 import com.flexive.core.storage.genericSQL.GenericSequencerStorage;
 import com.flexive.shared.CustomSequencer;
 import com.flexive.shared.exceptions.FxApplicationException;
 import com.flexive.shared.exceptions.FxCreateException;
 import com.flexive.shared.exceptions.FxDbException;
 import org.apache.commons.lang.StringUtils;
 import org.apache.commons.logging.Log;
 import org.apache.commons.logging.LogFactory;
 
 import java.sql.*;
 import java.util.ArrayList;
 import java.util.List;
 
 /**
  * Concrete H2 sequencer storage implementation.
  * Please note that H2 sequences do not support rollover and will always return false for rollover,
  * they just don't increment if the max. number (Long?) is reached and no error is thrown.
  *
  * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
  */
 public class H2SequencerStorage extends GenericSequencerStorage {
     private static final Log LOG = LogFactory.getLog(H2SequencerStorage.class);
 
     private static final H2SequencerStorage instance = new H2SequencerStorage();
 
     private final static String TBL_H2_SEQUENCES = "INFORMATION_SCHEMA.SEQUENCES";
     private final static String H2_SEQ_PREFIX = "FXSEQ_";
     private final static String SQL_NEXT = "SELECT CURRVAL(?), NEXTVAL(?) FROM DUAL";
     private final static String SQL_CREATE = "CREATE SEQUENCE ";
     private final static String SQL_DELETE = "DROP SEQUENCE " + H2_SEQ_PREFIX;
     private final static String SQL_EXIST = "SELECT COUNT(*) FROM " + TBL_H2_SEQUENCES + " WHERE SEQUENCE_NAME=?";
     private final static String SQL_GET_COMMENT = "SELECT r.REMARKS FROM " + TBL_H2_SEQUENCES + " r WHERE r.SEQUENCE_NAME=";
     private final static String SQL_SET_COMMENT = "COMMENT ON SEQUENCE ";
     private final static String SQL_GET_USER = "SELECT SUBSTR(s.SEQUENCE_NAME," + (H2_SEQ_PREFIX.length() + 1) + "), (" + SQL_GET_COMMENT + "s.SEQUENCE_NAME) AS ROLLOVER, s.CURRENT_VALUE FROM " +
             TBL_H2_SEQUENCES + " s WHERE s.SEQUENCE_NAME NOT LIKE '" + H2_SEQ_PREFIX + "SYS_%' AND s.SEQUENCE_NAME LIKE '"+H2_SEQ_PREFIX+"%' ORDER BY s.SEQUENCE_NAME ASC";
 
    private static final String NOROLLOVER = "NOROLLOVER";
    private static final String ROLLOVER = "ROLLOVER";
 
     /**
      * Singleton getter
      *
      * @return SequencerStorage
      */
     public static SequencerStorage getInstance() {
         return instance;
     }
 
     /**
      * {@inheritDoc}
      */
     public long getMaxId() {
         return Long.MAX_VALUE;
     }
 
     /**
      * {@inheritDoc}
      */
     public long fetchId(String name, boolean allowRollover) throws FxCreateException {
         Connection con = null;
         PreparedStatement ps = null;
         try {
             // Obtain a database connection
             con = Database.getDbConnection();
 
             // Prepare the new id
             ps = con.prepareStatement(SQL_NEXT);
             ps.setString(1, H2_SEQ_PREFIX + name);
             ps.setString(2, H2_SEQ_PREFIX + name);
             ResultSet rs = ps.executeQuery();
             long curr, newId;
             if (rs != null && rs.next()) {
                 curr = rs.getLong(1);
                 newId = rs.getLong(2);
             } else
                 throw new FxCreateException(LOG, "ex.sequencer.fetch.failed", name);
             if (curr == newId || curr < -1 || newId >= getMaxId()) {
                 if (!name.startsWith("SYS_")) {
                     //get allowRollover setting
                     ps.close();
                     ps = con.prepareStatement(SQL_GET_COMMENT + "?");
                     ps.setString(1, H2_SEQ_PREFIX + name);
                     ResultSet rso = ps.executeQuery();
                     if (rso == null || !rso.next())
                         throw new FxCreateException(LOG, "ex.sequencer.fetch.failed", name);
                     allowRollover = ROLLOVER.equals(rso.getString(1));
                 }
                 if (!allowRollover)
                     throw new FxCreateException("ex.sequencer.exhausted", name);
                 //reset it
                 ps.close();
                 ps = con.prepareStatement("ALTER SEQUENCE " + H2_SEQ_PREFIX + name + " RESTART WITH 1");
                 ps.executeUpdate();
                 newId = 0;
             }
             // Return new id
             return newId;
         } catch (SQLException exc) {
             throw new FxCreateException(LOG, exc, "ex.sequencer.fetch.failedMsg", name, exc.getMessage());
         } finally {
             Database.closeObjects(H2SequencerStorage.class, con, ps);
         }
     }
 
     /**
      * {@inheritDoc}
      */
     public void createSequencer(String name, boolean allowRollover, long startNumber) throws FxApplicationException {
         if (StringUtils.isEmpty(name) || name.toUpperCase().trim().startsWith("SYS_"))
             throw new FxCreateException(LOG, "ex.sequencer.create.invalid.name", name);
         name = name.toUpperCase().trim();
         if (sequencerExists(name))
             throw new FxCreateException(LOG, "ex.sequencer.create.invalid.name", name);
 
         Connection con = null;
         Statement stmt = null;
         try {
             con = Database.getDbConnection();
             stmt = con.createStatement();
             stmt.executeUpdate(SQL_CREATE + H2_SEQ_PREFIX + name + " START WITH " + startNumber + " INCREMENT BY 1");
             stmt.executeUpdate(SQL_SET_COMMENT + H2_SEQ_PREFIX + name + " IS '" + (allowRollover ? ROLLOVER : NOROLLOVER) + "'");
         } catch (SQLException exc) {
             throw new FxCreateException(LOG, exc, "ex.sequencer.create.failed", name);
         } finally {
             Database.closeObjects(H2SequencerStorage.class, con, stmt);
         }
     }
 
     /**
      * {@inheritDoc}
      */
     public void removeSequencer(String name) throws FxApplicationException {
         if (!sequencerExists(name))
             throw new FxCreateException(LOG, "ex.sequencer.remove.notFound", name);
         Connection con = null;
         Statement stmt = null;
         try {
             con = Database.getDbConnection();
             stmt = con.createStatement();
             stmt.executeUpdate(SQL_DELETE + name);
         } catch (SQLException exc) {
             throw new FxCreateException(LOG, exc, "ex.sequencer.remove.failed", name, exc.getMessage());
         } finally {
             Database.closeObjects(H2SequencerStorage.class, con, stmt);
         }
     }
 
     /**
      * {@inheritDoc}
      */
     public boolean sequencerExists(String name) throws FxApplicationException {
         if (StringUtils.isEmpty(name) || name.toUpperCase().trim().startsWith("SYS_"))
             return false;
         Connection con = null;
         PreparedStatement ps = null;
         try {
             con = Database.getDbConnection();
             ps = con.prepareStatement(SQL_EXIST);
             ps.setString(1, H2_SEQ_PREFIX + name.trim().toUpperCase());
             ResultSet rs = ps.executeQuery();
             return !(rs == null || !rs.next()) && rs.getInt(1) != 0;
         } catch (SQLException exc) {
             throw new FxDbException(LOG, exc, "ex.db.sqlError", exc.getMessage());
         } finally {
             Database.closeObjects(H2SequencerStorage.class, con, ps);
         }
     }
 
     /**
      * {@inheritDoc}
      */
     public List<CustomSequencer> getCustomSequencers() throws FxApplicationException {
         List<CustomSequencer> res = new ArrayList<CustomSequencer>(20);
         Connection con = null;
         PreparedStatement ps = null;
         try {
             con = Database.getDbConnection();
             ps = con.prepareStatement(SQL_GET_USER);
             ResultSet rs = ps.executeQuery();
             while (rs != null && rs.next())
                 res.add(new CustomSequencer(rs.getString(1), ROLLOVER.equals(rs.getString(2)), rs.getLong(3)));
         } catch (SQLException exc) {
             throw new FxDbException(LOG, exc, "ex.db.sqlError", exc.getMessage());
         } finally {
             Database.closeObjects(H2SequencerStorage.class, con, ps);
         }
         return res;
     }
 
     /**
      * {@inheritDoc}
      */
     public void setSequencerId(String name, long newId) throws FxApplicationException {
         Connection con = null;
         PreparedStatement ps = null;
         try {
             con = Database.getDbConnection();
             ps = con.prepareStatement("ALTER SEQUENCE " + H2_SEQ_PREFIX + name + " RESTART WITH ?");
             ps.setLong(1, newId + 1);
             ps.executeUpdate();
         } catch (SQLException exc) {
             throw new FxDbException(LOG, exc, "ex.db.sqlError", exc.getMessage());
         } finally {
             Database.closeObjects(H2SequencerStorage.class, con, ps);
         }
     }
 
 }
