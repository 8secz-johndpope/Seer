 package net.sf.katta.zk;
 
 import java.io.BufferedReader;
 import java.io.BufferedWriter;
 import java.io.File;
 import java.io.FileWriter;
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.InputStreamReader;
 import java.io.OutputStream;
 import java.net.ServerSocket;
 import java.net.Socket;
 
 import junit.framework.TestCase;
 import net.sf.katta.ZkServer;
 import net.sf.katta.util.ZkConfiguration;
 
 import com.yahoo.zookeeper.ZooKeeper;
 
 public abstract class ZkClientReconnectTest extends TestCase {
 
   int GATEWAY_PORT = 2190;
 
  public void testDisabled() throws Exception {
    // TODO jz: enable tests
  }

   class Gateway implements Runnable {
 
     private Socket _socket;
     private ServerSocket _serverSocket;
     private Socket _outgoingSocket;
 
     public void run() {
       try {
         _serverSocket = new ServerSocket(GATEWAY_PORT);
         _socket = _serverSocket.accept();
         System.out.println("new client is connected " + _socket.getInetAddress());
         final InputStream incomingInputStream = _socket.getInputStream();
         final OutputStream incomingOutputStream = _socket.getOutputStream();
 
         _outgoingSocket = new Socket("localhost", 2181);
         final InputStream outgoingInputStream = _outgoingSocket.getInputStream();
         final OutputStream outgoingOutputStream = _outgoingSocket.getOutputStream();
 
         Runnable runnable1 = new Runnable() {
           public void run() {
 
             try {
               int read = -1;
               while ((read = incomingInputStream.read()) != -1) {
                 outgoingOutputStream.write(read);
               }
             } catch (IOException e) {
               //
             }
           }
         };
 
         Runnable runnable2 = new Runnable() {
           public void run() {
             try {
               int read = -1;
               while ((read = outgoingInputStream.read()) != -1) {
                 incomingOutputStream.write(read);
               }
             } catch (IOException e) {
               //
             }
           }
         };
 
         Thread thread = new Thread(runnable1);
         thread.setDaemon(true);
         thread.start();
 
         Thread thread2 = new Thread(runnable2);
         thread2.setDaemon(true);
         thread2.start();
 
       } catch (Exception e) {
         e.printStackTrace();
       }
     }
 
     public void stop() {
       try {
         System.out.println("stop the gateway");
         _socket.close();
         _outgoingSocket.close();
         _serverSocket.close();
       } catch (Exception e) {
         e.printStackTrace();
       }
     }
 
   }
 
   public void disabled_testServerDown() throws Exception {
 
     ZkConfiguration configuration = new ZkConfiguration();
     ZkServer server = new ZkServer(configuration);
 
     ZKClient client = new ZKClient(configuration);
     client.start(30000);
 
     waitForStatus(client, ZooKeeper.States.CONNECTED, configuration.getZKTimeOut());
     assertTrue(client.getZookeeperStates().equals(ZooKeeper.States.CONNECTED));
     System.out.println("test client is connected");
 
     server.shutdown();
     waitForStatus(client, ZooKeeper.States.CONNECTING, configuration.getZKTimeOut());
     System.out.println("test client is re-connecting");
 
     server = new ZkServer(configuration);
     waitForStatus(client, ZooKeeper.States.CONNECTED, configuration.getZKTimeOut());
     assertTrue(client.getZookeeperStates().equals(ZooKeeper.States.CONNECTED));
     System.out.println("test client is connected");
 
     server.shutdown();
     client.close();
 
   }
 
   public void disabled_testNetworkDown() throws Exception {
 
     // write client property file
     File folder = new File(System.getProperty("java.io.tmpdir"), ZkClientReconnectTest.class.getName());
     folder.mkdir();
     File file = new File(folder, "zk.properties");
     BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
     InputStreamReader streamReader = new InputStreamReader(ZkClientReconnectTest.class
         .getResourceAsStream("/katta.zk.properties"));
     BufferedReader bufferedReader = new BufferedReader(streamReader);
     String line = null;
     while ((line = bufferedReader.readLine()) != null) {
       if (line.startsWith(ZkConfiguration.ZOOKEEPER_SERVERS)) {
         line = "zookeeper.servers=localhost:" + 2190;
       }
       bufferedWriter.write(line);
       bufferedWriter.write(System.getProperty("line.separator"));
     }
     bufferedWriter.close();
     bufferedReader.close();
 
     Gateway gateway = new Gateway();
     Thread thread = new Thread(gateway);
     thread.setDaemon(true);
     thread.start();
 
     ZkConfiguration serverConfiguration = new ZkConfiguration();
     ZkConfiguration clientConfiguration = new ZkConfiguration(file);
     ZkServer server = new ZkServer(serverConfiguration);
     ZKClient client = new ZKClient(clientConfiguration);
     client.start(2000);
 
     waitForStatus(client, ZooKeeper.States.CONNECTING, clientConfiguration.getZKTimeOut());
     waitForStatus(client, ZooKeeper.States.CONNECTING, clientConfiguration.getZKTimeOut());
 
     for (int i = 0; i < 3; i++) {
       System.out.println("test reconnect " + i);
       gateway = stopAndStartGateway(gateway, clientConfiguration, client);
     }
 
     gateway.stop();
     server.shutdown();
     client.close();
 
   }
 
   private Gateway stopAndStartGateway(Gateway gateway, ZkConfiguration clientConfiguration, ZKClient client)
       throws Exception {
 
     waitForStatus(client, ZooKeeper.States.CONNECTED, clientConfiguration.getZKTimeOut());
     assertEquals(ZooKeeper.States.CONNECTED, client.getZookeeperStates());
 
     gateway.stop();
 
     waitForStatus(client, ZooKeeper.States.CONNECTING, clientConfiguration.getZKTimeOut());
     assertEquals(ZooKeeper.States.CONNECTING, client.getZookeeperStates());
 
     gateway = new Gateway();
     Thread thread = new Thread(gateway);
     thread.setDaemon(true);
     thread.start();
 
     waitForStatus(client, ZooKeeper.States.CONNECTED, clientConfiguration.getZKTimeOut());
     assertEquals(ZooKeeper.States.CONNECTED, client.getZookeeperStates());
 
     return gateway;
   }
 
   private void waitForStatus(ZKClient client, ZooKeeper.States states, long timeOut) throws Exception {
     long endTime = System.currentTimeMillis() + timeOut;
 
     while ((endTime > System.currentTimeMillis()) && client.getZookeeperStates() != null
         && !(client.getZookeeperStates().name().equals(states.name()))) {
       Thread.sleep(500);
     }
   }
 
 }
