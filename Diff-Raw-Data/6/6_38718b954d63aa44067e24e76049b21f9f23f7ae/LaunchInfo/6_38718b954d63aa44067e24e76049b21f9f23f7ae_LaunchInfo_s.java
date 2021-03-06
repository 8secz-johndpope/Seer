 package org.eclipse.core.internal.boot;
 
 /*
  * Licensed Materials - Property of IBM,
  * WebSphere Studio Workbench
  * (c) Copyright IBM Corp 2000
  */
 
 import java.net.*;
 import java.util.*;
 import java.io.*;
 import org.eclipse.core.boot.*;
 import org.eclipse.core.internal.boot.update.BootUpdateManager;
  
 public class LaunchInfo implements IInstallInfo {
 	
 	public static final String PLATFORM_COMPONENT_ID = "org.eclipse.platform";
 	public static final String BOOT_PLUGIN_ID = "org.eclipse.core.boot";	
 
 	private static LaunchInfo profile = null;
 
 	private URL baseUrl;
 	private URL infoUrl;
 	private String id;
 	private String nextId;
 	private String platform;
 	private String app;
 	private String appconfig;
 	private ArrayList configs;
 	private ArrayList configsInact;
 	private ArrayList configsPendingDelete;
 	private HashMap   configsMap;
 	private ArrayList comps;
 	private ArrayList compsInact;
 	private ArrayList compsDang;
 	private ArrayList compsPendingDelete;
 	private ArrayList plugins;
 	private ArrayList pluginsInact;
 	private ArrayList pluginsUnmgd;
 	private ArrayList pluginsPendingDelete;
 	private ArrayList pluginsOld;
 	private ArrayList fragments;
 	private ArrayList fragmentsInact;
 	private ArrayList fragmentsUnmgd;
 	private ArrayList fragmentsPendingDelete;
 	private ArrayList fragmentsOld;
 	private ArrayList status;
 	private int historyCount;
 	private ArrayList historyPendingDelete;
 	private boolean newHistory = false;
 	private boolean isUpdateEnabled = false;
 	private FileOutputStream uos = null;
 	private File uf = null;
 
 	private static final String CONFIGSDIR = "configurations/";
 	private static final String COMPSDIR = "components/";
 	private static final String PLUGINSDIR = "plugins/";
 	private static final String FRAGMENTSDIR = "fragments/";
 	private static final String PLUGINXML = "plugin.xml";
 	private static final String FRAGMENTXML = "fragment.xml";
 	private static final String INSTALL_INFO_DIR = "install/";
 	private static final String LAUNCH_SUMMARY_NAME = "install";
 	private static final String LAUNCH_SUMMARY_EXT = "properties";
 	private static final String LAUNCH_SUMMARY = LAUNCH_SUMMARY_NAME+"."+LAUNCH_SUMMARY_EXT;
 	private static final String LAUNCH_PROFILE_NAME = "update";
 	private static final String LAUNCH_PROFILE_EXT = "cfg";
 	private static final String LAUNCH_PROFILE = LAUNCH_PROFILE_NAME+"."+LAUNCH_PROFILE_EXT;
 	private static final String LAUNCH_PROFILE_CHKPT = "_" + LAUNCH_PROFILE;
 	private static final String LAUNCH_PROFILE_BAK = "__" + LAUNCH_PROFILE;
 	private static final String UPDATE_MARKER = ".update";
 	
 	private static final String ID = "id";
 	private static final String PLATFORM = "runtime";
 	private static final String APP = "application";
 	private static final String APP_CONFIG = "application.configuration";
 	private static final String CONFIG_ACT = "configurations.active";
 	private static final String CONFIG_INACT = "configurations.inactive";
 	private static final String CONFIG_PENDDEL = "configurations.delete";
 	private static final String CONFIG_MAP = "configurations.map";
 	private static final String COMP_ACT = "components.active";
 	private static final String COMP_INACT = "components.inactive";
 	private static final String COMP_DANG = "components.dangling";
 	private static final String COMP_PENDDEL = "components.delete";
 	private static final String PLUGIN_ACT = "plugins.active";
 	private static final String PLUGIN_INACT = "plugins.inactive";
 	private static final String PLUGIN_UNMGD = "plugins.unmanaged";
 	private static final String PLUGIN_PENDDEL = "plugins.delete";
 	private static final String FRAG_ACT = "fragments.active";
 	private static final String FRAG_INACT = "fragments.inactive";
 	private static final String FRAG_UNMGD = "fragments.unmanaged";
 	private static final String FRAG_PENDDEL = "fragments.delete";
 	private static final String HISTORY_COUNT = "history.count";
 	private static final String HISTORY_PENDDEL = "history.delete";
 	private static final String EOF = "eof";
 	private static final String EOF_MARKER = EOF+"="+EOF;
 	private static final int LIST_SIZE = 10;
 
 	private static final String DEFAULT_PLATFORM = "";
 	private static final String DEFAULT_APP = "org.eclipse.ui.workbench";
 	private static final String DEFAULT_APP_CONFIG = "";
 	private static final int DEFAULT_HISTORY_COUNT = 5;
 
 	private static final String URL_FILE = "file";
 	private static final String URL_VA = "valoader";
 
 	private static final String CONFIG_URL = PlatformURLConfigurationConnection.CONFIG_URL_STRING;
 	private static final String COMP_URL = PlatformURLComponentConnection.COMP_URL_STRING;
 
 	// uninstall
 	private static final String UNINSTALLFLAG = "-uninstall";
 	
 	// debug tracing
 	private static final String DEBUGFLAG = "-debug";
 	public static boolean DEBUG = false;
 
 	public static class History {
 		private URL url;
 		private Date date;
 
 		public History(URL url,Date date) {
 			this.url = url;
 			this.date = date;
 		}
 		
 		public History(URL url,String id) {
 			this.url = url;
 			this.date = id==null ? null : new Date(Long.parseLong(id,Character.MAX_RADIX));
 		}
 
 		public String toString() {
 			if (date==null) 
 				return "current";
 			else return date.toString();
 		}
 
 		public URL getLaunchInfoURL() {
 			return url;
 		}
 
 		public Date getLaunchInfoDate() {
 			/**
 			*  Return history profile creation date, or null (if curent profile)
 			*/
 			return date;
 		}
 
 		public String getIdentifier() {
 			/**
 			*  Return history profile creation date, or null (if curent profile)
 			*/
 			if (date==null)
 				return null;
 			else
 				return Long.toString(date.getTime(),Character.MAX_RADIX);
 		}
 
 		public boolean isCurrent() {
 			return date == null;
 		}
 	}
 
 	public static class Status {
 		private String msg;
 		private Throwable exc;
 
 		public Status(String msg) {
 			this.msg = msg;
 			this.exc = null;
 		}
 		
 		public Status(String msg, Throwable exc) {
 			this.msg = msg;
 			this.exc = exc;
 		}
 
 		public String getMessage() {
 			return msg;
 		}
 
 		public Throwable getException() {
 			return exc;
 		}
 	}
 
 	public static class VersionedIdentifier {
 		private String id;
 		private String version;
 		public static final String SEPARATOR = "_";
 
 		public VersionedIdentifier(String s) {
 			if (s==null || (s=s.trim()).equals(""))
 				throw new IllegalArgumentException();
 			int ix = s.lastIndexOf(SEPARATOR);
 			if (ix > 0) {
 				this.id = s.substring(0, ix);
 				this.version = s.substring(ix + 1);
 			}
 			else {
 				this.id = s;
 				this.version = "";
 			}
 		}
 
 		public VersionedIdentifier(String id, String version) {
 			if (id==null || (id=id.trim()).equals("") || version==null)
 				throw new IllegalArgumentException();
 			this.id = id;
 			this.version = version.trim();
 		}
 
 		public String getIdentifier() {
 			return this.id;
 		}
 
 		public String getVersion() {
 			return this.version;
 		}
 
 		public String toString() {
 			return this.version.equals("") ? this.id : this.id + SEPARATOR + this.version;
 		}
 
 		public boolean equals(Object vid) {
 			if (!(vid instanceof VersionedIdentifier)) return false;
 			return equals((VersionedIdentifier) vid);
 		}
 		
 		public boolean equals(VersionedIdentifier vid) {
 			if (!this.id.equals(vid.id)) return false;
 			return this.version.equals(vid.version);
 		}
 	}
 
 	public static interface ListSelector {
 		public List get(LaunchInfo info);
 	}
 
 private LaunchInfo() {
 	setDefaults();	
 }
 
 private LaunchInfo(URL info) throws IOException {
	this(info, getCurrent().baseUrl);
 }
 
 private LaunchInfo(URL info, URL install) throws IOException {
 	super();
	
 	infoUrl = info;
 	baseUrl = install;
 	
 	Properties props = new Properties();
 	InputStream is = null;
 	try {
 		is = infoUrl.openStream();
 		props.load(is);
 		loadProperties(props);
 		if (!EOF.equals(props.getProperty(EOF)))
 			throw new IOException();
 	}
 	finally {
 		if (is!=null) try {is.close();} catch(IOException e) {}
 	}
 }
 
 public LaunchInfo(History history) throws IOException {
 	this(history.getLaunchInfoURL());
 }
 
 public void addStatus(Status[] status) {
 	if (status==null || status.length==0)
 		return;
 	if (this.status == null)
 		this.status = new ArrayList();
 	for (int i=0; i<status.length; i++)
 		this.status.add(status[i]);
 }
 
 public void addStatus(Status status) {
 	if (status==null)
 		return;
 	if (this.status == null)
 		this.status = new ArrayList();
 	this.status.add(status);
 }
 
 synchronized private boolean checkpoint() {
 	if (!isUpdateEnabled()) return false;
 
 	File active = new File(infoUrl.getFile().replace('/', File.separatorChar));
 	File dir = active.getParentFile();
 	if (dir==null) return false; // cannot save
 	dir.mkdirs();		
 	File chkpt = new File(dir,LAUNCH_PROFILE_CHKPT);
 
 	// write temp state
 	PrintWriter os = null;
 	try {
 		os = new PrintWriter(new FileOutputStream(chkpt));
 		write(getIdentifier(), os);
 	}
 	catch(IOException e) {
 		return false;
 	}
 	finally {
 		if (os!=null) os.close();
 	}
 	return true;	
 }
 private void checkRuntimePath() {
 
 	if (pluginsOld.size() != (plugins.size() + pluginsUnmgd.size())) {
 		setNewId();
 		return;
 	}
 
 	for (int i = 0; i < plugins.size(); i++) {
 		if (!pluginsOld.contains(plugins.get(i))) {
 			setNewId();
 			return;
 		}
 	}
 
 	for (int i = 0; i < pluginsUnmgd.size(); i++) {
 		if (!pluginsOld.contains(pluginsUnmgd.get(i))) {
 			setNewId();
 			return;
 		}
 	}
 	
 	if (fragmentsOld.size() != (fragments.size() + fragmentsUnmgd.size())) {
 		setNewId();
 		return;
 	}
 
 	for (int i = 0; i < fragments.size(); i++) {
 		if (!fragmentsOld.contains(fragments.get(i))) {
 			setNewId();
 			return;
 		}
 	}
 
 	for (int i = 0; i < fragmentsUnmgd.size(); i++) {
 		if (!fragmentsOld.contains(fragmentsUnmgd.get(i))) {
 			setNewId();
 			return;
 		}
 	}
 }
 
 private void checkUpdateEnabled() {
 	if (this!=LaunchInfo.getCurrent()) // only allow updates via current LaunchInfo
 		return;
 	if (infoUrl==null) // base must be set
 		return;
 	if (!isFileProtocol(infoUrl)) // must be file URL
 		return;
 	if (!InternalBootLoader.inDevelopmentMode()) {
 		try {	// install must be r/w
 			File f = new File((new URL(infoUrl,UPDATE_MARKER)).getFile().replace('/',File.separatorChar));
 			uf = f;
 			if (f.exists()) {
 				if (!f.delete())
 					return;
 			}
 			byte[] b = EOF.getBytes();
 			uos = new FileOutputStream(f);
 			uos.write(b);
 		} catch(IOException e) {
 			return;
 		}
 	}
 	isUpdateEnabled = true;
 }
 
 private VersionedIdentifier[] computeDelta(String[] list, List active, List inactive, List pendingDelete) {
 	if (active == null)
 		active = new ArrayList();
 	if (inactive == null)
 		inactive = new ArrayList();
 	ArrayList delta = new ArrayList();
 	VersionedIdentifier vid;
 	for (int i = 0; i < list.length; i++) {
 		try {
 			vid = new VersionedIdentifier(list[i]);
 			if (!active.contains(vid) && !inactive.contains(vid) && !pendingDelete.contains(vid))
 				delta.add(vid);
 		}
 		catch(Exception e) { /* skip bad identifiers */ }
 	}
 	if (delta.size() == 0)
 		return new VersionedIdentifier[0];
 
 	VersionedIdentifier[] result = new VersionedIdentifier[delta.size()];
 	delta.toArray(result);
 	if (DEBUG) for (int i=0; i<result.length; i++)
 		debug("   new "+result[i].toString());
 	return result;
 }
 
 private static void debug(String s) {
 	System.out.println("LaunchInfo: "+s);
 }
 
 synchronized public void flush() {
 
 	if (!isUpdateEnabled())
 		return;
 	
 	// flush the current state to disk.
 	checkpoint();
 
 	// check to see if we need to perform cleanup sweep
 	uninstall();
 }
 
 synchronized private VersionedIdentifier[] get(List list) {
 
 	VersionedIdentifier[] result = new VersionedIdentifier[list.size()];
 	list.toArray(result);
 	return result;	
 }
 
 synchronized private VersionedIdentifier[] get(List list1, List list2) {
 	ArrayList temp = new ArrayList(list1.size()+list2.size());
 	temp.addAll(list1);
 	temp.addAll(list2);	
 	VersionedIdentifier[] result = new VersionedIdentifier[temp.size()];
 	temp.toArray(result);
 	return result;	
 }
 
 public String getApplication() {
 
 	return app;	
 }
 
 public String getApplicationConfiguration() {
 
 	return appconfig;	
 }
 /**
  * @see ILaunchInfo#getApplicationConfigurationIdentifier
  */
 public String getApplicationConfigurationIdentifier() {
 	String appCfig = getApplicationConfiguration();
 	if (appCfig != null && appCfig.equals(""))
 		return null;
 	else
 		return appCfig;
 }
 /**
  * @see ILaunchInfo#getComponentInstallURLFor
  */
 public URL getComponentInstallURLFor(String componentId) {
 	if (componentId==null || componentId.trim().equals(""))
 		throw new IllegalArgumentException();
 	try {
 		return new URL(COMP_URL + componentId.trim() + "/");
 	} catch (MalformedURLException e) {
 		throw new IllegalStateException();
 	}
 }
 
 public VersionedIdentifier[] getComponents() {
 	return get(comps);
 }
 /**
  * @see ILaunchInfo#getConfigurationInstallURLFor
  */
 public URL getConfigurationInstallURLFor(String configurationId) {
 	if (configurationId==null || configurationId.trim().equals(""))
 		throw new IllegalArgumentException();
 	try {
 		return new URL(CONFIG_URL + configurationId.trim() + "/");
 	} catch (MalformedURLException e) {
 		throw new IllegalStateException();
 	}
 }
 
 public VersionedIdentifier[] getConfigurations() {
 	return get(configs);
 }
 
 public static LaunchInfo getCurrent() {
 	return profile;
 }
 public URL[] getFragmentPath() {
 
 	ArrayList path = new ArrayList();
 	VersionedIdentifier vid;
 	
 	// include active
 	for (int i=0; i< fragments.size(); i++) {
 		vid = (VersionedIdentifier) fragments.get(i);
 		try {
 			path.add(new URL(baseUrl,FRAGMENTSDIR+vid.toString()+"/"+FRAGMENTXML));
 		}
 		catch (MalformedURLException e) {
 		}
 	}
 
 	// include unmanaged
 	for (int i=0; i< fragmentsUnmgd.size(); i++) {
 		vid = (VersionedIdentifier) fragmentsUnmgd.get(i);
 		try {
 			path.add(new URL(baseUrl,FRAGMENTSDIR+vid.toString()+"/"+FRAGMENTXML));
 		}
 		catch (MalformedURLException e) {
 		}
 	}
 	
 	URL[] result = new URL[path.size()];
 	path.toArray(result);
 	return result;
 
 }
 
 public VersionedIdentifier[] getFragments() {
 	return get(fragments, fragmentsUnmgd);
 }
 /**
  * returns install profile history, sorted from oldest (least recent)
  * to youngest (most recent). Typically the most recent entry is
  * the current profile
  */
  
 private static History[] getHistory(URL url) {
 	
 	if (url==null || !isFileProtocol(url)) return new History[] { new History(url,(Date)null) };
 	
 	File dir = (new File(url.getFile().replace('/',File.separatorChar))).getParentFile();
 	String[] list = null;
 	if (dir != null)
 		list = dir.list();
 	if (list==null) return new History[] { new History(url,(Date)null) };
 
 	Arrays.sort(list);
 	ArrayList result = new ArrayList();
 	History current = null;
 	for (int i=0; i<list.length; i++) {
 		if (list[i].startsWith(LAUNCH_PROFILE_NAME) && list[i].endsWith(LAUNCH_PROFILE_EXT)) {
 			String time = list[i].substring(LAUNCH_PROFILE_NAME.length(),list[i].length()-LAUNCH_PROFILE_EXT.length()-1);
 			Date date = null;
 			try {
 				if (time.length()>0) {
 					time = time.substring(1);
 					date = new Date(Long.parseLong(time,Character.MAX_RADIX));
 				}
 				URL newurl = new URL(url,list[i]);
 				if (time.length()>0)
 					result.add(new History(newurl,date));
 				else
 					current = new History(newurl,(Date)null);
 			}
 			catch(MalformedURLException e) {}
 			catch(NumberFormatException e) {}
 		}
 	}
 
 	if (current != null) result.add(current);
 	History[] array = new History[result.size()];
 	result.toArray(array);
 	return array;
 }
 
 public int getHistoryCount() {
 
 	return historyCount;	
 }
 
 public String getIdentifier() {
 	if (newHistory)
 		return nextId;
 	else
 		return id;	
 }
 /**
  * @see ILaunchInfo#getInstalledComponentIdentifiers
  */
 public String[] getInstalledComponentIdentifiers() {
 	VersionedIdentifier[] c = getComponents();
 	String[] result = new String[c.length];
 	for (int i=0; i<c.length; i++)
 		result[i] = c[i].toString();
 	return result;
 }
 /**
  * @see ILaunchInfo#getInstalledConfigurationIdentifiers
  */
 public String[] getInstalledConfigurationIdentifiers() {
 	VersionedIdentifier[] c = getConfigurations();
 	String[] result = new String[c.length];
 	for (int i=0; i<c.length; i++)
 		result[i] = c[i].toString();
 	return result;
 }
  
 public History[] getLaunchInfoHistory() {
 	return getHistory(infoUrl);
 }
 public URL[] getPluginPath() {
 
 	ArrayList path = new ArrayList();
 	VersionedIdentifier vid;
 	
 	// include active
 	for (int i=0; i< plugins.size(); i++) {
 		vid = (VersionedIdentifier) plugins.get(i);
 		try {
 			path.add(new URL(baseUrl,PLUGINSDIR+vid.toString()+"/"+PLUGINXML));
 		}
 		catch (MalformedURLException e) {
 		}
 	}
 
 	// include unmanaged
 	for (int i=0; i< pluginsUnmgd.size(); i++) {
 		vid = (VersionedIdentifier) pluginsUnmgd.get(i);
 		try {
 			path.add(new URL(baseUrl,PLUGINSDIR+vid.toString()+"/"+PLUGINXML));
 		}
 		catch (MalformedURLException e) {
 		}
 	}
 	
 	URL[] result = new URL[path.size()];
 	path.toArray(result);
 	return result;
 }
 
 public VersionedIdentifier[] getPlugins() {
 	return get(plugins, pluginsUnmgd);
 }
 
 public String getRuntime() {
 
 	return platform;	
 }
 public Status[] getStatus() {
 	if (status != null && status.size() == 0)
 		return null;
 	else
 		return (Status[])status.toArray();
 }
 public boolean hasStatus() {
 	if (status == null || status.size() == 0)
 		return false;
 	else
 		return true;
 }
 public boolean isDanglingComponent(VersionedIdentifier component) {
 	return compsDang.contains(component);
 }
 public void isDanglingComponent(VersionedIdentifier component, boolean isDangling) {
 	
 	if (!comps.contains(component)) {
 		if (!compsInact.contains(component))
 			return;
 	}
 	
 	if (isDangling) {
 		// check to see if we have to add as dangling
 		if (compsDang.contains(component))
 			return;
 		compsDang.add(component);
 	}
 	else {
 		// check to see if we have to remove as dangling
 		compsDang.remove(component);
 	}
 }
 
 private static boolean isFileProtocol(URL u) {
 
 	return URL_FILE.equals(u.getProtocol()) || URL_VA.equals(u.getProtocol());
 
 }
 public boolean isUpdateEnabled() {
 	return isUpdateEnabled;
 }
 
 private ArrayList loadListProperty(Properties props, String name) {
 	return loadListProperty(props,name,VersionedIdentifier.class);
 }
 
 private ArrayList loadListProperty(Properties props, String name, Class type) {
 
 	ArrayList list = new ArrayList();
 	String value = (String) props.get(name+".0");
 	for (int i=1; value != null; i++) {
 		loadListPropertyEntry(list, value, type);
 		value = (String) props.get(name+"."+i);
 	}
 	return list;
 }
 
 private void loadListPropertyEntry(List list, String value, Class type) {
 
 	if (value==null) return;
 	
 	StringTokenizer tokens = new StringTokenizer(value, ",");
 	String token;
 	Object o;
 	while (tokens.hasMoreTokens()) {
 		token = tokens.nextToken().trim();
 		if (!token.equals("")) {
 			try {
 				if (type.equals(VersionedIdentifier.class))
 					o = new VersionedIdentifier(token);
 				else 
 					o = token;
 				list.add(o);
 			}
 			catch (Exception e) { /* skip bad entry */ }
 		}
 	}
 	return;
 }
 
 private HashMap loadMapProperty(Properties props,String name) {
 
 	HashMap map = new HashMap();
 	Iterator i = props.keySet().iterator();
 	while(i.hasNext()) {
 		String key = (String) i.next();
 		if (key.startsWith(name)) {
 			String id = key.substring(name.length()+1);
 			ArrayList mapIds = new ArrayList();
 			loadListPropertyEntry(mapIds,props.getProperty(key),VersionedIdentifier.class);
 			map.put(new VersionedIdentifier(id), mapIds);
 		}
 	}
 	return map;
 }
 
 private void loadProperties(Properties props) {
 		
 		id = props.getProperty(ID,"");
 		if (id.trim().equals(""))
 			id = Long.toString((new java.util.Date()).getTime(),Character.MAX_RADIX);
 		platform = props.getProperty(PLATFORM,DEFAULT_PLATFORM);
 		app = props.getProperty(APP,DEFAULT_APP);
 		if (app.trim().equals(""))
 			app = DEFAULT_APP;
 		appconfig = props.getProperty(APP_CONFIG,DEFAULT_APP_CONFIG);
 		String count = props.getProperty(HISTORY_COUNT,"");
 		try {
 			historyCount = (new Integer(count)).intValue();
 		}
 		catch (Exception e) {
 			historyCount = DEFAULT_HISTORY_COUNT;
 		}
 		historyPendingDelete = loadListProperty(props, HISTORY_PENDDEL, String.class);
 		
 		configs = loadListProperty(props, CONFIG_ACT);
 		configsInact = loadListProperty(props, CONFIG_INACT);
 		configsPendingDelete = loadListProperty(props, CONFIG_PENDDEL);
 		configsMap = loadMapProperty(props, CONFIG_MAP);
 		
 		comps = loadListProperty(props, COMP_ACT);
 		compsInact = loadListProperty(props, COMP_INACT);
 		compsDang = loadListProperty(props, COMP_DANG);
 		compsPendingDelete = loadListProperty(props, COMP_PENDDEL);
 	
 		plugins = loadListProperty(props, PLUGIN_ACT);
 		pluginsInact = loadListProperty(props, PLUGIN_INACT);
 		pluginsUnmgd = new ArrayList();
 		pluginsPendingDelete = loadListProperty(props, PLUGIN_PENDDEL);
 		pluginsOld = loadListProperty(props, PLUGIN_UNMGD);
 		pluginsOld.addAll(plugins);
 	
 		fragments = loadListProperty(props, FRAG_ACT);
 		fragmentsInact = loadListProperty(props, FRAG_INACT);
 		fragmentsUnmgd = new ArrayList();
 		fragmentsPendingDelete = loadListProperty(props, FRAG_PENDDEL);
 		fragmentsOld = loadListProperty(props, FRAG_UNMGD);
 		fragmentsOld.addAll(fragments);
 }
 private static String[] processCommandLine(String[] args) throws Exception {
 	for (int i = 0; i < args.length; i++) {
 		if (args[i].equalsIgnoreCase(DEBUGFLAG)) {
 			DEBUG = true;
 			break;
 		}
 	}
 	return args;
 }
 
 synchronized private void remove(VersionedIdentifier id, List active, List inactive) {
 	if (active.contains(id)) {
 		setNewHistory();
 		active.remove(id);
 		if (!inactive.contains(id))
 			inactive.add(id);
 	}
 }
 
 public void removeComponent(VersionedIdentifier component) {
 	remove(component, comps, compsInact);
 }
 
 public void removeConfiguration(VersionedIdentifier configuration) {	
 	remove(configuration, configs, configsInact);
 }
 
 public void removeFragment(VersionedIdentifier fragment) {	
 	remove(fragment, fragments, fragmentsInact);
 }
 
 public void removePlugin(VersionedIdentifier plugin) {
 	remove(plugin, plugins, pluginsInact);
 }
 
 private static LaunchInfo restoreProfile(URL base) {
 	//make sure we come up using the most accurate information possible
 
 	LaunchInfo li;
 
 	// check for improper shutdown
 	URL info;
 	try {
 		info = new URL(base,INSTALL_INFO_DIR+LAUNCH_PROFILE_CHKPT);
 		li = new LaunchInfo(info,base);
 		if (DEBUG)
 			debug("Using temporary profile "+info.toString());
 		return li;
 	} catch(IOException e) {
 	}
 
 	// look for profile from last shutdown ... this is the normal case
 	try {
 		info = new URL(base,INSTALL_INFO_DIR+LAUNCH_PROFILE);
 		li = new LaunchInfo(info,base);
 		if (DEBUG)
 			debug("Using saved profile "+info.toString());
 		return li;
 	} catch(IOException e) {
 	}
 	
 	// check for backup copy
 	try {
 		info = new URL(base,INSTALL_INFO_DIR+LAUNCH_PROFILE_BAK);
 		li = new LaunchInfo(info,base);
 		if (DEBUG)
 			debug("Using backup profile "+info.toString());
 		return li;
 	} catch(IOException e) {
 	}
 
 	// try history (newest to oldest)
 	try {
 		info = new URL(base,INSTALL_INFO_DIR+LAUNCH_PROFILE);
 		History[] history = getHistory(info);
 		for(int i=history.length-1; i>=0; i--) {
 			try {
 				li = new LaunchInfo(history[i]);
 				li.setNewId();
 				if (DEBUG)
 					debug("Using history profile "+history[i].getIdentifier());
 				return li;
 			} catch(IOException e) {
 			}
 		}
 	} catch(MalformedURLException e) {
 	}
 
 	// we struck out ... come up with default
 	li = new LaunchInfo();
 	try {
 		li.baseUrl = base;
 		li.infoUrl = new URL(base,INSTALL_INFO_DIR+LAUNCH_PROFILE);
 		if (DEBUG)
 			debug("Creating new profile "+li.infoUrl.toString());
 	} catch(MalformedURLException e) {
 		if (DEBUG)
 			debug("Using default profile");
 	}
 	return li;
 }
 synchronized public void revertTo(History history) {
 
 	if (history == null)
 		return;
 	if (!isUpdateEnabled())
 		return;
 
 	// poof up launch info for specified history;
 	LaunchInfo old = null;
 	try {
 		old = new LaunchInfo(history);
 	} catch (IOException e) {
 		return;
 	}
 
 	ArrayList newConfigsInact =
 		revertToInactive(configs, configsInact, old.configs, old.configsInact);
 	ArrayList newCompsInact =
 		revertToInactive(comps, compsInact, old.comps, old.compsInact);
 	ArrayList newPluginsInact =
 		revertToInactive(plugins, pluginsInact, old.plugins, old.pluginsInact);
 	ArrayList newFragmentsInact =
 		revertToInactive(fragments, fragmentsInact, old.fragments, old.fragmentsInact);
 
 	// update current state
 	setNewHistory();
 	platform = old.platform;
 	app = old.app;
 	appconfig = old.appconfig;
 	// historyCount = historyCount; // keep current historyCount
 	configs = old.configs;
 	configsInact = newConfigsInact;
 	comps = old.comps;
 	compsInact = newCompsInact;
 	// compsDang = compsDang; // keep compsDang from current
 	plugins = old.plugins;
 	pluginsInact = newPluginsInact;
 	// pluginsUnmgd = pluginsUnmgd; // keep pluginsUnmgd from current
 	fragments = old.fragments;
 	fragmentsInact = newFragmentsInact;
 	// fragmentsUnmgd = fragmentsUnmgd; // keep fragmentsUnmgd from current
 
 }
 
 private ArrayList revertToInactive(List curAct, List curInact, List oldAct, List oldInact) {
 
 	// start with old inactive list
 	ArrayList inactive = (ArrayList)oldInact;
 	VersionedIdentifier vid;
 
 	// add current inactive
 	for (int i=0; i<curInact.size(); i++) {
 		vid = (VersionedIdentifier) curInact.get(i);
 		if (!inactive.contains(vid))
 			inactive.add(vid);
 	}
 
 	// add current active that are not active in new state
 	for (int i=0; i<curAct.size(); i++) {
 		vid = (VersionedIdentifier) curAct.get(i);
 		if (!oldAct.contains(vid)) {
 			if (!inactive.contains(vid))
 				inactive.add(vid);
 		}
 	}
 	
 	return inactive;
 }
 public static Object run(String flag, String value, String location, String[] args) throws Exception {
 	processCommandLine(args);
 	if (DEBUG) {
 		debug(flag+" "+value);
 	}
 
 	if (flag.equalsIgnoreCase(UNINSTALLFLAG)) {
 		URL cookie = null;	
 		URL base = InternalBootLoader.getInstallURL();	
 		try {
 			cookie = new URL("file",null,0,value);
 		}
 		catch(MalformedURLException e) {
 			return null;
 		}
 		startup(base);
 		BootUpdateManager.uninstall(cookie);
 	}
 	return null;
 }
 synchronized private void set(VersionedIdentifier id, List active, List inactive) {
 	if (id == null)
 		return;
 	for (int i = 0; i < active.size(); i++) {
 		VersionedIdentifier vid = (VersionedIdentifier) active.get(i);
 		if (vid.getIdentifier().equals(id.getIdentifier())) {
 			if (vid.getVersion().equals(id.getVersion()))
 				return; // same identifier already active ... do nothing
 
 			active.remove(i); // different version found ... replace it
 			active.add(i,id);
 			if (!inactive.contains(vid))
 				inactive.add(vid);
 			inactive.remove(id);
 			setNewHistory();
 			return;
 		}
 	}
 	active.add(id); // did not exist ... add it
 	setNewHistory();
 }
 
 private void setApplication(String app) {
 	if (this.app!=null && !this.app.equals(app)) {
 		setNewHistory();
 		if (app!=null) this.app = app;
 		else this.app = DEFAULT_APP;
 	}
 }
 /**
  * @param appconfig is the directory identifier (incl. version suffix)
  * of the "dominant" application configuration.
  */	
 private void setApplicationConfiguration(String appconfig) {
 	if (this.appconfig!=null && !this.appconfig.equals(appconfig)) {
 		setNewHistory();
 		if (appconfig!=null) this.appconfig = appconfig;
 		else this.appconfig = DEFAULT_APP_CONFIG;
 	}
 }
 public void setComponent(VersionedIdentifier component) {
 	set(component, comps, compsInact);
 }
 
 public void setConfiguration(VersionedIdentifier config) {
 	set(config, configs, configsInact);
 }
 /*
  * called after any new configs, components and plugins are processed
  */
 private void setDefaultRuntime() {
 
 	boolean found = false;
 	if (getRuntime().equals(DEFAULT_PLATFORM)) {
 		VersionedIdentifier vid;
 		// check active list for runtime
 		for (int i=0; i< plugins.size(); i++) {
 			vid = (VersionedIdentifier) plugins.get(i);
 			if (vid.getIdentifier().equals(BOOT_PLUGIN_ID)) {
 				setRuntime(vid.toString());
 				found = true;
 				break;
 			}
 		}
 
 		if (!found) {
 			// check unmanaged list for runtime
 			for (int i=0; i< pluginsUnmgd.size(); i++) {
 				vid = (VersionedIdentifier) pluginsUnmgd.get(i);
 				if (vid.getIdentifier().equals(BOOT_PLUGIN_ID)) {
 					setRuntime(vid.toString());
 					found = true;
 					break;
 				}
 			}
 		}
 		
 	}
 }
 
 private void setDefaults() {
 	setNewId();
 	platform = DEFAULT_PLATFORM;
 	app = DEFAULT_APP;
 	appconfig = DEFAULT_APP_CONFIG;
 	historyCount = DEFAULT_HISTORY_COUNT;	
 	historyPendingDelete = new ArrayList();
 	
 	configs = new ArrayList();
 	configsInact = new ArrayList();
 	configsPendingDelete = new ArrayList();
 	configsMap = new HashMap();
 	
 	comps = new ArrayList();
 	compsInact = new ArrayList();
 	compsDang = new ArrayList();
 	compsPendingDelete = new ArrayList();
 	
 	plugins = new ArrayList();
 	pluginsInact = new ArrayList();
 	pluginsUnmgd = new ArrayList();
 	pluginsPendingDelete = new ArrayList();
 	pluginsOld = new ArrayList();
 	
 	fragments = new ArrayList();
 	fragmentsInact = new ArrayList();
 	fragmentsUnmgd = new ArrayList();
 	fragmentsPendingDelete = new ArrayList();
 	fragmentsOld = new ArrayList();
 }
 
 public void setFragment(VersionedIdentifier fragment) {
 	set(fragment, fragments, fragmentsInact);
 }
 
 public void setHistoryCount(int count) {
 
 	if (count<=0)
 		historyCount = DEFAULT_HISTORY_COUNT;
 	else
 		historyCount = count;
 }
 /**
  * This method is called by the BootUpdateManager with a list of
  * configurations, components, plugins and fragments that could not
  * be installed (after they were discovered). If these items
  * do not exist on an active or inactive list already, they are added
  * to the corresponding inactive list.
  */
 synchronized public void setInactive(
 	VersionedIdentifier[] configId,
 	VersionedIdentifier[] compId,
 	VersionedIdentifier[] pluginId,
 	VersionedIdentifier[] fragId) {
 
 	VersionedIdentifier vid;
 
 	for (int i = 0; configId != null && i < configId.length; i++) {
 	    vid = (VersionedIdentifier) configId[i];
 	    configs.remove(vid);
 	    if (!configsInact.contains(vid)) configsInact.add(vid);
 	}
 	
 	for (int i = 0; compId != null && i < compId.length; i++) {
 	    vid = (VersionedIdentifier) compId[i];
 	    comps.remove(vid);
 	    if (!compsInact.contains(vid)) compsInact.add(vid);
 	}
 	
 	for (int i = 0; pluginId != null && i < pluginId.length; i++) {
 	    vid = (VersionedIdentifier) pluginId[i];
 	    plugins.remove(vid);
 	    if (!pluginsInact.contains(vid)) pluginsInact.add(vid);
 	}
 	
 	for (int i = 0; fragId != null && i < fragId.length; i++) {
 	    vid = (VersionedIdentifier) fragId[i];
 	    fragments.remove(vid);
 	    if (!fragmentsInact.contains(vid)) fragmentsInact.add(vid);
 	}
 }
 
 private void setNewHistory() {
 	if (newHistory)
 		return;
 	nextId = Long.toString((new java.util.Date()).getTime(),Character.MAX_RADIX);
 	newHistory = true;
 }
 
 private void setNewId() {
 	id = Long.toString((new java.util.Date()).getTime(),Character.MAX_RADIX);
 }
 
 public void setPlugin(VersionedIdentifier plugin) {
 	set(plugin, plugins, pluginsInact);
 }
 /**
  * @param platform is the directory identifier (incl. version suffix)
  * of the plugins subdirectory containing boot.jar. At install time
  * it is conained in a component identifier by 
  * LaunchInfo.PLATFORM_COMPONENT_ID and has the base directory 
  * name (no version suffix) identified by
  * LaunchInfo.BOOT_PLUGIN_ID
  */	
 public void setRuntime(String platform) {
 	if (this.platform!=null && !this.platform.equals(platform)) {
 		setNewHistory();
 		if (platform!=null) this.platform = platform;
 		else this.platform = DEFAULT_PLATFORM;
 	}
 }
 
 static void shutdown() {
 	if (profile==null) return;
 	try {
 		profile.store();
 	}
 	catch(IOException e) {
 		// was not able to save updated install profile
 	}
 	finally {
 		if (profile.uos!=null) {
 			try {
 				profile.uos.close();
 			} catch(IOException e) {
 			}
 		}
 		if (profile.uf!=null)
 			profile.uf.delete();
 	}
 }
 static void startup(URL base) {
 
 	if (profile == null) {
 		// restore profile
 		profile = restoreProfile(base);
 
 		// check if update will be enabled
 		profile.checkUpdateEnabled();		
 		if (DEBUG)
 			debug("Update mode "+(profile.isUpdateEnabled()?"enabled":"disabled"));
 		if (!profile.isUpdateEnabled())
 			return;
 
 		// clean up any pending deletes
 		profile.uninstallPendingDelete();
 
 		// detect changes from last startup
 		String path;
 		File dir;
 		String[] list;
 		FilenameFilter filter = new FilenameFilter() {
 			public boolean accept(File dir, String name) {
 				return (new File(dir, name)).isDirectory();
 			}
 		};
 
 		// look for configurations
 		path =
 			(base.getFile() + INSTALL_INFO_DIR + CONFIGSDIR).replace(
 				'/',
 				File.separatorChar);
 		dir = new File(path);
 		list = dir.list(filter);
 		if (DEBUG) debug("Detecting configuration changes");
 		VersionedIdentifier[] configDelta;
 		if (list == null)
 			configDelta = new VersionedIdentifier[0];
 		else {
 			profile.synchConfigurations(list);
 			configDelta = profile.computeDelta(list, profile.configs, profile.configsInact, profile.configsPendingDelete);
 		}
 
 		// look for components	
 		path =
 			(base.getFile() + INSTALL_INFO_DIR + COMPSDIR).replace('/', File.separatorChar);
 		dir = new File(path);
 		list = dir.list(filter);
 		if (DEBUG) debug("Detecting component changes");
 		VersionedIdentifier[] compDelta;
 		if (list == null)
 			compDelta = new VersionedIdentifier[0];
 		else {
 			profile.synchComponents(list);
 			compDelta = profile.computeDelta(list, profile.comps, profile.compsInact, profile.compsPendingDelete);
 		}
 
 		// complete "installation" of new configurations and components	
 		if (configDelta.length > 0 || compDelta.length > 0) 
 			profile.addStatus(BootUpdateManager.install(configDelta, compDelta));
 				
 		// look for plugins	
 		path =
 			(base.getFile() + PLUGINSDIR).replace('/', File.separatorChar);
 		dir = new File(path);
 		list = dir.list(filter);
 		if (DEBUG) debug("Detecting plugin changes");
 		VersionedIdentifier[] pluginDelta;
 		if (list == null)
 			pluginDelta = new VersionedIdentifier[0];
 		else {
 			profile.synchPlugins(list);
 			pluginDelta = profile.computeDelta(list, profile.plugins, profile.pluginsInact, profile.pluginsPendingDelete);
 		}
 		for (int i=0; i<pluginDelta.length; i++)
 			profile.pluginsUnmgd.add(pluginDelta[i]);
 				
 		// look for fragments	
 		path =
 			(base.getFile() + FRAGMENTSDIR).replace('/', File.separatorChar);
 		dir = new File(path);
 		list = dir.list(filter);
 		if (DEBUG) debug("Detecting fragment changes");
 		VersionedIdentifier[] fragmentDelta;
 		if (list == null)
 			fragmentDelta = new VersionedIdentifier[0];
 		else {
 			profile.synchFragments(list);
 			fragmentDelta = profile.computeDelta(list, profile.fragments, profile.fragmentsInact, profile.fragmentsPendingDelete);
 		}
 		for (int i=0; i<fragmentDelta.length; i++)
 			profile.fragmentsUnmgd.add(fragmentDelta[i]);
 
 		// check to see if runtime is set
 		if (profile.getRuntime().equals(DEFAULT_PLATFORM)) {
 			profile.setDefaultRuntime();
 		}
 
 		// try to see if we need to do cleanup pass
 		if (profile.newHistory)
 			profile.checkpoint();
 		profile.uninstall();
 
 		// check if runtime path has changed
 		profile.checkRuntimePath();
 	}
 }
 
 synchronized private void store() throws IOException {
 	if (!isUpdateEnabled())
 		return;
 
 	File active = new File(infoUrl.getFile().replace('/', File.separatorChar));
 	File dir = active.getParentFile();
 	if (dir==null) return; // cannot save
 	dir.mkdirs();
 	File chkpt = new File(dir,LAUNCH_PROFILE_CHKPT);
 	File bak = new File(dir,LAUNCH_PROFILE_BAK);
 
 	// write out temp state
 	if (!checkpoint())
 		return;	
 
 	// check to see if we need to clone history
 	if (newHistory) {
 		if (active.exists()) {
 			String suffix = this.id; // id for history
 			File history = new File(dir,LAUNCH_PROFILE_NAME+"_"+suffix+"."+LAUNCH_PROFILE_EXT);
 			if (history.exists()) {
 				if (!history.delete())
 					return;
 			}
 			if (!active.renameTo(history))
 				return; 
 		}
 	}
 	else {
 		if (bak.exists()) {
 			if (!bak.delete())
 				return;
 		}
 		if (!active.renameTo(bak))
 			return;
 	}
 
 	// activate new state
 	if (!chkpt.renameTo(active))
 		return;
 	bak.delete();
 
 	// write out launcher summary
 	File summary = new File(dir,LAUNCH_SUMMARY);
 	PrintWriter sum = null;
 	try {
 		// write summary
 		sum = new PrintWriter(new FileOutputStream(summary));
 		writeSummary(getIdentifier(), sum);
 	}
 	finally {
 		if (sum!=null) sum.close();
 	}
 }
 private synchronized void synch(List dirlist, List infoList) {
 	// remove state entries that do not exist in file system
 	List list = new ArrayList(infoList); // clone list
 	for (int i=0; i<list.size(); i++) {
 		VersionedIdentifier vid = (VersionedIdentifier) list.get(i);
 		if (!dirlist.contains(vid.toString())) {
 			infoList.remove(vid);
 			if (DEBUG)
 				debug("   missing "+vid.toString());
 		}
 	}
 }
 
 private void synchComponents(String[] dirlist) {
 	List list = Arrays.asList(dirlist);
 	synch(list, comps);
 	synch(list, compsInact);
 	synch(list, compsDang);
 	synch(list, compsPendingDelete);
 }
 
 private void synchConfigurations(String[] dirlist) {	
 	List list = Arrays.asList(dirlist);
 	synch(list, configs);
 	synch(list, configsInact);
 	synch(list, configsPendingDelete);
 }
 
 private void synchFragments(String[] dirlist) {
 	List list = Arrays.asList(dirlist);
 	synch(list, plugins);
 	synch(list, pluginsInact);
 	synch(list, pluginsUnmgd);
 	synch(list, pluginsPendingDelete);
 }
 
 private void synchPlugins(String[] dirlist) {
 	List list = Arrays.asList(dirlist);
 	synch(list, fragments);
 	synch(list, fragmentsInact);
 	synch(list, fragmentsUnmgd);
 	synch(list, fragmentsPendingDelete);
 }
 
 public static void todo() {
 /*
 	* delta computation ... ignore pendingDelete, compute additions, deletions
 	*
 	* launchInfo should not do discovery/etc in a R/O install tree (only read the profile and go, must include unmanaged items)
 	*    also should have a mechanism for detecting update in flight (? ... only rewriting the profile)
 	*
 	* detecting bad failure and recovering from it	*
 	* need to hook in error recovery that recomputes all from scratch (when I can't trust state)
 	* ... but how do we know which plugins comps should be inactive vs. be reinstalled?
 	*
 	* batch uninstall
 	*
 	* time stamps on debug(), not on debugDetail()
 	*
 	* "reliability" sweep ... handling hard errors at critical points in processing
 	*    - harden state on flush (but only create new history on shutdown)
 	*    - write pending deletes before deleting
 	*    - startup - cleanup lists wrt. file state (computeAdd/ computeMissing)
 	*    - startup - bad config.properties
 	*    - backup for install.properties (needs to be manually renamed)
 	*    - startup - no state (recompute from scratch)
 */
 }
 synchronized public void uninstall() {
 
 	// do history-based deletion sweep
 	if (!isUpdateEnabled())
 		return;
 
 	History[] history = getLaunchInfoHistory();
 	if (history.length <= (historyCount + 1))
 		return;
 
 	// poof up launch info objects
 	LaunchInfo[] historyInfo = new LaunchInfo[history.length];
 	for (int i = 0; i < history.length; i++) {
 		if (history[i].isCurrent())
 			historyInfo[i] = LaunchInfo.getCurrent();
 		else {
 			try {
 				historyInfo[i] = new LaunchInfo(history[i]);
 			} catch(IOException e) {
 				historyInfo[i] = new LaunchInfo();
 			}
 		}
 	}
 
 	// determine list of deletion candidates
 	List candidateConfigs = new ArrayList();
 	List candidateComps = new ArrayList();
 	List candidatePlugins = new ArrayList();
 	List candidateFragments = new ArrayList();
 
 	for (int i = 0; i < (history.length - (historyCount + 1)); i++) {
 		uninstallGetCandidates(
 			candidateConfigs,
 			historyInfo[i].configs,
 			historyInfo[i].configsInact);
 		uninstallGetCandidates(
 			candidateComps,
 			historyInfo[i].comps,
 			historyInfo[i].compsInact);
 		uninstallGetCandidates(
 			candidatePlugins,
 			historyInfo[i].plugins,
 			historyInfo[i].pluginsInact);
 		uninstallGetCandidates(
 			candidateFragments,
 			historyInfo[i].fragments,
 			historyInfo[i].fragmentsInact);
 	}
 
 	// determine which candidates are not active in recent histories
 	List deleteConfigs =
 		uninstallMarkForDeletion(candidateConfigs, historyInfo, new ListSelector() {
 		public List get(LaunchInfo i) {
 			return i.configs;
 		}
 	});
 	List deleteComps =
 		uninstallMarkForDeletion(candidateComps, historyInfo, new ListSelector() {
 		public List get(LaunchInfo i) {
 			return i.comps;
 		}
 	});
 	List deletePlugins =
 		uninstallMarkForDeletion(candidatePlugins, historyInfo, new ListSelector() {
 		public List get(LaunchInfo i) {
 			return i.plugins;
 		}
 	});
 	List deleteFragments =
 		uninstallMarkForDeletion(candidateFragments, historyInfo, new ListSelector() {
 		public List get(LaunchInfo i) {
 			return i.fragments;
 		}
 	});
 		
 	if (deleteConfigs.size() <= 0
 		&& deleteComps.size() <= 0
 		&& deletePlugins.size() <= 0
 		&& deleteFragments.size() <= 0)
 		return;
 
 	// update state prior to deletion and harden it
 	uninstallPendingDelete(deleteConfigs, configsInact, configsPendingDelete);
 	uninstallPendingDelete(deleteComps, compsInact, compsPendingDelete);
 	uninstallPendingDelete(deletePlugins, pluginsInact, pluginsPendingDelete);
 	uninstallPendingDelete(deleteFragments, fragmentsInact, fragmentsPendingDelete);
 	uninstallPendingDelete(history);
 	if (!checkpoint())
 		return;
 
 	// delete files
 	uninstall(history, 0, history.length - (historyCount + 1));
 	uninstall(deleteConfigs, deleteComps, deletePlugins, deleteFragments);
 	checkpoint();
 }
 
 private synchronized void uninstall(History[] history, int from, int to) {
 	for (int i = from; i < to; i++) {
 		if (history[i].isCurrent())
 			continue; // just in case
 		if (DEBUG)
 			debug("Removing history " + history[i].getIdentifier() + " [" + history[i].getLaunchInfoDate().toString() + "]");
 		File info =
 			new File(
 				history[i].getLaunchInfoURL().getFile().replace('/', File.separatorChar));
 		if (!info.exists() || uninstall(info))
 			historyPendingDelete.remove(history[i].getIdentifier());
 	}
 }
 
 private boolean uninstall(File f) {
 
 	if (f.isDirectory()) {
 		File[] list = f.listFiles();
 		if (list!=null) {
 			for (int i=0; i<list.length; i++)
 				uninstall(list[i]);
 		}
 	}
 	
 	boolean ok = f.delete();
 	if (DEBUG)
 		debug((ok?"Unistalled ":"Unable to uninstall ")+f.toString());
 	return ok;
 }
 
 synchronized private void uninstall(List configId, List compId, List pluginId, List fragId) {
 
 	if (!isUpdateEnabled())
 		return;
 	
 	String root = baseUrl.getFile().replace('/',File.separatorChar);
 	File dir;
 	
 	// uninstall configurations
 	for (int i=0; i<configId.size(); i++) {
 		if (DEBUG) debug("Removing configuration "+configId.get(i).toString());
 		dir = new File(root+INSTALL_INFO_DIR+CONFIGSDIR+configId.get(i).toString()+File.separator);
 		if (!dir.exists() || uninstall(dir))
 			configsPendingDelete.remove(configId.get(i));		
 	}
 
 	// unistall components
 	for (int i=0; i<compId.size(); i++) {
 		if (DEBUG) debug("Removing component "+compId.get(i).toString());
 		dir = new File(root+INSTALL_INFO_DIR+COMPSDIR+compId.get(i).toString()+File.separator);
 		if (!dir.exists() || uninstall(dir))
 			compsPendingDelete.remove(compId.get(i));
 	}
 
 	// uninstall plugins
 	for (int i=0; i<pluginId.size(); i++) {
 		if (DEBUG) debug("Removing plugin "+pluginId.get(i).toString());
 		dir = new File(root+PLUGINSDIR+pluginId.get(i).toString()+File.separator);
 		if (!dir.exists() || uninstall(dir))
 			pluginsPendingDelete.remove(pluginId.get(i));
 	}
 
 	// uninstall fragments
 	for (int i=0; i<fragId.size(); i++) {
 		if (DEBUG) debug("Removing fragment "+fragId.get(i).toString());
 		dir = new File(root+FRAGMENTSDIR+fragId.get(i).toString()+File.separator);
 		if (!dir.exists() || uninstall(dir))
 			fragmentsPendingDelete.remove(fragId.get(i));
 	}
 }
 
 private void uninstallGetCandidates(List candidates, List active, List inactive) {
 	for (int i=0; i<active.size(); i++) {
 		if (!candidates.contains(active.get(i)))
 			candidates.add(active.get(i));
 	}
 	for (int i=0; i<inactive.size(); i++) {
 		if (!candidates.contains(inactive.get(i)))
 			candidates.add(inactive.get(i));
 	}
 }
 
 private List uninstallMarkForDeletion(List candidates, LaunchInfo[] history, ListSelector active) {
 	List delete = new ArrayList();
 	Iterator list = candidates.iterator();
 	while(list.hasNext()) {
 		boolean found = false;
 		VersionedIdentifier vid = (VersionedIdentifier) list.next();
 		for (int i=history.length-(historyCount+1); i<history.length; i++) {
 			if (active.get(history[i]).contains(vid)) {
 				found = true;
 				break;
 			}
 		}
 		if (!found) delete.add(vid);
 	}
 	return delete;
 }
 private synchronized void uninstallPendingDelete() {
 
 	if (baseUrl != null && historyPendingDelete.size() > 0) {
 		ArrayList history = new ArrayList();
 		for (int i=0; i<historyPendingDelete.size(); i++) {
 			String id = (String) historyPendingDelete.get(i);
 			URL prof;
 			try {
 				prof = new URL(baseUrl,INSTALL_INFO_DIR+LAUNCH_PROFILE_NAME+"_"+id+"."+LAUNCH_PROFILE_EXT);
 				history.add(new History(prof,id));
 			}
 			catch(MalformedURLException e) {
 			}
 		}
 		History[] historyArray = new History[history.size()];
 		history.toArray(historyArray);
 		uninstall(historyArray, 0, historyArray.length);
 	}
 
 	if (configsPendingDelete.size() > 0
 		|| compsPendingDelete.size() > 0
 		|| pluginsPendingDelete.size() > 0
 		|| fragmentsPendingDelete.size() > 0) {
 	        ArrayList configsToDelete = new ArrayList(configsPendingDelete); // need to clone list
 	        ArrayList compsToDelete = new ArrayList(compsPendingDelete);
 	        ArrayList pluginsToDelete = new ArrayList(pluginsPendingDelete);
 	        ArrayList fragsToDelete = new ArrayList(fragmentsPendingDelete);
 	        uninstall(configsToDelete, compsToDelete, pluginsToDelete, fragsToDelete);
 	}
 }
 
 private void uninstallPendingDelete(History[] history) {	
 	for (int i = 0; i < (history.length - (historyCount + 1)); i++) {
 		String id = history[i].getIdentifier();
 		if (!historyPendingDelete.contains(id))
 			historyPendingDelete.add(id);
 	}
 }
 
 private void uninstallPendingDelete(List delete, List inactive, List pending) {
 	for (int i=0; i< delete.size(); i++) {
 		Object o = delete.get(i);
 		inactive.remove(o);
 		if (!pending.contains(o))
 			pending.add(o);
 	}
 }
 
 synchronized private void write(String id, PrintWriter w) throws IOException {
 
 		w.println(ID+"="+id);
 		w.println(PLATFORM+"="+platform);
 		w.println(APP+"="+app);
 		w.println(APP_CONFIG+"="+appconfig);
 		w.println(HISTORY_COUNT+"="+Integer.toString(historyCount));
 		writeList(w, historyPendingDelete, HISTORY_PENDDEL);
 		
 		writeList(w, configs, CONFIG_ACT);
 		writeList(w, configsInact, CONFIG_INACT);
 		writeList(w, configsPendingDelete, CONFIG_PENDDEL);
 		writeMap (w, configsMap, CONFIG_MAP);
 		writeList(w, comps, COMP_ACT);
 		writeList(w, compsInact, COMP_INACT);
 		writeList(w, compsDang, COMP_DANG);
 		writeList(w, compsPendingDelete, COMP_PENDDEL);
 		writeList(w, plugins, PLUGIN_ACT);
 		writeList(w, pluginsInact, PLUGIN_INACT);
 		writeList(w, pluginsUnmgd, PLUGIN_UNMGD);
 		writeList(w, pluginsPendingDelete, PLUGIN_PENDDEL);
 		writeList(w, fragments, FRAG_ACT);
 		writeList(w, fragmentsInact, FRAG_INACT);
 		writeList(w, fragmentsUnmgd, FRAG_UNMGD);
 		writeList(w, fragmentsPendingDelete, FRAG_PENDDEL);
 
 		w.println(EOF_MARKER);
 }
 
 private void writeList(PrintWriter w, List list, String id) throws IOException {
 	
 	if (list==null || list.size() <= 0) return;
 	for (int i=0; LIST_SIZE*i<list.size(); i++) {
 		String prop = "";
 		for (int j=0; j<10 && LIST_SIZE*i+j<list.size() ; j++) {
 			if (j!=0) prop += ",";
 			prop += list.get(LIST_SIZE*i+j).toString();
 		}
 		w.println(id+"."+i+"="+prop);
 	}
 }
 
 private void writeMap(PrintWriter w, Map map, String id) throws IOException {
 
 	if (map==null) return;
 	Iterator keys = map.keySet().iterator();
 	while(keys.hasNext()) {
 		VersionedIdentifier key = (VersionedIdentifier) keys.next();
 		List list = (List) map.get(key);
 		if (list!=null && list.size()>0) {
 			String prop = "";
 			for (int i=0; i<list.size(); i++) {
 				if (i!=0) prop += ",";
 				prop += list.get(i).toString();
 			}
 			w.println(id+"."+key.toString()+"="+prop);
 		}
 	}
 }
 
 synchronized private void writeSummary(String id, PrintWriter w) throws IOException {
 	w.println(ID+"="+id);
 	w.println(PLATFORM+"="+platform);
 	w.println(APP+"="+app);
 	w.println(APP_CONFIG+"="+appconfig);
 }
 }
