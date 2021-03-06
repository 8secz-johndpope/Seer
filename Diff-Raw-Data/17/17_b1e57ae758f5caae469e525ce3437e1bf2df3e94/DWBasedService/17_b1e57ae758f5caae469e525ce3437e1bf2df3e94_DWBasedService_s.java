 package com.fasterxml.clustermate.dw;
 
 import java.io.ByteArrayOutputStream;
 import java.io.IOException;
 import java.io.InputStream;
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.List;
 
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import com.yammer.dropwizard.Service;
 import com.yammer.dropwizard.assets.AssetsBundle;
 import com.yammer.dropwizard.config.Bootstrap;
 import com.yammer.dropwizard.config.Environment;
 import com.yammer.dropwizard.lifecycle.Managed;
 
 import com.fasterxml.storemate.shared.*;
 import com.fasterxml.storemate.store.StorableStore;
 import com.fasterxml.storemate.store.backend.StoreBackend;
 import com.fasterxml.storemate.store.backend.StoreBackendBuilder;
 import com.fasterxml.storemate.store.backend.StoreBackendConfig;
 import com.fasterxml.storemate.store.file.FileManager;
 import com.fasterxml.storemate.store.impl.StorableStoreImpl;
 
 import com.fasterxml.clustermate.api.RequestPathBuilder;
 import com.fasterxml.clustermate.jaxrs.IndexResource;
 import com.fasterxml.clustermate.service.SharedServiceStuff;
 import com.fasterxml.clustermate.service.Stores;
 import com.fasterxml.clustermate.service.StartAndStoppable;
 import com.fasterxml.clustermate.service.cfg.ServiceConfig;
 import com.fasterxml.clustermate.service.cleanup.CleanerUpper;
 import com.fasterxml.clustermate.service.cluster.ClusterBootstrapper;
 import com.fasterxml.clustermate.service.cluster.ClusterInfoHandler;
 import com.fasterxml.clustermate.service.cluster.ClusterViewByServer;
 import com.fasterxml.clustermate.service.cluster.ClusterViewByServerImpl;
 import com.fasterxml.clustermate.service.cluster.ClusterViewByServerUpdatable;
 import com.fasterxml.clustermate.service.servlet.NodeStatusServlet;
 import com.fasterxml.clustermate.service.servlet.ServiceDispatchServlet;
 import com.fasterxml.clustermate.service.servlet.StoreEntryServlet;
 import com.fasterxml.clustermate.service.servlet.SyncListServlet;
 import com.fasterxml.clustermate.service.servlet.SyncPullServlet;
 import com.fasterxml.clustermate.service.store.StoreHandler;
 import com.fasterxml.clustermate.service.store.StoredEntry;
 import com.fasterxml.clustermate.service.store.StoredEntryConverter;
 import com.fasterxml.clustermate.service.store.StoresImpl;
 import com.fasterxml.clustermate.service.sync.SyncHandler;
 import com.fasterxml.clustermate.std.JdkHttpClientPathBuilder;
 
 public abstract class DWBasedService<
     K extends EntryKey,
     E extends StoredEntry<K>,
     SCONFIG extends ServiceConfig,
     CONF extends DWConfigBase<SCONFIG, CONF>
 >
     extends Service<CONF>
     implements Managed
 {
     private final Logger LOG = LoggerFactory.getLogger(getClass());
 
     /**
      * List of {@link StartAndStoppable} objects we will dispatch start/stop calls to.
      */
     protected List<StartAndStoppable> _managed = Collections.emptyList();
     
     /**
      * Marker flag used to indicate cases when service is run in test
      * mode; propagated through configuration.
      */
     protected final boolean _testMode;
 
     /**
      * We need to keep track of things created, to let tests access
      * the information
      */
     protected SharedServiceStuff _serviceStuff;
     
     /**
      * Container for various stores we use for data, metadata.
      */
     protected StoresImpl<K,E> _stores;
     
     /**
      * This object is needed to allow test code to work around usual
      * waiting time restrictions.
      */
     protected final TimeMaster _timeMaster;
 
     /**
      * And we better hang on to cluster view as well
      */
     protected ClusterViewByServerUpdatable _cluster;
     
     /**
      * Manager object that deals with data expiration and related
      * clean up tasks.
      */
     protected CleanerUpper<K,E> _cleanerUpper;
 
     /*
     /**********************************************************************
     /* Handlers
     /**********************************************************************
      */
     
     protected StoreHandler<K,E> _storeHandler;
     
     /*
     /**********************************************************************
     /* Construction
     /**********************************************************************
      */
 
     protected DWBasedService(TimeMaster timings)
     {
         this(timings, false);
     }
 
     protected DWBasedService(TimeMaster timings, boolean testMode)
     {
         super();
         _timeMaster = timings;
         _testMode = testMode;
     }
 
     /*
     /**********************************************************************
     /* Life-cycle
     /**********************************************************************
      */
 
     public void initialize(Bootstrap<CONF> bootstrap) {
         // Static stuff from under /html (except for root  level things
         // like /index.html that need special handling)
         bootstrap.addBundle(new AssetsBundle("/html"));
     }
     
     @Override
     public void run(CONF dwConfig, Environment environment) throws IOException
     {
         // first things first: we need to get start()/stop() calls, so:
         environment.manage(this);
 
         final SCONFIG config = dwConfig.getServiceConfig();
         
         _managed = new ArrayList<StartAndStoppable>();
 
         StoredEntryConverter<K,E> entryConverter = constructEntryConverter(config, environment);
         FileManager files = constructFileManager(config);
 
         _serviceStuff = constructServiceStuff(config, _timeMaster,
                entryConverter, files);
         if (_testMode) {
             _serviceStuff.markAsTest();
         }
         
         /* Let's try opening up BDB-JE: must have been created,
          * and have tables we expect; otherwise we'll fail right away.
          */
         LOG.info("Trying open Stores (StorableStore, node store, last-access store)");
          _stores = _constructStores(_serviceStuff);
          _managed.add(_stores);
         LOG.info("Opened Stores successfully");
         _stores.initAndOpen(false);
 
         // Then: read in cluster information (config file, BDB):
         final int port = dwConfig.getHttpConfiguration().getPort();
         LOG.info("Initializing cluster configuration (port {})...", port);
         final long startTime = _timeMaster.currentTimeMillis();
         ClusterViewByServerImpl<K,E> cl = new ClusterBootstrapper<K,E>(startTime, _serviceStuff, _stores)
                 .bootstrap(port);
         _cluster = cl;
         _managed.add(_cluster);
      
         LOG.info("Cluster configuration setup complete, with {} nodes", _cluster.size());
         
         // Index page must be done via resource, otherwise will conflict with DW/JAX-RS Servlet:
         environment.addResource(new IndexResource(loadResource("/index.html"), loadResource("/favicon.jpg")));
 
         // Let's first construct handlers we use:
         LOG.info("Creating handlers for service endpoints");
         ClusterInfoHandler nodeH = constructClusterInfoHandler(_serviceStuff, _cluster);
         SyncHandler<K,E> syncH = constructSyncHandler(_serviceStuff, _stores, _cluster);
        _storeHandler = constructStoreHandler(_serviceStuff, _stores);
         
         LOG.info("Adding service end points");
         addServiceEndpoints(_serviceStuff, environment,
                 nodeH, syncH, _storeHandler);
 
         LOG.info("Adding health checks");
         addHealthChecks(_serviceStuff, environment);
 
         LOG.info("Initializing background cleaner tasks");
         _cleanerUpper = new CleanerUpper<K,E>(_serviceStuff, _stores, _cluster);
         _managed.add(_cleanerUpper);
         
         LOG.info("Initialization complete: HTTP service now running on port {}",
                 dwConfig.getHttpConfiguration().getPort());
     }
 
     /*
     /**********************************************************************
     /* Factory methods: basic config objects
     /**********************************************************************
      */
     
     /**
      * Overridable method that is used for getting helper object used for
      * constructing {@link StoredEntry} instances to store in the
      * entry metadata store (currently BDB).
      */
     protected abstract StoredEntryConverter<K,E> constructEntryConverter(SCONFIG config,
             Environment environment);
 
     protected abstract FileManager constructFileManager(SCONFIG serviceConfig);
 
     protected abstract StoresImpl<K,E> constructStores(SharedServiceStuff stuff,
             SCONFIG serviceConfig, StorableStore store);    
 
     protected abstract SharedServiceStuff constructServiceStuff(SCONFIG serviceConfig,
             TimeMaster timeMaster, StoredEntryConverter<K,E> entryConverter,
             FileManager files);
     
     /*
     /**********************************************************************
     /* Factory methods for constructing handlers
     /**********************************************************************
      */
 
     protected abstract StoreHandler<K,E> constructStoreHandler(SharedServiceStuff serviceStuff,
            Stores<K,E> stores);
 
     protected SyncHandler<K,E> constructSyncHandler(SharedServiceStuff stuff,
             StoresImpl<K,E> stores, ClusterViewByServerUpdatable cluster)
     {
         return new SyncHandler<K,E>(stuff, stores, cluster);
     }
 
     protected ClusterInfoHandler constructClusterInfoHandler(SharedServiceStuff stuff,
             ClusterViewByServerUpdatable cluster)
     {
         return new ClusterInfoHandler(stuff, cluster);
     }
 
     /*
     /**********************************************************************
     /* Factory methods: servlets
     /**********************************************************************
      */
 
     protected abstract StoreEntryServlet<K,E> constructStoreEntryServlet(SharedServiceStuff stuff,
             ClusterViewByServer cluster, StoreHandler<K,E> storeHandler);
 
     protected NodeStatusServlet constructNodeStatusServlet(SharedServiceStuff stuff, ClusterInfoHandler nodeHandler) {
         return new NodeStatusServlet(stuff, nodeHandler);
     }
 
     protected SyncListServlet<K,E> constructSyncListServlet(SharedServiceStuff stuff,
             ClusterViewByServer cluster, SyncHandler<K,E> syncHandler) {
         return new SyncListServlet<K,E>(stuff, cluster, syncHandler);
     }
 
     protected SyncPullServlet<K,E> constructSyncPullServlet(SharedServiceStuff stuff,
             ClusterViewByServer cluster, SyncHandler<K,E> syncHandler) {
         return new SyncPullServlet<K,E>(stuff, cluster, syncHandler);
     }
     
     /*
     /**********************************************************************
     /* Methods for service end point additions
     /**********************************************************************
      */
 
     /**
      * Method called to create service endpoints, given set of
      * handlers.
      */
     protected void addServiceEndpoints(SharedServiceStuff stuff,
             Environment environment,
             ClusterInfoHandler nodeHandler, SyncHandler<K,E> syncHandler,
             StoreHandler<K,E> storeHandler)
     {
         final ClusterViewByServer cluster = syncHandler.getCluster();
         NodeStatusServlet nodeStatusServlet = constructNodeStatusServlet(stuff, nodeHandler);
         SyncListServlet<K,E> syncListServlet = constructSyncListServlet(
                 stuff, cluster, syncHandler);
         SyncPullServlet<K,E> syncPullServlet = constructSyncPullServlet(
                 stuff, cluster, syncHandler);
         StoreEntryServlet<K,E> storeEntryServlet = constructStoreEntryServlet(stuff,
                 cluster, storeHandler);
         
         ServiceDispatchServlet<K,E> dispatcher = new ServiceDispatchServlet<K,E>(
                 cluster, stuff,
                 nodeStatusServlet, storeEntryServlet, null,
                 syncListServlet, syncPullServlet);
 
         RequestPathBuilder rootBuilder = rootPath(stuff.getServiceConfig());
         String rootPath = servletPath(rootBuilder);
         LOG.info("Registering main Dispatcher servlet at: {}", rootPath);
         environment.addServlet(dispatcher, rootPath);
 
         // // And finally servlet for for entry access
         
         addStoreEntryServlet(stuff, environment, cluster, _storeHandler);
 
     }
 
     /*
      * Old single-Servlet implementation:
      */
     /*
     {
         final ServiceConfig config = stuff.getServiceConfig();
         final ClusterViewByServer cluster = syncHandler.getCluster();
 
         // All paths are dynamic, so we need a mapper:
         RequestPathStrategy pathStrategy = stuff.getPathStrategy();
         RequestPathBuilder pathBuilder;
 
         pathBuilder = pathStrategy.appendNodeStatusPath(rootPath(config));
 
         // // And then start by adding cluster-info (aka node status)
         // // and sync end points as plain servlets
         
         NodeStatusServlet nsServlet = constructNodeStatusServlet(nodeHandler);
         if (nsServlet != null) {
             environment.addServlet(nsServlet, servletPath(pathBuilder));
         }
     
         pathBuilder = pathStrategy.appendSyncListPath(rootPath(config));
         environment.addServlet(new SyncListServlet<K,E>(stuff, cluster, syncHandler),
                 servletPath(pathBuilder));
         pathBuilder = pathStrategy.appendSyncPullPath(rootPath(config));
         environment.addServlet(new SyncPullServlet<K,E>(stuff, cluster, syncHandler),
                 servletPath(pathBuilder));
 
         // // And finally servlet for for entry access
         
         addStoreEntryServlet(stuff, environment, pathStrategy, cluster, _storeHandler);
     }
     */
     
     protected void addStoreEntryServlet(SharedServiceStuff stuff,
             Environment environment,
             ClusterViewByServer cluster,
             StoreHandler<K,E> storeHandler)
     {
     }
     
     protected void addHealthChecks(SharedServiceStuff stuff,
             Environment environment) { }
     
     /*
     /**********************************************************************
     /* Internal methods
     /**********************************************************************
      */
     
     protected StoresImpl<K,E> _constructStores(SharedServiceStuff stuff)
         throws IOException
     {
         final SCONFIG v = stuff.getServiceConfig();
         StoreBackendBuilder<?> b = v.instantiateBackendBuilder();
         StoreBackendConfig backendConfig = v._storeBackendConfigOverride;
         if (backendConfig == null) { // no overrides, use databinding
             Class<? extends StoreBackendConfig> cfgType = b.getConfigClass();
             if (v.storeBackendConfig == null) {
                 throw new IllegalStateException("Missing 'v.storeBackendConfig");
             }
             backendConfig = stuff.convertValue(v.storeBackendConfig, cfgType);
         }
         StoreBackend backend = b.with(v.storeConfig)
                 .with(backendConfig)
                 .build();
         StorableStore store = new StorableStoreImpl(v.storeConfig, backend, _timeMaster,
                stuff.getFileManager());
         return constructStores(stuff, v, store);
     }
 
     @Override
     public void start() throws Exception
     {
         LOG.info("Starting up {} VManaged objects", _managed.size());
         for (StartAndStoppable managed : _managed) {
             LOG.info("Starting up: {}", managed.getClass().getName());
             managed.start();
         }
         // TODO Auto-generated method stub
         LOG.info("VManaged object startup complete");
         
     }
 
     @Override
     public void stop() throws Exception
     {
         int count = _managed.size();
         LOG.info("Stopping {} VManaged objects", _managed.size());
         while (--count >= 0) {
             StartAndStoppable managed = _managed.remove(count);
             String desc = managed.getClass().getName();
             try {
                 LOG.info("Stopping: {}", desc);
                 managed.stop();
             } catch (Exception e) {
                 LOG.warn(String.format("Problems trying to stop VManaged of type %s: (%s) %s",
                         desc, e.getClass().getName(), e.getMessage()),
                         e);
             }
         }
         LOG.info("VManaged object shutdown complete");
     }
 
     /*
     /**********************************************************************
     /* Accessors for tests
     /**********************************************************************
      */
 
     public TimeMaster getTimeMaster() {
         return _timeMaster;
     }
     
     /*
     /**********************************************************************
     /* Helper methods
     /**********************************************************************
      */
 
     protected RequestPathBuilder rootPath(ServiceConfig config)
     {
         return new JdkHttpClientPathBuilder("localhost")
             .addPathSegments(config.servicePathRoot);
     }
 
     /**
      * Helper method for constructing Servlet registration path, given
      * a basic end point path definition. Currently just verifies prefix
      * and suffix slashes and adds '*' as necessary.
      */
     protected String servletPath(RequestPathBuilder pathBuilder)
     {
         String base = pathBuilder.getPath();
         if (!base.endsWith("*")) {
             if (base.endsWith("/")) {
                 base += "*";
             } else {
                 base += "/*";
             }
         }
         if (!base.startsWith("/")) {
             base = "/"+base;
         }
         return base;
     }
 
     protected byte[] loadResource(String ref) throws IOException
     {
         ByteArrayOutputStream bytes = new ByteArrayOutputStream(4000);
         InputStream in = getClass().getResourceAsStream(ref);
         byte[] buffer = new byte[4000];
         int count;
      
         while ((count = in.read(buffer)) > 0) {
             bytes.write(buffer, 0, count);
         }
         in.close();
         byte[] data = bytes.toByteArray();
         if (data.length == 0) {
             String msg = "Could not find resource '"+ref+"'";
             LOG.error(msg);
             throw new IllegalArgumentException(msg);
         }
         return bytes.toByteArray();
     }
 }
