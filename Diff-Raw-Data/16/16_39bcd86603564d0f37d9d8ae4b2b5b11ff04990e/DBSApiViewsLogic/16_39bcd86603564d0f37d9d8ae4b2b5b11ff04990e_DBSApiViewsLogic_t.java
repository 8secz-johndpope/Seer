 /**
 $Revision: 1.7 $
 $Id: DBSApiViewsLogic.java,v 1.7 2009/06/11 15:34:33 valya Exp $
  Author: Valentin Kuznetsov
  **/
 
 package dbs.api;
 import java.sql.Connection;
 import java.sql.ResultSet;
 import java.sql.PreparedStatement;
 import java.sql.ResultSetMetaData;
 import java.io.Writer;
 import java.io.StringWriter;
 import java.util.Hashtable;
 import java.util.Vector;
 import java.util.ArrayList;
 import java.util.List;
 import java.util.HashSet;
 import java.sql.SQLException;
 import db.DBManagement;
 import dbs.DBSConstants;
 import dbs.sql.DBSSql;
 import dbs.util.DBSUtil;
 import dbs.DBSException;
 
 public class DBSApiViewsLogic extends DBSApiLogic {
     DBSApiData data = null;
     public DBSApiViewsLogic(DBSApiData data) {
         super(data);
         this.data = data;
     }
 
     public static PreparedStatement
     getViewQuery(Connection conn, String viewName,
         String key, List cond,
         String sortKey, String sortOrder) 
     throws SQLException {
         String owner = DBSSql.owner();
         if (owner == null) {
             owner = "";
         }
         String sql = "SELECT * FROM " + owner + viewName;
         if (key != null && cond != null) {
             sql += " WHERE " + key + " IN (";
             for(int i=0;i<cond.size();i++) {
                 if (i == cond.size()-1) {
                     sql += "'"+cond.get(i)+"'";
                 } else {
                     sql += "'"+cond.get(i)+"', ";
                 }
             }
             sql += ") ";
         }
         if (sortKey != null && !sortKey.equals("")) {
             sql += " ORDER BY " + sortKey;
             if (sortOrder != null && !sortOrder.equals("")) {
                 sql += " " + sortOrder;
             } else {
                 sql += " DESC ";
             } 
         } else if (key != null) {
             sql += " ORDER BY " + key + " DESC";
         }
         PreparedStatement ps = DBManagement.getStatement(conn, sql);
         DBSUtil.writeLog("\n\n" + ps + "\n\n");
         return ps;
     }
 
     public void
     getSummary(Connection conn, Writer out, String viewName,
         int begin, int end,
         String key, List cond,
         String sortKey, String sortOrder) 
     throws Exception {
         PreparedStatement ps = null;
         ResultSet rs =  null;
 //        ps = getViewQuery(conn, viewName, key, cond, sortKey, sortOrder);
 //        pushQuery(ps);
 //        rs =  ps.executeQuery();
         try {
             ps = getViewQuery(conn, viewName, key, cond, sortKey, sortOrder);
             pushQuery(ps);
             rs =  ps.executeQuery();
         } catch(Exception e) {
             out.write( ((String)"<summary_view>\n"));
             out.write("</summary_view>\n");
             return;
         }
         ResultSetMetaData rsmd = rs.getMetaData();
         int numberOfColumns = rsmd.getColumnCount();
         out.write( ((String)"<summary_view>\n"));
         int counter = 0;
         while(rs.next()) {
             if (counter >= begin && counter <= end) {
                 out.write("<row>\n");
                 for(int i=1;i<=numberOfColumns;i++) {
                     String cname = rsmd.getColumnName(i);
                     String res = get(rs, cname);
                     String name = viewName+"."+cname;
                     out.write( "<"+name+">"+res+"</"+name+">\n" );
                 }
                 out.write("</row>\n");
             }
             counter += 1;
         }
         out.write("</summary_view>\n");
     }
 
     public void
     executeSummary(Connection conn, Writer out, String userQuery)
     throws Exception {
         String begin = "";
         String end = "";
         String sortKey = null;
         String sortOrder = null;
         executeSummary(conn, out, userQuery, begin, end, sortKey, sortOrder);
     }
     public void
     executeSummary(Connection conn, Writer out, 
         String userQuery, String begin, String end, 
         String sortKey, String sortOrder)
     throws Exception {
         if(begin == null || end == null || begin.equals("") || end.equals("")) {
            throw new Exception("Unbound query is not allowed in summary view");
         } 
         int lbound = new Integer(begin).intValue();
         int rbound = new Integer(end).intValue();
         if ((rbound-lbound)>100) {
            throw new Exception("Too wide range for summary view");
         }
 
         // Step 1, call executeQuery to get SQL for userQuery
         ArrayList objList = executeQuery(conn, out, userQuery, begin, end, false);
         String finalQuery = (String)objList.get(1);

         List<String> bindValues = (List<String>)objList.get(2);
         List<Integer> bindIntValues = (List<Integer>)objList.get(3);

         // Step 2, execute SQL to get results
         PreparedStatement ps = 
                 DBSSql.getQuery(conn, finalQuery, bindValues, bindIntValues);

         List<String> resList = new ArrayList();
         ResultSet rs =  null;
         try {
             rs =  ps.executeQuery();
             ResultSetMetaData rsmd = rs.getMetaData();
             int numberOfColumns = rsmd.getColumnCount();
             while(rs.next()) {
                 for(int i=1;i<=numberOfColumns;i++) {
                     String cname = rsmd.getColumnName(i);
                     String res = get(rs, cname);
                     resList.add(res);
                 }
             }
         } catch(Exception e) {
             out.write( "<exception>\n"+e+"\n</exception>\n" );
             return;
         } finally { 
             if (rs != null) rs.close();
             if (ps != null) ps.close();
         }
 
         // Step 3, pass results to summary view
         String [] temp = userQuery.split(" ");
         String skey = temp[1]; // temp[0]==find
         String key = "Name";
         String view = null;
         // Mapping between QL keys and summary views, 
         // e.g. primds has PrimSummary view and its key is Name
         if (skey.equals("primds")) {
             view = "PrimSummary";
         } else if (skey.equals("run")) {
             view = "RunSummary";
             key  = "RunNumber";
         } else if (skey.equals("file")) {
             view = "FileSummary";
             key  = "LogicalFileName";
         } else if (skey.equals("procds")) {
             view = "ProcSummary";
         } else if (skey.equals("release")) {
             view = "ReleaseSummary";
             key  = "Version";
         } else if (skey.equals("site")) {
             view = "SiteSummary";
             key  = "SEName";
         } else if (skey.equals("tier")) {
             view = "TierSummary";
         } else if (skey.equals("dataset")) {
             view = "DatasetSummary";
             key  = "Path";
         } else if (skey.equals("ads")) {
             view = "AdsSummary";
         }
 
         try {
             getSummary(conn, out, view, lbound, rbound, key, resList, sortKey, sortOrder);
             out.write( "<results>\n" );
             if  (!resList.isEmpty()) {

		//AA-07/30/2009, cannot go abve the limit of rows returned by database
		//even if user specifies a ridiculously large right bound
		if (resList.size() < rbound) rbound=resList.size();

                 for(int i=lbound;i<rbound;i++) {
                     out.write( "<row>\n<"+skey+">"+resList.get(i)+"</"+skey+">\n"+"</row>\n" );
                 }
             }
             out.write( "</results>\n" );
         } catch(Exception e) {
             DBSUtil.writeLog("\nexecuteSummary exception: " + e + "\n");
             out.write( "<summary_view>\n" );
             out.write( "<exception>\n" + e + "\n</exception>\n" );
             out.write( "</summary_view>\n" );
         }
     }
 }
