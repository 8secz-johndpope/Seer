 package org.tmatesoft.svn.core.wc2;
 
 import java.io.File;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Collection;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.LinkedList;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
import java.util.logging.Level;
 
 import org.tmatesoft.svn.core.ISVNCanceller;
 import org.tmatesoft.svn.core.SVNErrorCode;
 import org.tmatesoft.svn.core.SVNException;
 import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
 import org.tmatesoft.svn.core.internal.db.SVNSqlJetDb.Mode;
import org.tmatesoft.svn.core.internal.wc.admin.SVNAdminArea;
import org.tmatesoft.svn.core.internal.wc.admin.SVNWCAccess;
 import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
 import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb.SVNWCDbOpenMode;
 import org.tmatesoft.svn.core.internal.wc17.db.SVNWCDb;
 import org.tmatesoft.svn.core.internal.wc2.SvnWcGeneration;
 import org.tmatesoft.svn.core.internal.wc2.ng.SvnNgCheckout;
 import org.tmatesoft.svn.core.internal.wc2.ng.SvnNgGetInfo;
 import org.tmatesoft.svn.core.internal.wc2.ng.SvnNgGetProperties;
 import org.tmatesoft.svn.core.internal.wc2.ng.SvnNgGetStatus;
 import org.tmatesoft.svn.core.internal.wc2.ng.SvnNgSwitch;
 import org.tmatesoft.svn.core.internal.wc2.ng.SvnNgUpdate;
 import org.tmatesoft.svn.core.internal.wc2.old.SvnOldCheckout;
 import org.tmatesoft.svn.core.internal.wc2.old.SvnOldGetInfo;
 import org.tmatesoft.svn.core.internal.wc2.old.SvnOldGetProperties;
 import org.tmatesoft.svn.core.internal.wc2.old.SvnOldGetStatus;
 import org.tmatesoft.svn.core.internal.wc2.old.SvnOldSwitch;
 import org.tmatesoft.svn.core.internal.wc2.old.SvnOldUpdate;
 import org.tmatesoft.svn.core.internal.wc2.remote.SvnRemoteGetInfo;
 import org.tmatesoft.svn.core.internal.wc2.remote.SvnRemoteGetProperties;
 import org.tmatesoft.svn.core.wc.DefaultSVNRepositoryPool;
 import org.tmatesoft.svn.core.wc.ISVNEventHandler;
 import org.tmatesoft.svn.core.wc.ISVNOptions;
 import org.tmatesoft.svn.core.wc.ISVNRepositoryPool;
 import org.tmatesoft.svn.core.wc.SVNWCUtil;
 
 public class SvnOperationFactory {
     
     private Map<Class<?>, List<ISvnOperationRunner<?, SvnOperation<?>>>> anyFormatOperationRunners;
     private Map<Class<?>, List<ISvnOperationRunner<?, SvnOperation<?>>>> noneOperationRunners;
     private Map<Class<?>, List<ISvnOperationRunner<?, SvnOperation<?>>>> v17OperationRunners;
     private Map<Class<?>, List<ISvnOperationRunner<?, SvnOperation<?>>>> v16OperationRunners;
     
     private ISVNAuthenticationManager authenticationManager;
     private ISVNCanceller canceller;
     private ISVNEventHandler eventHandler;
     private ISVNOptions options;
     private ISVNRepositoryPool repositoryPool;
     
     private boolean autoCloseContext;
     private boolean autoDisposeRepositoryPool;
     private SVNWCContext wcContext;
     
     private SvnWcGeneration primaryWcGeneration;
 
     public SvnOperationFactory() {
         this(null);
     }
     
     public SvnOperationFactory(SVNWCContext context) {
         wcContext = context;
         
         setAutoCloseContext(wcContext == null);
         registerRunners();
     }
     
     private void registerRunners() {
         v17OperationRunners = new HashMap<Class<?>, List<ISvnOperationRunner<?, SvnOperation<?>>>>();
         v16OperationRunners = new HashMap<Class<?>, List<ISvnOperationRunner<?, SvnOperation<?>>>>();
         anyFormatOperationRunners = new HashMap<Class<?>, List<ISvnOperationRunner<?, SvnOperation<?>>>>();
         noneOperationRunners = new HashMap<Class<?>, List<ISvnOperationRunner<?, SvnOperation<?>>>>();
 
         registerOperationRunner(SvnGetInfo.class, new SvnRemoteGetInfo());
         registerOperationRunner(SvnGetInfo.class, new SvnNgGetInfo());
         registerOperationRunner(SvnGetInfo.class, new SvnOldGetInfo());
 
         registerOperationRunner(SvnGetProperties.class, new SvnRemoteGetProperties());
         registerOperationRunner(SvnGetProperties.class, new SvnNgGetProperties());
         registerOperationRunner(SvnGetProperties.class, new SvnOldGetProperties());
 
         registerOperationRunner(SvnGetStatus.class, new SvnNgGetStatus());
         registerOperationRunner(SvnGetStatus.class, new SvnOldGetStatus());
 
         registerOperationRunner(SvnCheckout.class, new SvnNgCheckout());
         registerOperationRunner(SvnCheckout.class, new SvnOldCheckout());
         
         registerOperationRunner(SvnSwitch.class, new SvnNgSwitch());
         registerOperationRunner(SvnSwitch.class, new SvnOldSwitch());
 
         registerOperationRunner(SvnUpdate.class, new SvnNgUpdate());
         registerOperationRunner(SvnUpdate.class, new SvnOldUpdate());
     }
     
     public boolean isAutoCloseContext() {
         return autoCloseContext;
     }
 
     public void setAutoCloseContext(boolean autoCloseContext) {
         this.autoCloseContext = autoCloseContext;
     }
 
     public ISVNAuthenticationManager getAuthenticationManager() {
         if (authenticationManager == null) {
             authenticationManager = SVNWCUtil.createDefaultAuthenticationManager();
         }
         return authenticationManager;
     }
 
     public ISVNCanceller getCanceller() {
         if (canceller == null && getEventHandler() != null) {
             return getEventHandler();
         }
         return canceller;
     }
 
     public ISVNEventHandler getEventHandler() {
         return eventHandler;
     }
 
     public ISVNRepositoryPool getRepositoryPool() {
         if (repositoryPool == null) {
             repositoryPool = new DefaultSVNRepositoryPool(getAuthenticationManager(), getOptions());
             setAutoDisposeRepositoryPool(true);
         }
         return repositoryPool;
     }
 
     public ISVNOptions getOptions() {
         return options;
     }
 
     public void setAuthenticationManager(ISVNAuthenticationManager authenticationManager) {
         this.authenticationManager = authenticationManager;
         if (repositoryPool != null) {
             repositoryPool.setAuthenticationManager(authenticationManager);
         }
     }
 
     public void setCanceller(ISVNCanceller canceller) {
         this.canceller = canceller;
     }
 
     public void setEventHandler(ISVNEventHandler eventHandler) {
         this.eventHandler = eventHandler;
         disposeWcContext();
     }
 
     public void setOptions(ISVNOptions options) {
         this.options = options;
         disposeWcContext();
     }
 
     public void setRepositoryPool(ISVNRepositoryPool repositoryPool) {
         this.repositoryPool = repositoryPool;
         setAutoDisposeRepositoryPool(repositoryPool == null);
     }
     
     public void dispose() {
         disposeWcContext();
         if (isAutoDisposeRepositoryPool() && repositoryPool != null) {
             repositoryPool.dispose();
         }
     }
 
     public SvnGetInfo createGetInfo() {
         return new SvnGetInfo(this);
     }
     
     public SvnGetProperties createGetProperties() {
         return new SvnGetProperties(this);
     }
 
     public SvnGetStatus createGetStatus() {
         return new SvnGetStatus(this);
     }
     
     public SvnUpdate createUpdate() {
         return new SvnUpdate(this);
     }
     
     public SvnSwitch createSwitch() {
         return new SvnSwitch(this);
     }
 
     public SvnCheckout createCheckout() {
         return new SvnCheckout(this);
     }
 
     protected Object run(SvnOperation<?> operation) throws SVNException {
         ISvnOperationRunner<?, SvnOperation<?>> runner = getImplementation(operation);
         if (runner != null) {
             SVNWCContext wcContext = null;
             try {
                 wcContext = obtainWcContext();
                 runner.setWcContext(wcContext);
                 return runner.run(operation);
             } finally {
                 releaseWcContext(wcContext);
             }
         }
         return null;
     }
 
     private void releaseWcContext(SVNWCContext wcContext) {
         if (isAutoCloseContext() && wcContext != null) {
             if (this.wcContext == wcContext) {
                 disposeWcContext();
             } else {
                 wcContext.close();
             }
         }
     }
 
     private SVNWCContext obtainWcContext() {
         if (wcContext == null) {
             wcContext = new SVNWCContext(getOptions(), getEventHandler());
         }
         return wcContext;
     }
 
     private void disposeWcContext() {
         if (wcContext != null) {
             wcContext.close();
             wcContext = null;
         }
     }
     
     private boolean isAutoDisposeRepositoryPool() {
         return autoDisposeRepositoryPool;
     }
     
     private void setAutoDisposeRepositoryPool(boolean dispose) {
         autoDisposeRepositoryPool = dispose;
     }
 
     protected ISvnOperationRunner<?, SvnOperation<?>> getImplementation(SvnOperation<?> operation) throws SVNException {
         if (operation == null) {
             return null;
         }
         SvnWcGeneration wcGeneration = null;
         if (operation.hasLocalTargets()) {
             wcGeneration = detectWcGeneration(operation.getFirstTarget().getFile());
         }
         final List<ISvnOperationRunner<?, SvnOperation<?>>> candidateRunners = new LinkedList<ISvnOperationRunner<?, SvnOperation<?>>>();
         
         candidateRunners.addAll(getRunners(operation.getClass(), anyFormatOperationRunners));
         if (wcGeneration == SvnWcGeneration.NOT_DETECTED) {
             candidateRunners.addAll(getRunners(operation.getClass(), noneOperationRunners));
         } else if (wcGeneration == SvnWcGeneration.V16) {
             candidateRunners.addAll(getRunners(operation.getClass(), v16OperationRunners));
         } else if (wcGeneration == SvnWcGeneration.V17) {
             candidateRunners.addAll(getRunners(operation.getClass(), v17OperationRunners));
         }
         
         ISvnOperationRunner<?, SvnOperation<?>> runner = null;
         
         for (ISvnOperationRunner<?, SvnOperation<?>> candidateRunner : candidateRunners) {            
             boolean isApplicable = candidateRunner.isApplicable(operation, wcGeneration);
             if (!isApplicable) {
                 continue;
             }
             runner = candidateRunner;
             break;
         }
         if (runner != null) {
             runner.reset();
         }
         return runner;
     }
     
     @SuppressWarnings("unchecked")
     protected void registerOperationRunner(Class<?> operationClass, ISvnOperationRunner<?, ? extends SvnOperation<?>> runner) {
         if (operationClass == null || runner == null) {
             return;
         }
         Collection<Map<Class<?>, List<ISvnOperationRunner<?, SvnOperation<?>>>>> maps = new ArrayList<Map<Class<?>,List<ISvnOperationRunner<?, SvnOperation<?>>>>>();
         SvnWcGeneration[] scope = getRunnerScope(runner);
         if (scope == null || scope.length == 0) {
             maps.add(anyFormatOperationRunners);
         } else {
             Set<SvnWcGeneration> formatsSet = new HashSet<SvnWcGeneration>();
             formatsSet.addAll(Arrays.asList(scope));
             if (formatsSet.contains(SvnWcGeneration.NOT_DETECTED)) {
                 maps.add(noneOperationRunners);
             }
             if (formatsSet.contains(SvnWcGeneration.V17)) {
                 maps.add(v17OperationRunners);
             }
             if (formatsSet.contains(SvnWcGeneration.V16)) {
                 maps.add(v16OperationRunners);
             }
         }
         for (Map<Class<?>, List<ISvnOperationRunner<?, SvnOperation<?>>>> runnerMap : maps) {
             List<ISvnOperationRunner<?, SvnOperation<?>>> runners = runnerMap.get(operationClass);
             if (runners == null) {
                 runners = new LinkedList<ISvnOperationRunner<?, SvnOperation<?>>>();
                 runnerMap.put(operationClass, runners);
             }
             runners.add((ISvnOperationRunner<?, SvnOperation<?>>) runner);
         }
     }
     
     protected SvnWcGeneration detectWcGeneration(File path) throws SVNException {
         SVNWCDb db = new SVNWCDb();
         try {
             db.open(SVNWCDbOpenMode.ReadOnly, (ISVNOptions) null, false, false);
             db.parseDir(path, Mode.ReadOnly);
             return SvnWcGeneration.V17;
         } catch (SVNException e) {
             if (e.getErrorMessage().getErrorCode() == SVNErrorCode.WC_NOT_WORKING_COPY) {
                 return SvnWcGeneration.NOT_DETECTED;
            } else if (e.getErrorMessage().getErrorCode() == SVNErrorCode.WC_UNSUPPORTED_FORMAT) {                
                if (path.getParentFile() == null) {
                    return SvnWcGeneration.V16;
                }
                    
                // there should be an exception for an 'add' operation.
                SVNWCAccess wcAccess = SVNWCAccess.newInstance(null);
                try {
                    SVNAdminArea dir = wcAccess.open(path, false, false, false, 0, Level.OFF);
                    if (dir != null && dir.getEntry(path.getName(), false) != null) {
                        return SvnWcGeneration.V16;
                    }
                } catch (SVNException inner) {                         
                } finally {
                    wcAccess.close();
                }
                return SvnWcGeneration.NOT_DETECTED;
                
             } else {
                 throw e;
             }
         } finally {
             db.close();
         }
     }
     
     private static List<ISvnOperationRunner<?, SvnOperation<?>>> getRunners(Class<?> clazz, Map<Class<?>, List<ISvnOperationRunner<?,SvnOperation<?>>>> map) {
         List<ISvnOperationRunner<?, SvnOperation<?>>> list = map.get(clazz);
         if (list == null) {
             list = Collections.emptyList();
         }
         return list;
     }
 
     public SVNWCContext getWcContext() {
         return wcContext;
     }
 
     public SvnWcGeneration getPrimaryWcGeneration() {
         if (primaryWcGeneration == null) {
             String systemProperty = System.getProperty("svnkit.wc.17", "false");
             if (Boolean.toString(true).equalsIgnoreCase(systemProperty)) {
                 primaryWcGeneration = SvnWcGeneration.V17;
             } else {
                 primaryWcGeneration = SvnWcGeneration.V16;
             }
         }
         return primaryWcGeneration;
     }
     
     public SvnWcGeneration getSecondaryWcGeneration() {
         return getPrimaryWcGeneration() == SvnWcGeneration.V17 ? SvnWcGeneration.V16 : SvnWcGeneration.V17;
     }
 
     public void setPrimaryWcGeneration(SvnWcGeneration primaryWcGeneration) {
         this.primaryWcGeneration = primaryWcGeneration;
         registerRunners();
     }
     
     private SvnWcGeneration[] getRunnerScope(ISvnOperationRunner<?, ? extends SvnOperation<?>> runner) {
         if (runner.getWcGeneration() == getPrimaryWcGeneration()) {
             return new SvnWcGeneration[] { getPrimaryWcGeneration(), SvnWcGeneration.NOT_DETECTED};
         } else if (runner.getWcGeneration() == getSecondaryWcGeneration()) {
             return new SvnWcGeneration[] { getSecondaryWcGeneration() };
         } else {
             // any.
             return new SvnWcGeneration[] { };
         }
     }
     
 }
