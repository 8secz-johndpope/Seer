 /*******************************************************************************
  * Copyright (c) 2000, 2007 IBM Corporation and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *     Atsuhiko Yamanaka, JCraft,Inc. - initial API and implementation.
  *     IBM Corporation - ongoing maintenance
  *******************************************************************************/
 package org.eclipse.jsch.internal.core;
 
 // import org.eclipse.core.runtime.Plugin;
 import java.util.Hashtable;
 import java.util.Map;
 
 import org.eclipse.core.net.proxy.IProxyService;
 import org.eclipse.core.runtime.*;
 import org.eclipse.core.runtime.preferences.*;
 import org.eclipse.jsch.core.IJSchService;
 import org.eclipse.osgi.util.NLS;
 import org.osgi.framework.BundleContext;
 import org.osgi.framework.ServiceRegistration;
 import org.osgi.util.tracker.ServiceTracker;
 
 import com.jcraft.jsch.JSch;
 import com.jcraft.jsch.JSchException;
 
 public class JSchCorePlugin extends Plugin{
 
   public static String ID="org.eclipse.jsch.core"; //$NON-NLS-1$
 
   private static final String PROP_REGISTER_SERVICE="org.eclipse.jsch.core.enableService"; //$NON-NLS-1$
 
   // communication timeout with the server
   public static final int DEFAULT_TIMEOUT=60;
   private int communicationsTimeout=DEFAULT_TIMEOUT;
   private boolean needToLoadKnownHosts=true;
   private boolean needToLoadKeys=true;
 
   private JSch jsch;
 
  private String current_pkeys;
 
   private static final String INFO_PROXY_USER="org.eclipse.team.cvs.core.proxy.user"; //$NON-NLS-1$ 
   private static final String INFO_PROXY_PASS="org.eclipse.team.cvs.core.proxy.pass"; //$NON-NLS-1$ 
 
   public static final String PT_AUTHENTICATOR="authenticator"; //$NON-NLS-1$
 
   private static JSchCorePlugin plugin;
   private ServiceTracker tracker;
 
   private ServiceRegistration proxyService;
 
   public JSchCorePlugin(){
     plugin=this;
   }
 
   public static JSchCorePlugin getPlugin(){
     return plugin;
   }
 
   /**
    * Convenience method for logging CoreExceptions to the plugin log
    * 
    * @param e
    *          the exception
    */
   public static void log(CoreException e){
     log(e.getStatus().getSeverity(), e.getMessage(), e);
   }
 
   /**
    * Log the given status. Do not use this method for the IStatus from a
    * CoreException. Use<code>log(CoreException)</code> instead so the stack
    * trace is not lost.
    * 
    * @param status
    *          the status
    */
   public static void log(IStatus status){
     getPlugin().getLog().log(status);
   }
 
   public static void log(int severity, String message, Throwable e){
     log(new Status(severity, ID, 0, message, e));
   }
 
   // proxy configuration
   boolean isUseProxy(){
     IPreferencesService ps=Platform.getPreferencesService();
     String value=ps.get(IConstants.PREF_USE_PROXY,
         "false", getProxyPreferenceStore()); //$NON-NLS-1$
     return value==null ? false : !value.equals("false"); //$NON-NLS-1$
   }
 
   String getProxyType(){
     IPreferencesService ps=Platform.getPreferencesService();
     return ps.get(IConstants.PREF_PROXY_TYPE, "", getProxyPreferenceStore()); //$NON-NLS-1$
   }
 
   String getProxyHost(){
     IPreferencesService ps=Platform.getPreferencesService();
     return ps.get(IConstants.PREF_PROXY_HOST, "", getProxyPreferenceStore()); //$NON-NLS-1$
   }
 
   String getProxyPort(){
     IPreferencesService ps=Platform.getPreferencesService();
     return ps.get(IConstants.PREF_PROXY_PORT, "", getProxyPreferenceStore()); //$NON-NLS-1$
   }
 
   boolean isUseProxyAuth(){
     IPreferencesService ps=Platform.getPreferencesService();
     String value=ps.get(IConstants.PREF_PROXY_AUTH,
         "false", getProxyPreferenceStore()); //$NON-NLS-1$
     return value==null ? false : !value.equals("false"); //$NON-NLS-1$
   }
 
   String getProxyUser(){
     Object user=getAuthInfo().get(INFO_PROXY_USER);
     return user==null ? "" : (String)user; //$NON-NLS-1$
   }
 
   private Map getAuthInfo(){
     // XXX Auto-generated method stub
     return null;
   }
 
   String getProxyPassword(){
     Object pass=getAuthInfo().get(INFO_PROXY_PASS);
     return pass==null ? "" : (String)pass; //$NON-NLS-1$
   }
 
   /**
    * Get the communications timeout value in seconds
    * 
    * @return the timeout value in seconds
    */
   public int getTimeout(){
     return communicationsTimeout;
   }
 
   /**
    * Set the timeout value for communications to a value in seconds. The value
    * must be greater than or equal 0. If is it 0, there is no timeout.
    * 
    * @param timeout
    *          the timeout value in seconds
    */
   public void setTimeout(int timeout){
     this.communicationsTimeout=Math.max(0, timeout);
   }
 
   /**
    * Return the preferences node in the instance scope
    */
   IEclipsePreferences getInstancePreferences(){
     return new InstanceScope().getNode(ID);
   }
 
   private static IEclipsePreferences[] proxyPreferences;
 
   static IEclipsePreferences[] getProxyPreferenceStore(){
     if(proxyPreferences==null){
       proxyPreferences=new IEclipsePreferences[] {
           new InstanceScope().getNode("org.eclipse.team.cvs.ui"), //$NON-NLS-1$
           new DefaultScope().getNode("org.eclipse.team.cvs.ui") //$NON-NLS-1$
       };
     }
     return proxyPreferences;
   }
 
   public synchronized JSch getJSch(){
     if(jsch==null)
       jsch=new JSch();
     return jsch;
   }
 
   public void loadKnownHosts(){
     Preferences preferences=JSchCorePlugin.getPlugin().getPluginPreferences();
     String ssh_home=preferences.getString(IConstants.KEY_SSH2HOME);
 
     if(ssh_home.length()==0)
       ssh_home=PreferenceInitializer.SSH_HOME_DEFAULT;
 
     java.io.File file=new java.io.File(ssh_home, "known_hosts"); //$NON-NLS-1$
     try{
       getJSch().setKnownHosts(file.getPath());
     }
     catch(JSchException e){
       JSchCorePlugin.log(IStatus.ERROR, NLS.bind(
           "An error occurred while loading the know hosts file {0}", file //$NON-NLS-1$
               .getAbsolutePath()), e);
     }
     needToLoadKnownHosts=false;
   }
 
   public boolean isNeedToLoadKnownHosts(){
     return needToLoadKnownHosts;
   }
 
   public void setNeedToLoadKnownHosts(boolean needToLoadKnownHosts){
     this.needToLoadKnownHosts=needToLoadKnownHosts;
   }
 
   public boolean isNeedToLoadKeys(){
     return needToLoadKeys;
   }
 
   public void setNeedToLoadKeys(boolean needToLoadKeys){
     this.needToLoadKeys=needToLoadKeys;
   }
 
   public void loadPrivateKeys(){
     current_pkeys=Utils.loadPrivateKeys(getJSch(), current_pkeys);
     setNeedToLoadKeys(false);
   }
 
   /**
    * Return the {@link IProxyService} or <code>null</code> if the service is
    * not available.
    * 
    * @return the {@link IProxyService} or <code>null</code>
    */
   public IProxyService getProxyService(){
     return (IProxyService)tracker.getService();
   }
 
   public void start(BundleContext context) throws Exception{
     super.start(context);
     tracker=new ServiceTracker(getBundle().getBundleContext(),
         IProxyService.class.getName(), null);
     tracker.open();
     if(Boolean
         .valueOf(System.getProperty(PROP_REGISTER_SERVICE, "true")).booleanValue()){ //$NON-NLS-1$
       proxyService=getBundle().getBundleContext().registerService(
           IJSchService.class.getName(), JSchProvider.getInstance(), new Hashtable());
     }
   }
 
   public void stop(BundleContext context) throws Exception{
     super.stop(context);
     tracker.close();
     if(proxyService!=null){
       proxyService.unregister();
       proxyService=null;
     }
   }
 }
