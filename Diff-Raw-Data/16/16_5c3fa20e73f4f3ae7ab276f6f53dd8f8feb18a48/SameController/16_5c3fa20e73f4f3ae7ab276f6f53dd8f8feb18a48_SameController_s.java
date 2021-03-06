 package com.orbekk.same;
 
 import java.util.concurrent.ExecutorService;
 import java.util.concurrent.Executors;
 
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import com.google.protobuf.RpcCallback;
 import com.orbekk.paxos.PaxosServiceImpl;
 import com.orbekk.protobuf.Rpc;
 import com.orbekk.protobuf.SimpleProtobufServer;
 import com.orbekk.same.config.Configuration;
 
 public class SameController {
    private Logger logger = LoggerFactory.getLogger(getClass());
    private SimpleProtobufServer pServer;
    private Master master;
    private Client client;
    private PaxosServiceImpl paxos;
    private Configuration configuration;
    private ConnectionManager connections;
     private final RpcFactory rpcf;
 
     /**
      * Timeout for remote operations in milliseconds.
      */
     private static final int timeout = 10000;
 
     private MasterController masterController = new MasterController() {
         @Override
         public void enableMaster(State lastKnownState, int masterId) {
             String myLocation = configuration.get("localIp") + ":" +
                     configuration.get("pport");
             String masterUrl = configuration.get("baseUrl") +
                     "MasterService.json";
             master = Master.create(connections,
                     masterUrl, configuration.get("networkName"), myLocation,
                     rpcf);
             master.resumeFrom(lastKnownState, masterId);
             pServer.registerService(master.getNewService());
             master.start();
         }
 
         @Override
         public void disableMaster() {
             if (master != null) {
                 master.interrupt();
             }
         }
     };
     
     public static SameController create(Configuration configuration) {
         int pport = configuration.getInt("pport");
         String myLocation = configuration.get("localIp") + ":" + pport;
         
         ConnectionManagerImpl connections = new ConnectionManagerImpl(
                 timeout, timeout);
         RpcFactory rpcf = new RpcFactory(timeout);
         
         State clientState = new State(".InvalidClientNetwork");
         String baseUrl = String.format("http://%s:%s/",
                 configuration.get("localIp"), configuration.getInt("port"));
         String clientUrl = baseUrl + "ClientService.json";
 
         ExecutorService clientExecutor = Executors.newCachedThreadPool();
         Client client = new Client(clientState, connections,
                 clientUrl, myLocation, rpcf, clientExecutor);
         PaxosServiceImpl paxos = new PaxosServiceImpl("");
         
         SimpleProtobufServer pServer = SimpleProtobufServer.create(pport);
         pServer.registerService(client.getNewService());
         pServer.registerService(paxos.getService());
         
         SameController controller = new SameController(
                 configuration, connections, client,
                 paxos, pServer, rpcf);
         return controller;
     }
 
     public SameController(
             Configuration configuration,
             ConnectionManager connections,
             Client client,
             PaxosServiceImpl paxos,
             SimpleProtobufServer pServer,
             RpcFactory rpcf) {
         this.configuration = configuration;
         this.connections = connections;
         this.client = client;
         this.paxos = paxos;
         this.pServer = pServer;
         this.rpcf = rpcf;
     }
 
     public void start() throws Exception {
         pServer.start();
         client.setMasterController(masterController);
         client.start();
     }
 
     public void stop() {
         try {
             client.interrupt();
             if (master != null) {
                 master.interrupt();
             }
             pServer.interrupt();
         } catch (Exception e) {
             logger.error("Failed to stop webserver", e);
         }
     }
 
     public void join() {
         client.interrupt();
         if (master != null) {
             master.interrupt();
         }
     }
 
     public void createNetwork(String networkName) {
         masterController.disableMaster();
         masterController.enableMaster(new State(networkName), 1);
         joinNetwork(master.getMasterInfo());
     }
 
     public void joinNetwork(Services.MasterState masterInfo) {
         client.joinNetwork(masterInfo);
     }
 
     public Client getClient() {
         return client;
     }
 
     public Master getMaster() {
         return master;
     }
     
     public void registerCurrentNetwork() {
         registerNetwork(master);
     }
     
     public void registerNetwork(Master master) {
         Services.Directory directory = getDirectory();
         if (directory == null) {
             return;
         }
         Services.MasterState request = Services.MasterState.newBuilder()
                 .setNetworkName(master.getNetworkName())
                 .setMasterLocation(master.getLocation())
                 .build();
         final Rpc rpc = rpcf.create();
         RpcCallback<Services.Empty> done = new RpcCallback<Services.Empty>() {
             @Override public void run(Services.Empty unused) {
                 if (!rpc.isOk()) {
                     logger.warn("Failed to register network: {}", rpc);
                 }
             }
         };
         directory.registerNetwork(rpc, request, done);
     }
     
     public Services.Directory getDirectory() {
         String directoryLocation = configuration.get("directoryLocation");
         if (directoryLocation != null) {
             return connections.getDirectory(directoryLocation);
         } else {
             return null;
         }
     }
 
     public VariableFactory createVariableFactory() {
         return new VariableFactory(client.getInterface());
     }
 }
