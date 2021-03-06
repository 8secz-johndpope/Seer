 package msf;
 
 import java.util.*;
 import java.sql.*;
 
 import java.io.*;
 
 import graph.Route;
 
 /* implement the old MSF RPC database calls in a way Armitage likes */
 public class DatabaseImpl implements RpcConnection  {
 	protected Connection db;
 	protected Map queries;
 	protected String workspaceid = "0";
 	protected String hFilter = null;
 	protected String sFilter = null;
 	protected Route[]  rFilter = null;
 	protected String[] oFilter = null;
 
 	private static String join(List items, String delim) {
 		StringBuffer result = new StringBuffer();
 		Iterator i = items.iterator();
 		while (i.hasNext()) {
 			result.append(i.next());
 			if (i.hasNext()) {
 				result.append(delim);
 			}
 		}
 		return result.toString();
 	}
 
 	public void setWorkspace(String name) {
 		try {
 			List spaces = executeQuery("SELECT DISTINCT * FROM workspaces");
 			Iterator i = spaces.iterator();
 			while (i.hasNext()) {
 				Map temp = (Map)i.next();
 				if (name.equals(temp.get("name"))) {
 					workspaceid = temp.get("id") + "";
 					queries = build();
 				}
 			}
 		}
 		catch (Exception ex) {
 			throw new RuntimeException(ex);
 		}
 	}
 
 	public void setDebug(boolean d) {
 
 	}
 
 	public DatabaseImpl() {
 		queries = build();
 	}
 
 	/* marshall the type into something we'd rather deal with */
 	protected Object fixResult(Object o) {
 		if (o instanceof java.sql.Timestamp) {
 			return new Long(((Timestamp)o).getTime());
 		}
 		return o;
 	}
 
 	protected int executeUpdate(String query) throws Exception {
 		Statement s = db.createStatement();
 		return s.executeUpdate(query);
 	}
 
 	/* execute the query and return a linked list of the results..., whee?!? */
 	protected List executeQuery(String query) throws Exception {
 		List results = new LinkedList();
 
 		Statement s = db.createStatement();
 		ResultSet r = s.executeQuery(query);
 
 		while (r.next()) {
 			Map row = new HashMap();
 
 			ResultSetMetaData m = r.getMetaData();
 			int c = m.getColumnCount();
 			for (int i = 1; i <= c; i++) {
 				row.put(m.getColumnLabel(i), fixResult(r.getObject(i)));
 			}
 
 			results.add(row);
 		}
 
 		return results;
 	}
 
 	private boolean checkRoute(String address) {
 		for (int x = 0; x < rFilter.length; x++) {
 			if (rFilter[x].shouldRoute(address))
 				return true;
 		}
 		return false;
 	}
 
 	private boolean checkOS(String os) {
 		String os_l = os.toLowerCase();
 
 		for (int x = 0; x < oFilter.length; x++) {
 			if (os_l.indexOf(oFilter[x]) != -1)
 				return true;
 		}
 		return false;
 	}
 
 	public List filterByRoute(List rows, int max) {
 		if (rFilter != null || oFilter != null) {
 			Iterator i = rows.iterator();
 			while (i.hasNext()) {
 				Map entry = (Map)i.next();
 				if (rFilter != null && entry.containsKey("address")) {
 					if (!checkRoute(entry.get("address") + "")) {
 						i.remove();
 						continue;
 					}
 				}
 				else if (rFilter != null && entry.containsKey("host")) {
 					if (!checkRoute(entry.get("host") + "")) {
 						i.remove();
 						continue;
 					}
 				}
 
 				if (oFilter != null && entry.containsKey("os_name")) {
 					if (!checkOS(entry.get("os_name") + ""))
 						i.remove();
 				}
 			}
 
 			if (rows.size() > max) {
 				rows.subList(max, rows.size()).clear();
 			}
 		}
 
 		return rows;
 	}
 
 	public void connect(String dbstring, String user, String password) throws Exception {
 		db = DriverManager.getConnection(dbstring, user, password);
 		setWorkspace("default");
 	}
 
 	public Object execute(String methodName) throws IOException {
 		return execute(methodName, new Object[0]);
 	}
 
 	protected Map build() {
 		Map temp = new HashMap();
 
 		/* this is an optimization. If we have a network or OS filter, we need to pull back all host/service records and
 		   filter them here. If we do not have these types of filters, then we can let the database do the heavy lifting
 		   and limit the size of the final result there. */
 		String limit1 = rFilter == null && oFilter == null ? "512" : "30000";
 		String limit2 = rFilter == null && oFilter == null ? "12288" : "100000";
 
 		temp.put("db.creds", "SELECT DISTINCT creds.*, hosts.address as host, services.name as sname, services.port as port, services.proto as proto FROM creds, services, hosts WHERE services.id = creds.service_id AND hosts.id = services.host_id AND hosts.workspace_id = " + workspaceid);
 
 		/* db.creds2 exists to prevent duplicate entries for the stuff I care about */
 		temp.put("db.creds2", "SELECT DISTINCT creds.user, creds.pass, hosts.address as host, services.name as sname, services.port as port, services.proto as proto, creds.ptype FROM creds, services, hosts WHERE services.id = creds.service_id AND hosts.id = services.host_id AND hosts.workspace_id = " + workspaceid);
 
 		if (hFilter != null) {
			List tables = new LinkedList();
			tables.add("hosts");
			if (hFilter.indexOf("services.") >= 0)
				tables.add("services");

			if (hFilter.indexOf("sessions.") >= 0)
				tables.add("sessions");

			temp.put("db.hosts", "SELECT DISTINCT hosts.* FROM " + join(tables, ", ") + " WHERE hosts.workspace_id = " + workspaceid + " AND " + hFilter + " LIMIT " + limit1);
 		}
 		else {
 			temp.put("db.hosts", "SELECT DISTINCT hosts.* FROM hosts WHERE hosts.workspace_id = " + workspaceid + " LIMIT " + limit1);
 		}
 
 		temp.put("db.services", "SELECT DISTINCT services.*, hosts.address as host FROM services, (" + temp.get("db.hosts") + ") as hosts WHERE hosts.id = services.host_id LIMIT " + limit2);
 		temp.put("db.loots", "SELECT DISTINCT loots.*, hosts.address as host FROM loots, hosts WHERE hosts.id = loots.host_id AND hosts.workspace_id = " + workspaceid);
 		temp.put("db.workspaces", "SELECT DISTINCT * FROM workspaces");
 		temp.put("db.notes", "SELECT DISTINCT notes.*, hosts.address as host FROM notes, hosts WHERE hosts.id = notes.host_id AND hosts.workspace_id = " + workspaceid);
 		temp.put("db.clients", "SELECT DISTINCT clients.*, hosts.address as host FROM clients, hosts WHERE hosts.id = clients.host_id AND hosts.workspace_id = " + workspaceid);
 		return temp;
 	}
 
 	public Object execute(String methodName, Object[] params) throws IOException {
 		try {
 			if (queries.containsKey(methodName)) {
 				String query = queries.get(methodName) + "";
 				Map result = new HashMap();
 
 				if (methodName.equals("db.services")) {
 					result.put(methodName.substring(3), filterByRoute(executeQuery(query), 12288));
 				}
 				else if (methodName.equals("db.hosts")) {
 					result.put(methodName.substring(3), filterByRoute(executeQuery(query), 512));
 				}
 				else {
 					result.put(methodName.substring(3), executeQuery(query));
 				}
 				return result;
 			}
 			else if (methodName.equals("db.vulns")) {
 				//List a = executeQuery("SELECT DISTINCT vulns.*, hosts.address as host, services.port as port, services.proto as proto FROM vulns, hosts, services WHERE hosts.id = vulns.host_id AND services.id = vulns.service_id");
 				//List b = executeQuery("SELECT DISTINCT vulns.*, hosts.address as host FROM vulns, hosts WHERE hosts.id = vulns.host_id AND vulns.service_id IS NULL");
 				List a = executeQuery("SELECT DISTINCT vulns.*, hosts.address as host, services.port as port, services.proto as proto, refs.name as refs FROM vulns, hosts, services, vulns_refs, refs WHERE hosts.id = vulns.host_id AND services.id = vulns.service_id AND vulns_refs.vuln_id = vulns.id AND vulns_refs.ref_id = refs.id AND hosts.workspace_id = " + workspaceid);
 				List b = executeQuery("SELECT DISTINCT vulns.*, hosts.address as host, refs.name as refs FROM vulns, hosts, refs, vulns_refs WHERE hosts.id = vulns.host_id AND vulns.service_id IS NULL AND vulns_refs.vuln_id = vulns.id AND vulns_refs.ref_id = refs.id AND hosts.workspace_id = " + workspaceid);
 
 				a.addAll(b);
 
 				Map result = new HashMap();
 				result.put("vulns", a);
 				return result;
 			}
 			else if (methodName.equals("db.clear")) {
 				executeUpdate("DELETE FROM hosts");
 				executeUpdate("DELETE FROM services");
 				executeUpdate("DELETE FROM events");
 				executeUpdate("DELETE FROM notes");
 				executeUpdate("DELETE FROM creds");
 				executeUpdate("DELETE FROM loots");
 				executeUpdate("DELETE FROM vulns");
				executeUpdate("DELETE FROM sessions");
				executeUpdate("DELETE FROM clients");
 				return new HashMap();
 			}
 			else if (methodName.equals("db.filter")) {
 				/* I'd totally do parameterized queries if I wasn't building this
 				   damned query dynamically. Hence it'll have to do. */
 				Map values = (Map)params[0];
 
 				rFilter = null;
 				oFilter = null;
 
 				List hosts = new LinkedList();
 				List srvcs = new LinkedList();
 
 				if ((values.get("session") + "").equals("1")) {
 					hosts.add("sessions.host_id = hosts.id AND sessions.closed_at IS NULL AND sessions.close_reason IS NULL");
 					//srvcs.add("sessions.host_id = hosts.id AND sessions.closed_at IS NULL");
 				}
 
 				if (values.containsKey("hosts") && (values.get("hosts") + "").length() > 0) {
 					String h = values.get("hosts") + "";
 					if (!h.matches("[0-9a-fA-F\\.:\\%\\_/, ]+")) {
 						System.err.println("Host value did not validate!");
 						return new HashMap();
 					}
 					String[] routes = h.split(",\\s+");
 					rFilter = new Route[routes.length];
 
 					for (int x = 0; x < routes.length; x++) {
 						rFilter[x] = new Route(routes[x]);
 					}
 				}
 
 				if (values.containsKey("ports") && (values.get("ports") + "").length() > 0) {
 					List ports = new LinkedList();
 					List ports2 = new LinkedList();
 					String[] p = (values.get("ports") + "").split(",\\s+");
 					for (int x = 0; x < p.length; x++) {
 						if (!p[x].matches("[0-9]+")) {
 							return new HashMap();
 						}
 
 						ports.add("services.port = " + p[x]);
 						//ports2.add("s.port = " + p[x]);
 					}
 					hosts.add("services.host_id = hosts.id");
 					hosts.add("(" + join(ports, " OR ") + ")");
 				}
 
 				if (values.containsKey("os") && (values.get("os") + "").length() > 0) {
 					oFilter = (values.get("os") + "").toLowerCase().split(",\\s+");
 				}
 
 				if (hosts.size() == 0) {
 					hFilter = null;
 				}
 				else {
 					hFilter = join(hosts, " AND ");
 				}
 
 				queries = build();
 				return new HashMap();
 			}
 			else if (methodName.equals("db.fix_creds")) {
 				Map values = (Map)params[0];
 				PreparedStatement stmt = null;
 				stmt = db.prepareStatement("UPDATE creds SET ptype = 'smb_hash' WHERE creds.user = ? AND creds.pass = ?");
 				stmt.setString(1, values.get("user") + "");
 				stmt.setString(2, values.get("pass") + "");
 
 				Map result = new HashMap();
 				result.put("rows", new Integer(stmt.executeUpdate()));
 				return result;
 			}
 			else if (methodName.equals("db.report_host")) {
 				Map values = (Map)params[0];
 				String host = values.get("host") + "";
 				PreparedStatement stmt = null;
 
 				if (values.containsKey("os_name") && values.containsKey("os_flavor")) {
 					stmt = db.prepareStatement("UPDATE hosts SET os_name = ?, os_flavor = ?, os_sp = '' WHERE hosts.address = ? AND hosts.workspace_id = " + workspaceid);
 					stmt.setString(1, values.get("os_name") + "");
 					stmt.setString(2, values.get("os_flavor") + "");
 					stmt.setString(3, host);
 				}
 				else if (values.containsKey("os_name")) {
 					stmt = db.prepareStatement("UPDATE hosts SET os_name = ?, os_flavor = '', os_sp = '' WHERE hosts.address = ? AND hosts.workspace_id = " + workspaceid);
 					stmt.setString(1, values.get("os_name") + "");
 					stmt.setString(2, host);
 				}
 				else {
 					return new HashMap();
 				}
 
 				Map result = new HashMap();
 				result.put("rows", new Integer(stmt.executeUpdate()));
 				return result;
 			}
 			else {
 				System.err.println("Need to implement: " + methodName);
 			}
 		}
 		catch (Exception ex) {
 			System.err.println(ex);
 			ex.printStackTrace();
 		}
 
 		return new HashMap();
 	}
 }
