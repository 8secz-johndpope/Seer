 /**
  * Start time:13:40:09 2009-04-13<br>
  * Project: mobicents-jainslee-server-core<br>
  * 
  * @author <a href="mailto:baranowb@gmail.com">baranowb - Bartosz Baranowski
  *         </a>
  * @author <a href="mailto:brainslog@gmail.com"> Alexandre Mendonca </a>
  */
 package org.mobicents.slee.container.component.security;
 
 import java.io.File;
 import java.io.FileInputStream;
 import java.io.FilePermission;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.InputStreamReader;
 import java.lang.reflect.Constructor;
 import java.lang.reflect.InvocationTargetException;
 import java.lang.reflect.Method;
 import java.net.MalformedURLException;
 import java.net.URI;
 import java.net.URL;
 import java.security.AccessController;
 import java.security.CodeSource;
 import java.security.KeyStore;
 import java.security.Permission;
 import java.security.PermissionCollection;
 import java.security.Permissions;
 import java.security.Policy;
 import java.security.Principal;
 import java.security.PrivilegedAction;
 import java.security.PrivilegedExceptionAction;
 import java.security.ProtectionDomain;
 import java.security.UnresolvedPermission;
 import java.security.cert.Certificate;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Enumeration;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.Map;
 import java.util.Properties;
 import java.util.PropertyPermission;
 import java.util.Set;
 import java.util.concurrent.atomic.AtomicReference;
 
 import org.apache.log4j.Logger;
 
 import sun.security.provider.PolicyParser;
 import sun.security.provider.PolicyParser.GrantEntry;
 import sun.security.provider.PolicyParser.PermissionEntry;
 import sun.security.util.SecurityConstants;
 
 /**
  * Start time:13:40:09 2009-04-13<br>
  * Project: mobicents-jainslee-server-core<br>
  * 
  * @author <a href="mailto:baranowb@gmail.com">baranowb - Bartosz Baranowski
  *         </a>
  */
 public class PolicyFile extends Policy {
 	//FIXME: should this be private class?
 	private static final Logger logger = Logger.getLogger(PolicyFile.class);
 
 	// FIXME: make PermissionHolder default container??
 	private AtomicReference<GlobalPolicyHolder> currentPolicy = new AtomicReference<GlobalPolicyHolder>(new GlobalPolicyHolder());
 
 	private final Set<PermissionHolder> sleeComponentsPermissions = new HashSet<PermissionHolder>();
 
 
 	// Some statics
 	private static String _DEFAULT_POLICY;
 	private static String _DEFAULT_USER_POLICY;
 	private final static String _POLICY_URL_PART = "policy.url.";
 	private static final String _POLICY_PROPERTY = "java.security.policy";
 
 	// no args
 	private static final Class[] _PARAMS_0 = {};
 	// name
 	private static final Class[] _PARAMS_1 = { String.class };
 	// name,action
 	private static final Class[] _PARAMS_2 = { String.class, String.class };
 
 	private final static String _PROTOCOL_FILE = "file";
 	private final static String _PROTOCOL_FILE_PREFIX = "file://";
 	static {
 
 		AccessController.doPrivileged(new PrivilegedAction() {
 
 			public Object run() {
 				Properties systemProperties = System.getProperties();
 				_DEFAULT_POLICY = (_PROTOCOL_FILE_PREFIX + systemProperties.getProperty("java.home") + File.separator + "lib" + File.separator + "security" + File.separator + "java.policy").replace(
 						"\\", File.separator).replace(" ", "\\ ");
 
 				_DEFAULT_USER_POLICY = (_PROTOCOL_FILE_PREFIX + systemProperties.getProperty("user.home") + File.separator + ".java.policy").replace("\\", File.separator).replace(" ", "\\ ");
 				return null;
 			}
 
 		});
 
 	}
 
 	@Override
 	public PermissionCollection getPermissions(CodeSource codesource) {
 
 		Permissions p = new Permissions();
 		// We must always return muttable coolection
 		//FIXME: shoyuld this be done ni priviledged section?
 		codesource = performUrlConversion(codesource, true);
 		p = (Permissions) this.getPermissions(p, codesource);
 	
 		return p;
 
 	}
 
 	@Override
 	public PermissionCollection getPermissions(ProtectionDomain domain) {
 		Permissions permissions = new Permissions();
 
 		if (domain == null)
 			return permissions;
 
 		getPermissions(permissions, domain);
 		PermissionCollection pc = domain.getPermissions();
 		if (pc != null) {
 			Enumeration domainPermissions = pc.elements();
 			while (domainPermissions.hasMoreElements()) {
 				permissions.add((Permission) domainPermissions.nextElement());
 			}
 		}
 	
 
 		return permissions;
 	}
 
 	private void doPermDump(PermissionCollection pc, String string) {
 	
 		
 		Enumeration<Permission> en = pc.elements();
 		while (en.hasMoreElements()) {
 			Permission p = en.nextElement();
 			logger.info(string + "===>P:" + p.getClass() + " N:" + p.getName() + " A:" + p.getActions());
 		}
 
 	}
 
 	private PermissionCollection getPermissions(Permissions permissions, CodeSource codesource) {
 
 		// Add some actions here?
 		return this.getPermissions(permissions, codesource, null);
 	}
 
 	// generic, for all calls
 	private Permissions getPermissions(Permissions permissions, final CodeSource cs, Principal[] principals) {
 
 		List<PolicyHolderEntry> entries = this.currentPolicy.get().policyHolderEntries;
 
 		for (PolicyHolderEntry phe : entries) {
 
 			// general
 			selectPermissions(permissions, cs, principals, phe);
 
 			// FIXME: certs?
 
 		}
 
 		return permissions;
 	}
 
 	private void getPermissions(Permissions permissions, ProtectionDomain domain) {
 		CodeSource cs = domain.getCodeSource();
 		if (cs == null)
 			return;
 		//FIXME: should we 
 		cs = performUrlConversion(cs, true);
 		// FIXME: add more actions?
 		getPermissions(permissions, cs, domain.getPrincipals());
 
 	}
 
 	private void selectPermissions(Permissions permissions, final CodeSource cs, Principal[] principals, final PolicyHolderEntry phe) {
 
 		//FIXME: this always gets into if... even if debug is not on...
 //		 if (logger.isDebugEnabled() ) {
 //		
 //			logger.info("Select permissions. \nFor: " + cs + ", Entry\n CS: " + phe.getCodeSource() + "\n" + phe.getCodeSource().implies(cs));
 //		 }
 
 		Boolean implies = phe.getCodeSource().implies(cs);
 		// Boolean implies = (Boolean) AccessController.doPrivileged(new
 		// ImpliesAction(cs,phe));
 		// System.err.println("I["+implies+"]  For: " + cs + ", Entry CS: " +
 		// phe.getCodeSource());
 		boolean addPermissions = false;
 		if (!implies.booleanValue()) {
 			
 			return;
 		} else {
 			
 			final Principal[] pdp = principals;
 			List entryPs = phe.getPrincipals();
 			List domainPs = new LinkedList();
 			// if (pdp != null && pdp.length != 0) {
 			// PolicyParser.PrincipalEntry pe = null;
 			// for (int j = 0; j < pdp.length; j++) {
 			// pe = new PolicyParser.PrincipalEntry(pdp[j].getClass().getName(),
 			// pdp[j].getName());
 			// domainPs.add(pe);
 			// }
 			// }
 
 			if (entryPs == null || entryPs.size() == 0) {
 				
 				addPermissions = true;
 			} else if (domainPs.size() != 0) {
 				
 				// FIXME: what goes here?
 			}
 
 			// implies succeeded - grant the permissions
 			if (!addPermissions) {
 		
 			} else {
 				
 				List<Permission> permissionsList = phe.getPermissions();
 				for (int j = 0; j < permissionsList.size(); j++) {
 					Permission p = permissionsList.get(j);
 					
 
 					permissions.add(p);
 					
 				}
 			}
 		}
 
 	}
 
 	@Override
 	public boolean implies(ProtectionDomain domain, Permission permission) {
 
 		Map<ProtectionDomain, PermissionCollection> pdMap = this.currentPolicy.get().getProtectionDomain2PermissionCollection();
 		;
 
 		PermissionCollection pc = (PermissionCollection) pdMap.get(domain);
 
 		if (pc != null) {
 			return pc.implies(permission);
 		}
 
 		pc = getPermissions(domain);
 		if (pc == null) {
 			return false;
 		}
 
 		// cache mapping of protection domain to its PermissionCollection
 		pdMap.put(domain, pc);
 		return pc.implies(permission);
 
 	}
 
 	@Override
 	public void refresh() {
 
 		final GlobalPolicyHolder newGlobalPolicyHolder = new GlobalPolicyHolder();
 		final Properties p = System.getProperties();
 		// First we load all default policies.
 		try {
 			AccessController.doPrivileged(new PrivilegedExceptionAction() {
 
 				public Object run() throws Exception {
 
 					String extraURL = p.getProperty(_POLICY_PROPERTY);
 					if (extraURL != null) {
 						// if we have this we dont load default policies.
 						loadPolicy(extraURL, newGlobalPolicyHolder);
 						newGlobalPolicyHolder.loadedFiles.add(extraURL);
 
 					} else {
 						// URL policyURL = new URL(_DEFAULT_POLICY);
 						String policyURL = _DEFAULT_POLICY;
 						if (!loadPolicy(policyURL, newGlobalPolicyHolder)) {
 
 							if (logger.isDebugEnabled()) {
 								logger.debug("Failed to load policy file: " + policyURL);
 							}
 						} else {
 							newGlobalPolicyHolder.loadedFiles.add(policyURL);
 						}
 
 						// policyURL = new URL(_DEFAULT_USER_POLICY);
 						policyURL = _DEFAULT_USER_POLICY;
 						if (!loadPolicy(policyURL, newGlobalPolicyHolder)) {
 							if (logger.isDebugEnabled()) {
 								logger.debug("Failed to load policy file: " + policyURL);
 							}
 						} else {
 							newGlobalPolicyHolder.loadedFiles.add(policyURL);
 						}
 
 					}
 
 					return null;
 				}
 
 			});
 			AccessController.doPrivileged(new PrivilegedExceptionAction() {
 
 				public Object run() throws Exception {
 					// load some other parts :)
 					for (int counter = 1;; counter++) {
 						String urlValue = p.getProperty(_POLICY_URL_PART + counter);
 						if (urlValue == null)
 							break;
 						// policyURL = new URL(urlValue);
 						String policyURL = urlValue;
 						if (!loadPolicy(policyURL, newGlobalPolicyHolder)) {
 							if (logger.isDebugEnabled()) {
 								logger.debug("Failed to load policy file: " + policyURL);
 							}
 						}
 
 					}
 					return null;
 				}
 
 			});
 			// System.err.println("SLEE POLICIES: "+sleeComponentsPermissions.size());
 
 			// now lets push slee policies :)
 			for (PermissionHolder ph : sleeComponentsPermissions) {
 
 				for (PolicyHolderEntry phe : ph.getPolicyHolderEntry()) {
 					// System.err.println("ADDING PHE: "+phe);
 					newGlobalPolicyHolder.policyHolderEntries.add(phe);
 				}
 			}
 
 			// for(Object o: sleeComponentsPermissions)
 			// {
 			// System.err.println("SLEE POLICIES: "+o);
 			// }
 		} catch (Exception e) {
 			// if we fail, lets dump info, and load defualt policy
 
 			logger.error("Failed to load policies due to some reason. See error message. Loadign default policy.");
 			e.printStackTrace();
 			loadDefaultPolicy(newGlobalPolicyHolder);
 		}
 		if (newGlobalPolicyHolder.isAnyPolicyLoaded())
 			loadDefaultPolicy(newGlobalPolicyHolder);
 		this.currentPolicy.set(newGlobalPolicyHolder);
 	}
 
 	private void loadDefaultPolicy(final GlobalPolicyHolder newGlobalPolicyHolder) {
 		AccessController.doPrivileged(new PrivilegedAction<Void>() {
 			public Void run() {
 				URL u = null;
 				Certificate[] c = null;
 				PolicyHolderEntry pe = new PolicyHolderEntry(new CodeSource(u, c));
 				pe.add(SecurityConstants.LOCAL_LISTEN_PERMISSION);
 				pe.add(new PropertyPermission("java.version", SecurityConstants.PROPERTY_READ_ACTION));
 				pe.add(new PropertyPermission("java.vendor", SecurityConstants.PROPERTY_READ_ACTION));
 				pe.add(new PropertyPermission("java.vendor.url", SecurityConstants.PROPERTY_READ_ACTION));
 				pe.add(new PropertyPermission("java.class.version", SecurityConstants.PROPERTY_READ_ACTION));
 				pe.add(new PropertyPermission("os.name", SecurityConstants.PROPERTY_READ_ACTION));
 				pe.add(new PropertyPermission("os.version", SecurityConstants.PROPERTY_READ_ACTION));
 				pe.add(new PropertyPermission("os.arch", SecurityConstants.PROPERTY_READ_ACTION));
 				pe.add(new PropertyPermission("file.separator", SecurityConstants.PROPERTY_READ_ACTION));
 				pe.add(new PropertyPermission("path.separator", SecurityConstants.PROPERTY_READ_ACTION));
 				pe.add(new PropertyPermission("line.separator", SecurityConstants.PROPERTY_READ_ACTION));
 				pe.add(new PropertyPermission("java.specification.version", SecurityConstants.PROPERTY_READ_ACTION));
 				pe.add(new PropertyPermission("java.specification.vendor", SecurityConstants.PROPERTY_READ_ACTION));
 				pe.add(new PropertyPermission("java.specification.name", SecurityConstants.PROPERTY_READ_ACTION));
 				pe.add(new PropertyPermission("java.vm.specification.version", SecurityConstants.PROPERTY_READ_ACTION));
 				pe.add(new PropertyPermission("java.vm.specification.vendor", SecurityConstants.PROPERTY_READ_ACTION));
 				pe.add(new PropertyPermission("java.vm.specification.name", SecurityConstants.PROPERTY_READ_ACTION));
 				pe.add(new PropertyPermission("java.vm.version", SecurityConstants.PROPERTY_READ_ACTION));
 				pe.add(new PropertyPermission("java.vm.vendor", SecurityConstants.PROPERTY_READ_ACTION));
 				pe.add(new PropertyPermission("java.vm.name", SecurityConstants.PROPERTY_READ_ACTION));
 
 				// No need to sync because noone has access to newInfo yet
 				newGlobalPolicyHolder.policyHolderEntries.add(pe);
 
 				return null;
 			}
 		});
 
 	}
 
 	// private boolean loadPolicy(URL policyURL, GlobalPolicyHolder
 	// newGlobalPolicyHolder) {
 	private boolean loadPolicy(String policyURLString, GlobalPolicyHolder newGlobalPolicyHolder) {
 
 		// System.err.println("Load policy: " + policyURLString);
 		try {
 			URI policyURI = null;
 			if (policyURLString.startsWith(_PROTOCOL_FILE)) {
 				File policyFile = new File(policyURLString.replaceFirst(_PROTOCOL_FILE_PREFIX, ""));
 				if (!policyFile.exists() || !policyFile.isFile() || !policyFile.canRead()) {
 					if (logger.isDebugEnabled()) {
 						logger.debug("Could not load file: " + policyURLString + ", exists[" + policyFile.exists() + "] isFile[" + policyFile.isFile() + "] canRead[" + policyFile.canRead() + "] ");
 
 					}
 					// Hmm...
 					return false;
 
 				} else {
 					policyURI = policyFile.toURI().normalize();
 				}
 			} else {
 				policyURI = new URI(policyURLString);
 			}
 
 			PolicyParser pp = new PolicyParser(true);
 			InputStream is = getStream(policyURI);
 			InputStreamReader reader = new InputStreamReader(is);
 			pp.read(reader);
 			reader.close();
 
 			// KeyStore ks = ..... FIXME:
 			KeyStore ks = null;
 
 			Enumeration<PolicyParser.GrantEntry> grantEntries = pp.grantElements();
 			while (grantEntries.hasMoreElements()) {
 
 				parseGrantEntry(grantEntries.nextElement(), ks, newGlobalPolicyHolder);
 			}
 
 		} catch (Exception e) {
 
 			if (logger.isDebugEnabled()) {
 				logger.debug("Failed to read policy file due to some error.");
 				e.printStackTrace();
 			}
 			return false;
 		}
 
 		return true;
 	}
 
 	private void parseGrantEntry(GrantEntry nextElement, KeyStore ks, GlobalPolicyHolder newGlobalPolicyHolder) {
 
 		try {
 			CodeSource cs = getCodeSource(nextElement, ks);
 			// should not be null but:
 			if (cs == null)
 				return;
 			PolicyHolderEntry phe = new PolicyHolderEntry(cs, nextElement.principals);
 
 			Enumeration<PolicyParser.PermissionEntry> permissionEntries = nextElement.permissionElements();
 			while (permissionEntries.hasMoreElements()) {
 
 				// FIXME: sun has something like - exapnd permission???
 				// FIXME: sun has permission self ?
 				PolicyParser.PermissionEntry pEntry = permissionEntries.nextElement();
 
 				try {
 					Permission permission = getPermission(pEntry);
 
 					phe.add(permission);
 				} catch (ClassNotFoundException cnfe) {
 					// FIXME:
 					Certificate certs[] = null;
 
 					// only add if we had no signer or we had a
 					// a signer and found the keys for it.
 					if (certs != null || pEntry.signedBy == null) {
 						Permission permission = new UnresolvedPermission(pEntry.permission, pEntry.name, pEntry.action, certs);
 						phe.add(permission);
 
 					}
 				} catch (Exception e) {
 
 					// if (logger.isDebugEnabled()) {
 					logger.error("Failed to add permission to entry.");
 					e.printStackTrace();
 					// }
 
 				}
 			}
 	
 			newGlobalPolicyHolder.policyHolderEntries.add(phe);
 		} catch (Exception e) {
 
 			// if (logger.isDebugEnabled()) {
 			logger.error("Failed to parse grant entry.");
 			e.printStackTrace();
 			// }
 
 		}
 
 	}
 
 	private void parseGrantEntry(GrantEntry nextElement, KeyStore ks, PermissionHolder permissionHolder) {
 
 		try {
 			// System.err.println("PARSING GRANT ENTRY: "+permissionHolder);
 			CodeSource cs = getCodeSource(nextElement, ks);
 
 			// should not be null but:
 			if (cs == null)
 				return;
 			PolicyHolderEntry phe = new PolicyHolderEntry(cs, nextElement.principals);
 
 			Enumeration<PolicyParser.PermissionEntry> permissionEntries = nextElement.permissionElements();
 			permissionHolder.addPolicyHolderEntry(phe);
 			while (permissionEntries.hasMoreElements()) {
 
 				// FIXME: sun has something like - exapnd permission???
 				// FIXME: sun has permission self ?
 				PolicyParser.PermissionEntry pEntry = permissionEntries.nextElement();
 
 				try {
 					Permission permission = getPermission(pEntry);
 					phe.add(permission);
 				} catch (ClassNotFoundException cnfe) {
 					cnfe.printStackTrace();
 					// FIXME:
 					Certificate certs[] = null;
 
 					// only add if we had no signer or we had a
 					// a signer and found the keys for it.
 					if (certs != null || pEntry.signedBy == null) {
 						Permission permission = new UnresolvedPermission(pEntry.permission, pEntry.name, pEntry.action, certs);
 						phe.add(permission);
 
 					}
 
 				} catch (Exception e) {
 
 					if (logger.isDebugEnabled()) {
 						logger.error("Failed to add permission to entry.");
 						e.printStackTrace();
 					}
 
 				}
 			}
 
 		} catch (Exception e) {
 
 			if (logger.isDebugEnabled()) {
 				logger.error("Failed to parse grant entry.");
 				e.printStackTrace();
 			}
 
 		}
 	}
 
 	private Permission getPermission(PermissionEntry entry) throws ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException, InstantiationException,
 			IllegalAccessException, InvocationTargetException {
 
 		Class permissionClass = Class.forName(entry.permission);
 		// FIXME: should we look for other permissions
 		Permission p = null;
 		if (entry.name != null && entry.action != null) {
 			Constructor c = permissionClass.getConstructor(_PARAMS_2);
 			p = (Permission) c.newInstance(new Object[] { entry.name, entry.action });
 			// System.err.println("PC: "+permissionClass.getCanonicalName()+" - "+entry.name+" -- "+entry.action);
 
 		} else if (entry.name != null && entry.action == null) {
 			Constructor c = permissionClass.getConstructor(_PARAMS_1);
 			p = (Permission) c.newInstance(new Object[] { entry.name });
 			// System.err.println("PC: "+permissionClass.getCanonicalName()+" - "+entry.name);
 		} else {
 			Constructor c = permissionClass.getConstructor(_PARAMS_0);
 			p = (Permission) c.newInstance(new Object[] {});
 			// System.err.println("PC: "+permissionClass.getCanonicalName());
 		}
 
 		return p;
 	}
 
 	// private InputStream getStream(URL url) throws IOException {
 	private InputStream getStream(URI uri) throws IOException {
 
 		if (uri.toURL().getProtocol().equals(_PROTOCOL_FILE)) {
 			String path = uri.toURL().getFile().replace('\\', File.separatorChar);
 
 			return new FileInputStream(path);
 		} else {
 			return uri.toURL().openStream();
 		}
 
 	}
 
 	private CodeSource getCodeSource(GrantEntry nextElement, KeyStore ks) throws MalformedURLException {
 
 		Certificate[] certs = null;
 		if (nextElement.signedBy != null) {
 			// FIXME:
 			// certs = ...;
 			if (certs == null) {
 				// we don't have a key for this alias,
 				// just return
 
 				// }
 				// return null;
 			}
 		}
 
 		URL location;
 
 		if (nextElement.codeBase != null)
 			location = new URL(nextElement.codeBase);
 		else
 			location = null;
 
 		// FIXME: sun has something cvalled : canonical ....
 		return new CodeSource(location, certs);
 	}
 
 	// Some add/remove methods
 	public boolean addPermissionHolder(PermissionHolder holder) {
 
 		return this.addPermissionHolder(holder, true);
 	}
 
 	public boolean addPermissionHolder(PermissionHolder holder, boolean refresh) {
 
 		
 		if (this.sleeComponentsPermissions.contains(holder)) {
 			// System.err.println("ALREADY PRESENT.");
 			return false;
 		} else {
 			this.sleeComponentsPermissions.add(holder);
 			// Parse
 
 			// KeyStore ks = ..... FIXME:
 			KeyStore ks = null;
 
 			Enumeration<PolicyParser.GrantEntry> grantEntries = holder.getPolicyParser().grantElements();
 			while (grantEntries.hasMoreElements()) {
 
 				parseGrantEntry(grantEntries.nextElement(), ks, holder);
 			}
 			// System.err.println("ADD: "+holder.getPermissionCodeBaseURI()+"\n"+holder.getPolicy()+"\n"+holder.getPolicyHolderEntry().size());
 			if (refresh)
 				this.refresh();
 			return true;
 		}
 	}
 
 	public boolean removePermissionHolder(PermissionHolder holder) {
 
 		return this.removePermissionHolder(holder, true);
 	}
 
 	public boolean removePermissionHolder(PermissionHolder holder, boolean refresh) {
 
 		if (this.sleeComponentsPermissions.contains(holder)) {
 			return false;
 		} else {
 			this.sleeComponentsPermissions.remove(holder);
 			if (refresh)
 				this.refresh();
 			return true;
 		}
 	}
 
 	/**
 	 * This class holds globaly active policies. Start time:13:42:44 2009-04-13<br>
 	 * Project: mobicents-jainslee-server-core<br>
 	 * 
 	 * @author <a href="mailto:baranowb@gmail.com">baranowb - Bartosz Baranowski
 	 *         </a>
 	 * @author <a href="mailto:brainslog@gmail.com"> Alexandre Mendonca </a>
 	 */
 	private class GlobalPolicyHolder {
 		private final List<PolicyHolderEntry> policyHolderEntries = new ArrayList<PolicyHolderEntry>();
 		private final Map<ProtectionDomain, PermissionCollection> protectionDomain2PermissionCollection = new HashMap<ProtectionDomain, PermissionCollection>();
 		private final Set<String> loadedFiles = new HashSet<String>();
 
 		public boolean isAnyPolicyLoaded() {
 			return policyHolderEntries.size() > 0;
 		}
 
 		public Map<ProtectionDomain, PermissionCollection> getProtectionDomain2PermissionCollection() {
 			return protectionDomain2PermissionCollection;
 		}
 
 	}
 
 	private class ImpliesAction implements PrivilegedExceptionAction {
 		private CodeSource cs;
 		private PolicyHolderEntry phe;
 
 		public ImpliesAction(CodeSource cs, PolicyHolderEntry phe) {
 			super();
 			this.cs = cs;
 			this.phe = phe;
 		}
 
 		public Object run() throws Exception {
 			// TODO Auto-generated method stuba
 			return new Boolean(phe.getCodeSource().implies(cs));
 		}
 
 	}
 
 	/**
 	 * This function MUST convert code source passed. Meaning it must normalize
 	 * URL. For instance security framework may pass url that conceptualy
 	 * matches pat specified policy files but comparison wont match. See url
 	 * belowe.
 	 * <ul>
 	 * <li><b>1 -
 	 * </b>jar:file:/D:/java/servers/jboss-5.0.0.GA/server/default/deploy
 	 * /mobicents.sar/lib/jar.jar!/</li>
 	 * <li><b>2 -
 	 * </b>file:/D:/java/servers/jboss-5.0.0.GA/server/default/deploy/-</li>
 	 * </ul>
 	 * URL 1 is passed from security framework as source of loaded classes.
 	 * However it is not matched by second one (and is should)
 	 * 
 	 * @param cs
 	 * @return
 	 */
 	private CodeSource performUrlConversion(CodeSource cs, boolean extractSignerCerts) {
 
 		String path = null;
 		CodeSource parsedCodeSource = null;
 
 		URL locationURL = cs.getLocation();
 		if (locationURL != null) {
 			// can happen for default.
 			Permission urlAccessPermission = null;
 			try {
 				urlAccessPermission = locationURL.openConnection().getPermission();
 			} catch (IOException e) {
 
 			}
 
 			if (urlAccessPermission != null && urlAccessPermission instanceof FilePermission) {
 				path = ((FilePermission) urlAccessPermission).getName();
 			} else if ((urlAccessPermission == null) && (locationURL.getProtocol().equals("file"))) {
 				path = locationURL.getFile().replace('/', File.separatorChar);
 				// FIXME: do more?
 			} else {
 				// FIXME: ??
 			}
 		}
 
 		if (path == null) {
 			if (extractSignerCerts) {
 				parsedCodeSource = new CodeSource(cs.getLocation(), getSignerCertificates(cs));
 			}
 		} else {
 
 			try {
 				// Sun says it fails
 				if (path.endsWith("*")) {
 					// remove trailing '*' because it causes canonicaization
 					// to fail on win32
 					path = path.substring(0, path.length() - 1);
 					boolean removeTrailingFileSep = false;
 					if (path.endsWith(File.separator))
 						removeTrailingFileSep = true;
 					if (path.equals("")) {
 						path = System.getProperty("user.dir");
 					}
 					File f = new File(path);
 					path = f.getCanonicalPath();
 					StringBuffer sb = new StringBuffer(path);
 					// reappend '*' to canonicalized filename (note that
 					// canonicalization may have removed trailing file
 					// separator, so we have to check for that, too)
 					if (!path.endsWith(File.separator) && (removeTrailingFileSep || f.isDirectory()))
 						sb.append(File.separatorChar);
 					sb.append('*');
 					path = sb.toString();
 				} else if(path.endsWith(File.separator))
 				{
 					//this is xxxx/x.jar!/ case.....
 					path = path.substring(0, path.length() - 1);
 					if(path.endsWith("!"))
 					{
 						path = path.substring(0, path.length() - 1);
 					}
 				}else {
 					path = new File(path).getCanonicalPath();
 				}
 				// FIXME: possible convert URL? what is check this in rfc
 				locationURL = new File(path).toURI().toURL();
 
 				if (extractSignerCerts) {
 					parsedCodeSource = new CodeSource(locationURL, getSignerCertificates(cs));
 				} else {
 					parsedCodeSource = new CodeSource(locationURL, cs.getCertificates());
 				}
 			} catch (IOException ioe) {
 				// leave codesource as it is, unless we have to extract its
 				// signer certificates
 				if (extractSignerCerts) {
 					parsedCodeSource = new CodeSource(cs.getLocation(), getSignerCertificates(cs));
 				}
 			}
 
 		}
 
 		return parsedCodeSource;
 	}
 
 	private Certificate[] getSignerCertificates(CodeSource cs) {
 		// FIXME: fill this once we have certs
 		return null;
 	}
 
 	// Some methods to expose info about what is goign on.
 
 	public String getCodeSources() {
 
 
 		List<String> css = new ArrayList<String>();
 		for (PolicyHolderEntry phe : this.currentPolicy.get().policyHolderEntries) {
			css.add(phe.getCodeSource().getLocation().toString());
 		}
 
 		return Arrays.toString(css.toArray());
 	}
 
 	public String getPolicyFilesURL() {
 		return Arrays.toString(this.currentPolicy.get().loadedFiles.toArray());
 	}
 
 	
 
 }
