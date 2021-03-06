 package com.github.nkzawa.socketio.client;
 
 import com.github.nkzawa.emitter.Emitter;
 import com.google.gson.JsonObject;
 import org.junit.After;
 import org.junit.Before;
 import org.junit.Test;
 import org.junit.runner.RunWith;
 import org.junit.runners.JUnit4;
 
 import java.io.BufferedReader;
 import java.io.IOException;
 import java.io.InputStreamReader;
 import java.net.URISyntaxException;
 import java.util.concurrent.*;
 
 import static org.hamcrest.CoreMatchers.is;
 import static org.junit.Assert.assertThat;
 
 @RunWith(JUnit4.class)
 public class ServerConnectionTest {
 
     final static int TIMEOUT = 3000;
     final static int PORT = 3000;
 
     private Process serverProcess;
     private ExecutorService serverService;
     private Future serverOutout;
     private Future serverError;
     private Socket socket;
 
     @Before
     public void startServer() throws IOException, InterruptedException {
         System.out.println("Starting server ...");
 
         final CountDownLatch latch = new CountDownLatch(1);
         serverProcess = Runtime.getRuntime().exec(
                 String.format("node src/test/resources/index.js %s %s", PORT, nsp()),
                 new String[] {"DEBUG=socket.io:*"});
         serverService = Executors.newCachedThreadPool();
         serverOutout = serverService.submit(new Runnable() {
             @Override
             public void run() {
                 BufferedReader reader = new BufferedReader(
                         new InputStreamReader(serverProcess.getInputStream()));
                 String line;
                 try {
                     line = reader.readLine();
                     latch.countDown();
                     do {
                         System.out.println("SERVER OUT: " + line);
                     } while ((line = reader.readLine()) != null);
                 } catch (IOException e) {
                     e.printStackTrace();
                 }
             }
         });
         serverError = serverService.submit(new Runnable() {
             @Override
             public void run() {
                 BufferedReader reader = new BufferedReader(
                         new InputStreamReader(serverProcess.getErrorStream()));
                 String line;
                 try {
                     while ((line = reader.readLine()) != null) {
                         System.err.println("SERVER ERR: " + line);
                     }
                 } catch (IOException e) {
                     e.printStackTrace();
                 }
             }
         });
         latch.await(3000, TimeUnit.MILLISECONDS);
     }
 
     @After
     public void stopServer() throws InterruptedException {
         System.out.println("Stopping server ...");
         serverProcess.destroy();
         serverOutout.cancel(false);
         serverError.cancel(false);
         serverService.shutdown();
         serverService.awaitTermination(3000, TimeUnit.MILLISECONDS);
     }
 
     @Test(timeout = TIMEOUT)
     public void openAndClose() throws URISyntaxException, InterruptedException {
         final BlockingQueue<String> events = new LinkedBlockingQueue<String>();
 
         IO.Options opts = new IO.Options();
         opts.forceNew = true;
         socket = IO.socket(uri(), opts);
         socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
             @Override
             public void call(Object... objects) {
                 System.out.println("connect:");
                 events.offer("connect");
             }
         }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
             @Override
             public void call(Object... objects) {
                 System.out.println("disconnect:");
                 events.offer("disconnect");
             }
         });
         socket.connect();
 
         assertThat(events.take(), is("connect"));
         socket.disconnect();
         assertThat(events.take(), is("disconnect"));
     }
 
     @Test(timeout = TIMEOUT)
     public void message() throws URISyntaxException, InterruptedException {
         final BlockingQueue<Object[]> events = new LinkedBlockingQueue<Object[]>();
 
         IO.Options opts = new IO.Options();
         opts.forceNew = true;
         socket = IO.socket(uri(), opts);
         socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
             @Override
             public void call(Object... objects) {
                 System.out.println("connect:");
                 socket.send("foo", "bar");
             }
         }).on(Socket.EVENT_MESSAGE, new Emitter.Listener() {
             @Override
             public void call(Object... objects) {
                 System.out.println(String.format(
                         objects.length > 1 ? "message: %s, %s" : "message: %s", objects));
                 events.offer(objects);
             }
         });
         socket.connect();
 
         assertThat(events.take(), is(new Object[] {"hello client"}));
         assertThat(events.take(), is(new Object[] {"foo", "bar"}));
         socket.disconnect();
     }
 
     @Test(timeout = TIMEOUT)
     public void event() throws URISyntaxException, InterruptedException {
         final BlockingQueue<Object[]> events = new LinkedBlockingQueue<Object[]>();
 
         final JsonObject obj = new JsonObject();
         obj.addProperty("foo", 1);
 
         IO.Options opts = new IO.Options();
         opts.forceNew = true;
         socket = IO.socket(uri(), opts);
         socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
             @Override
             public void call(Object... objects) {
                 System.out.println("connect:");
                 socket.emit("echo", obj, null, "bar");
             }
         }).on("echoBack", new Emitter.Listener() {
             @Override
             public void call(Object... objects) {
                 System.out.println(String.format("echoBack: %s, %s, %s", objects));
                 events.offer(objects);
             }
         });
         socket.connect();
 
         assertThat(events.take(), is(new Object[] {obj, null, "bar"}));
         socket.disconnect();
     }
 
     @Test(timeout = TIMEOUT)
     public void ack() throws URISyntaxException, InterruptedException {
         final BlockingQueue<Object[]> events = new LinkedBlockingQueue<Object[]>();
 
         final JsonObject obj = new JsonObject();
         obj.addProperty("foo", 1);
 
         IO.Options opts = new IO.Options();
         opts.forceNew = true;
         socket = IO.socket(uri(), opts);
         socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
             @Override
             public void call(Object... objects) {
                 System.out.println("connect:");
                 socket.emit("ack", new Object[] {obj, "bar"}, new Ack() {
                     @Override
                     public void call(Object... args) {
                         System.out.println(String.format("ack: %s, %s", args));
                         events.offer(args);
                     }
                 });
             }
         });
         socket.connect();
 
         assertThat(events.take(), is(new Object[] {obj, "bar"}));
         socket.disconnect();
     }
 
    @Test(timeout = TIMEOUT)
    public void ackWithoutArgs() throws URISyntaxException, InterruptedException {
        final BlockingQueue<Object[]> events = new LinkedBlockingQueue<Object[]>();

        IO.Options opts = new IO.Options();
        opts.forceNew = true;
        socket = IO.socket(uri(), opts);
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... objects) {
                socket.emit("ack", null, new Ack() {
                    @Override
                    public void call(Object... args) {
                        System.out.println("ack: " + args);
                        events.offer(args);
                    }
                });
            }
        });
        socket.connect();

        assertThat(events.take(), is(new Object[] {}));
        socket.disconnect();
    }
 
     private String uri() {
         return "http://localhost:" + PORT + nsp();
     }
 
     protected String nsp() {
         return "/";
     }
 }
