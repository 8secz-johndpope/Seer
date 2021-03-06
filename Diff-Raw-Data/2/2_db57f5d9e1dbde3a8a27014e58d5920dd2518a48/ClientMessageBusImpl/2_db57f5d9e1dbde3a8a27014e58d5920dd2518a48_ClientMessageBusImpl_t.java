 /*
  * Copyright 2010 JBoss, a divison Red Hat, Inc
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *    http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
 
 package org.jboss.errai.bus.client.framework;
 
 import com.google.gwt.core.client.GWT;
 import com.google.gwt.dom.client.Style;
 import com.google.gwt.event.dom.client.ClickEvent;
 import com.google.gwt.event.dom.client.ClickHandler;
 import com.google.gwt.http.client.*;
 import com.google.gwt.user.client.Timer;
 import com.google.gwt.user.client.Window;
 import com.google.gwt.user.client.ui.*;
 import org.jboss.errai.bus.client.api.*;
 import org.jboss.errai.bus.client.api.base.Capabilities;
 import org.jboss.errai.bus.client.api.base.MessageBuilder;
 import org.jboss.errai.bus.client.api.base.TransportIOException;
 import org.jboss.errai.bus.client.ext.ExtensionsLoader;
 import org.jboss.errai.bus.client.protocols.BusCommands;
 import org.jboss.errai.bus.client.protocols.MessageParts;
 
 import java.util.*;
 
 import static org.jboss.errai.bus.client.json.JSONUtilCli.decodePayload;
 import static org.jboss.errai.bus.client.json.JSONUtilCli.encodeMap;
 import static org.jboss.errai.bus.client.protocols.BusCommands.RemoteSubscribe;
 import static org.jboss.errai.bus.client.protocols.MessageParts.*;
 
 /**
  * The default client <tt>MessageBus</tt> implementation.  This bus runs in the browser and automatically federates
  * with the server immediately upon initialization.
  */
 public class ClientMessageBusImpl implements ClientMessageBus {
     private static final int HEARTBEAT_DELAY = 20000;
 
     private String clientId;
 
     /* The encoded URL to be used for the bus */
     private static final String SERVICE_ENTRY_POINT = "in.erraiBus";
 
     /* ArrayList of all subscription listeners */
     private List<SubscribeListener> onSubscribeHooks;
 
     /* ArrayList of all unsubscription listeners */
     private List<UnsubscribeListener> onUnsubscribeHooks;
 
     /* Used to build the HTTP POST request */
     private RequestBuilder sendBuilder;
 
     /* Used to build the HTTP GET request */
     private RequestBuilder recvBuilder;
 
     /* Map of subjects to subscriptions  */
     private Map<String, List<Object>> subscriptions;
 
     private Set<String> remote;
 
     /* Outgoing queue of messages to be transmitted */
     // private final Queue<Message> outgoingQueue = new LinkedList<Message>();
 
     /* Map of subjects to references registered in this session */
     private Map<String, List<Object>> registeredInThisSession = new HashMap<String, List<Object>>();
 
     /* A list of {@link Runnable} initialization tasks to be executed after the bus has successfully finished it's
      * initialization and is now communicating with the remote bus. */
     private List<Runnable> postInitTasks = new ArrayList<Runnable>();
     private List<Message> deferredMessages;
 
     /* The timer constantly ensures the client's polling with the server is active */
     private Timer incomingTimer;
     private Timer heartBeatTimer;
 
     /* True if the client's message bus has been initialized */
     private boolean initialized = false;
     private boolean postInit = false;
 
     private long lastTransmit = 0;
 
     private boolean disconnected = false;
 
     ProxySettings proxySettings;
 
     class ProxySettings {
         final String url = GWT.getModuleBaseURL() + "proxy";
         boolean hasProxy = false;
     }
 
     private LogAdapter logAdapter = new LogAdapter() {
         public void warn(String message) {
             GWT.log("WARN: " + message, null);
         }
 
         public void info(String message) {
             GWT.log("INFO: " + message, null);
         }
 
         public void debug(String message) {
             GWT.log("DEBUG: " + message, null);
         }
 
         public void error(String message, Throwable t) {
             showError(message, t);
         }
     };
 
     private BusErrorDialog errorDialog;
 
     public ClientMessageBusImpl() {
         init();
     }
 
     /**
      * Constructor creates sendBuilder for HTTP POST requests, recvBuilder for HTTP GET requests and
      * initializes the message bus.
      */
 
 
     private void createRequestBuilders() {
         sendBuilder = getSendBuilder();
         recvBuilder = getRecvBuilder();
 
         logAdapter.debug("Connecting Errai at URL " + sendBuilder.getUrl());
     }
 
     private RequestBuilder getSendBuilder() {
         String endpoint = proxySettings.hasProxy ? proxySettings.url : SERVICE_ENTRY_POINT;
 
         RequestBuilder builder = new RequestBuilder(
                 RequestBuilder.POST,
                 URL.encode(endpoint)
         );
 
      //   builder.setHeader("Connection", "Keep-Alive");
         builder.setHeader("Content-Type", "application/json");
         builder.setHeader(ClientMessageBus.REMOTE_QUEUE_ID_HEADER, clientId);
 
         return builder;
     }
 
     private RequestBuilder getRecvBuilder() {
         String endpoint = proxySettings.hasProxy ? proxySettings.url : SERVICE_ENTRY_POINT;
 
         RequestBuilder builder = new RequestBuilder(
                 RequestBuilder.GET,
                 URL.encode(endpoint)
         );
 
      //   builder.setHeader("Connection", "Keep-Alive");
         builder.setHeader("Content-Type", "application/json");
         builder.setHeader(ClientMessageBus.REMOTE_QUEUE_ID_HEADER, clientId);
 
         return builder;
     }
 
 
     /**
      * Removes all subscriptions attached to the specified subject
      *
      * @param subject - the subject to have all it's subscriptions removed
      */
     public void unsubscribeAll(String subject) {
         if (subscriptions.containsKey(subject)) {
             for (Object o : subscriptions.get(subject)) {
                 _unsubscribe(o);
             }
 
             fireAllUnSubscribeListeners(subject);
         }
     }
 
     /**
      * Add a subscription for the specified subject
      *
      * @param subject  - the subject to add a subscription for
      * @param callback - function called when the message is dispatched
      */
     public void subscribe(final String subject, final MessageCallback callback) {
         if ("ServerBus".equals(subject) && subscriptions.containsKey("ServerBus")) return;
 
         if (!postInit) {
             postInitTasks.add(new Runnable() {
                 public void run() {
                     subscribe(subject, callback);
                 }
             });
 
             return;
         }
 
         logAdapter.debug("new subscription: " + subject + " -> " + callback);
 
         fireAllSubscribeListeners(subject);
 
         directSubscribe(subject, callback);
     }
 
     private void directSubscribe(final String subject, final MessageCallback callback) {
         addSubscription(subject, _subscribe(subject, new MessageCallback() {
             public void callback(Message message) {
                 try {
                     callback.callback(message);
                 }
                 catch (Exception e) {
                     logError("receiver '" + subject + "' threw an exception", decodeCommandMessage(message), e);
                 }
             }
         }, null));
     }
 
     /**
      * Fire listeners to notify that a new subscription has been registered on the bus.
      *
      * @param subject - new subscription registered
      */
     private void fireAllSubscribeListeners(String subject) {
         Iterator<SubscribeListener> iter = onSubscribeHooks.iterator();
         SubscriptionEvent evt = new SubscriptionEvent(false, "InBrowser", 1, subject);
 
         while (iter.hasNext()) {
             iter.next().onSubscribe(evt);
             if (evt.isDisposeListener()) {
                 iter.remove();
                 evt.setDisposeListener(false);
             }
         }
     }
 
     /**
      * Fire listeners to notify that a subscription has been unregistered from the bus
      *
      * @param subject - subscription unregistered
      */
     private void fireAllUnSubscribeListeners(String subject) {
         Iterator<UnsubscribeListener> iter = onUnsubscribeHooks.iterator();
         SubscriptionEvent evt = new SubscriptionEvent(false, "InBrowser", 0, subject);
 
         while (iter.hasNext()) {
             iter.next().onUnsubscribe(evt);
             if (evt.isDisposeListener()) {
                 iter.remove();
                 evt.setDisposeListener(false);
             }
         }
     }
 
     private static int conversationCounter = 0;
 
     /**
      * Have a single two-way conversation
      *
      * @param message  - The message to be sent in the conversation
      * @param callback - The function to be called when the message is received
      */
     public void conversationWith(final Message message, final MessageCallback callback) {
         final String tempSubject = "temp:Conversation:" + (++conversationCounter);
 
         message.set(ReplyTo, tempSubject);
 
         subscribe(tempSubject, new MessageCallback() {
             public void callback(Message message) {
                 unsubscribeAll(tempSubject);
                 callback.callback(message);
             }
         });
 
         send(message);
     }
 
     /**
      * Globally send message to all receivers.
      *
      * @param message - The message to be sent.
      */
     public void sendGlobal(Message message) {
         send(message);
     }
 
     /**
      * Sends the specified message, and notifies the listeners.
      *
      * @param message       - the message to be sent
      * @param fireListeners - true if the appropriate listeners should be fired
      */
     public void send(Message message, boolean fireListeners) {
         // TODO: fire listeners?
 
         send(message);
     }
 
     /**
      * Sends the message using it's encoded subject. If the bus has not been initialized, it will be added to
      * <tt>postInitTasks</tt>.
      *
      * @param message -
      * @throws RuntimeException - if message does not contain a ToSubject field or if the message's callback throws
      *                          an error.
      */
     public void send(final Message message) {
         message.commit();
         try {
             if (message.hasPart(MessageParts.ToSubject)) {
                 if (!initialized) {
                     deferredMessages.add(message);
                 } else {
                     if (!subscriptions.containsKey(message.getSubject())) {
                         logError("No subscribers for: " + message.getSubject(),
                                 "Attempt to send message to subject for which there are no subscribers", null);
                         return;
                     }
 
                     directStore(message);
                 }
 
             } else {
                 throw new RuntimeException("Cannot send message using this method" +
                         " if the message does not contain a ToSubject field.");
             }
         }
         catch (RuntimeException e) {
             if (message.getErrorCallback() != null) {
                 if (!message.getErrorCallback().error(message, e)) {
                     return;
                 }
             }
             throw e;
         }
     }
 
     private void directStore(final Message message) {
         _store(message.getSubject(), message instanceof HasEncoded
                 ? ((HasEncoded) message).getEncoded() : encodeMap(message.getParts()));
     }
 
 
     /**
      * Add message to the queue that remotely transmits messages to the server.
      * All messages in the queue are then sent.
      *
      * @param message -
      */
     private void encodeAndTransmit(Message message) {
         //outgoingQueue.add(message);
         transmitRemote(message instanceof HasEncoded ?
                 ((HasEncoded) message).getEncoded() : encodeMap(message.getParts()), message);
     }
 
     private void addSubscription(String subject, Object reference) {
         if (!subscriptions.containsKey(subject)) {
             subscriptions.put(subject, new ArrayList<Object>());
         }
 
         if (registeredInThisSession != null && !registeredInThisSession.containsKey(subject)) {
             registeredInThisSession.put(subject, new ArrayList<Object>());
         }
 
         subscriptions.get(subject).add(reference);
         if (registeredInThisSession != null) registeredInThisSession.get(subject).add(reference);
     }
 
     /**
      * Checks if subject is already listed in the subscriptions map
      *
      * @param subject - subject to look for
      * @return true if the subject is already subscribed
      */
     public boolean isSubscribed(String subject) {
         return subscriptions.containsKey(subject);
     }
 
     /**
      * Retrieve all registrations that have occured during the current capture context.
      * <p/>
      * The Map returned has the subject of the registrations as the key, and Sets of registration objects as the
      * value of the Map.
      *
      * @return A map of registrations captured in the current capture context.
      */
     public Map<String, List<Object>> getCapturedRegistrations() {
         return registeredInThisSession;
     }
 
     /**
      * Marks the beginning of a new capture context.<p/>  From this point, the message is called forward, all
      * registration events which occur will be captured.
      */
     public void beginCapture() {
         registeredInThisSession = new HashMap<String, List<Object>>();
     }
 
     /**
      * End the current capturing context.
      */
     public void endCapture() {
         registeredInThisSession = null;
     }
 
     /**
      * Unregister all registrations in the specified Map.<p/>  It accepts a Map format returned from
      * {@link #getCapturedRegistrations()}.
      *
      * @param all A map of registrations to deregister.
      */
     public void unregisterAll(Map<String, List<Object>> all) {
         for (Map.Entry<String, List<Object>> entry : all.entrySet()) {
             for (Object o : entry.getValue()) {
                 subscriptions.get(entry.getKey()).remove(o);
                 _unsubscribe(o);
             }
 
             if (subscriptions.get(entry.getKey()).isEmpty()) {
                 fireAllUnSubscribeListeners(entry.getKey());
             }
         }
     }
 
     /**
      * Transmits JSON string containing message, using the <tt>sendBuilder</tt>
      *
      * @param message   - JSON string representation of message
      * @param txMessage - Message reference.
      */
     private void transmitRemote(final String message, final Message txMessage) {
         if (message == null) return;
 
         try {
             sendBuilder.sendRequest(message, new RequestCallback() {
                 public void onResponseReceived(Request request, Response response) {
                     if (503 == response.getStatusCode()) // Service Unavailable
                     {
                         // Sending the message failed.
                         // Although the response may still be valid
                         // Handle it gracefully
                         //noinspection ThrowableInstanceNeverThrown
 
                         TransportIOException tioe = new TransportIOException(response.getText(), response.getStatusCode(), "Failure communicating with server");
                         if (txMessage.getErrorCallback() == null || txMessage.getErrorCallback().error(txMessage, tioe)) {
                             logError("Problem communicating with remote bus (Received HTTP 503 Error)", message, tioe);
                         }
                     }
 
                     /**
                      * If the server bus returned us some client-destined messages
                      * in response to our send, handle them now.
                      */
                     try {
                         procIncomingPayload(response);
                     }
                     catch (Throwable e) {
                         e.printStackTrace();
                         logError("Problem decoding incoming message:", response.getText(), e);
                     }
 
                 }
 
                 public void onError(Request request, Throwable exception) {
                     exception.printStackTrace();
                     if (txMessage.getErrorCallback() == null || txMessage.getErrorCallback().error(txMessage, exception)) {
                         logError("Failed to communicate with remote bus", "", exception);
                     }
                 }
             });
         }
         catch (Exception e) {
             e.printStackTrace();
         }
 
         lastTransmit = System.currentTimeMillis();
     }
 
     /**
      * Initializes client message bus without a callback function
      */
     public void init() {
         init(null);
 
     }
 
     public void stop(boolean sendDisconnect) {
         if (sendDisconnect) {
             sendBuilder.setHeader("phase", "disconnect");
 
             Message m = MessageBuilder.createMessage()
                     .toSubject("ServerBus")
                     .command(BusCommands.Disconnect).getMessage();
 
             encodeAndTransmit(m);
         }
 
         for (Map.Entry<String, List<Object>> entry : subscriptions.entrySet()) {
             for (Object o : entry.getValue()) {
                 _unsubscribe(o);
             }
         }
 
         MessageBuilder.createMessage()
                 .toSubject("ClientBus")
                 .command(BusCommands.RemoteMonitorAttach)
                 .done().sendNowWith(this);
 
         this.heartBeatTimer.cancel();
         this.incomingTimer.cancel();
         this.incomingTimer = null;
         this.disconnected = true;
         this.initialized = false;
         this.sendBuilder = null;
         this.recvBuilder = null;
         this.postInitTasks.clear();
     }
 
     public class RemoteMessageCallback implements MessageCallback {
         public void callback(Message message) {
             encodeAndTransmit(message);
         }
     }
 
     private void initFields() {
         initialized = false;
         disconnected = false;
 
         clientId = String.valueOf(com.google.gwt.user.client.Random.nextInt(1000))
                 + "-" + (System.currentTimeMillis() % 1000);
 
         onSubscribeHooks = new ArrayList<SubscribeListener>();
         onUnsubscribeHooks = new ArrayList<UnsubscribeListener>();
         subscriptions = new HashMap<String, List<Object>>();
         remote = new HashSet<String>();
         deferredMessages = new ArrayList<Message>();
 
     }
 
 
     public final MessageCallback REMOTE_CALLBACK = new RemoteMessageCallback();
 
     /**
      * Initializes the message bus, by subscribing to the ClientBus (to receive subscription messages) and the
      * ClientErrorBus to dispatch errors when called.
      *
      * @param callback - callback function used for to send the initial message to connect to the queue.
      */
     public void init(final HookCallback callback) {
         if (sendBuilder == null) {
             proxySettings = new ProxySettings();
 
            if (!GWT.isScript()) {   // Hosted Mode
 
                 RequestBuilder bootstrap = new RequestBuilder(RequestBuilder.GET, proxySettings.url);
                 try {
                     bootstrap.sendRequest(null, new RequestCallback() {
                         public void onResponseReceived(Request request, Response response) {
                             if (200 == response.getStatusCode()) {
                                 proxySettings.hasProxy = true;
                                 logAdapter.debug("Identified proxy at " + proxySettings.url);
                             }
 
                             initFields();
                             createRequestBuilders();
                             init(callback);
                         }
 
                         public void onError(Request request, Throwable exception) {
                             throw new RuntimeException("Client bootstrap failed", exception);
                         }
                     });
                 }
                 catch (RequestException e) {
                     logError("Bootstrap proxy settings failed", proxySettings.url, e);
                 }
             } else {
                 initFields();
                 createRequestBuilders();
             }
         }
 
         if (sendBuilder == null) return;
 
         directSubscribe("ClientBus", new MessageCallback() {
             @SuppressWarnings({"unchecked"})
             public void callback(final Message message) {
                 switch (BusCommands.valueOf(message.getCommandType())) {
                     case RemoteSubscribe:
                         if (message.hasPart("SubjectsList")) {
                             for (String subject : (List<String>) message.get(List.class, "SubjectsList")) {
                                 subscribe(subject, REMOTE_CALLBACK);
                                 remote.add(subject);
                             }
                         } else {
                             String subject = message.get(String.class, Subject);
                             subscribe(subject, REMOTE_CALLBACK);
                             remote.add(subject);
                         }
                         break;
 
                     case RemoteUnsubscribe:
                         unsubscribeAll(message.get(String.class, Subject));
                         break;
 
                     case CapabilitiesNotice:
                         String[] capabilites = message.get(String.class, "Flags").split(",");
 
                         for (String capability : capabilites) {
                             switch (Capabilities.valueOf(capability)) {
                                 case LongPollAvailable:
                                     COMM_CALLBACK = new LongPollRequestCallback();
                                     break;
                                 case NoLongPollAvailable:
                                     POLL_FREQUENCY = 500;
                                     break;
                             }
                         }
 
                         break;
 
                     case RemoteMonitorAttach:
                         break;
 
                     case FinishStateSync:
                         List<String> subjects = new ArrayList<String>();
                         for (String s : subscriptions.keySet()) {
                             if (s.startsWith("local:")) continue;
                             if (!remote.contains(s)) subjects.add(s);
                         }
 
                         directSubscribe("ServerBus", REMOTE_CALLBACK);
 
                         MessageBuilder.createMessage()
                                 .toSubject("ServerBus")
                                 .command(RemoteSubscribe)
                                 .with("SubjectsList", subjects)
                                 .with(PriorityProcessing, "1")
                                 .noErrorHandling()
                                 .sendNowWith(ClientMessageBusImpl.this);
 
 
                         MessageBuilder.createMessage()
                                 .toSubject("ServerBus")
                                 .command(BusCommands.FinishStateSync)
                                 .with(PriorityProcessing, "1")
                                 .noErrorHandling().sendNowWith(ClientMessageBusImpl.this);
 
                         /**
                          * ... also send RemoteUnsubscribe signals.
                          */
 
                         addSubscribeListener(new SubscribeListener() {
                             public void onSubscribe(SubscriptionEvent event) {
                                 if (event.getSubject().startsWith("local:") || remote.contains(event.getSubject())) {
                                     return;
                                 }
 
                                 MessageBuilder.getMessageProvider().get().command(RemoteSubscribe)
                                         .toSubject("ServerBus")
                                         .set(Subject, event.getSubject())
                                         .set(PriorityProcessing, "1")
                                         .sendNowWith(ClientMessageBusImpl.this);
                             }
                         });
 
                         addUnsubscribeListener(new UnsubscribeListener() {
                             public void onUnsubscribe(SubscriptionEvent event) {
                                 MessageBuilder.getMessageProvider().get().command(BusCommands.RemoteUnsubscribe)
                                         .toSubject("ServerBus")
                                         .set(Subject, event.getSubject())
                                         .set(PriorityProcessing, "1")
                                         .sendNowWith(ClientMessageBusImpl.this);
                             }
                         });
 
                         subscribe("ClientBusErrors", new MessageCallback() {
                             public void callback(Message message) {
                                 logError(message.get(String.class, "ErrorMessage"),
                                         message.get(String.class, "AdditionalDetails"), null);
                             }
                         });
 
                         postInit = true;
                         logAdapter.debug("Executing " + postInitTasks.size() + " post init task(s)");
                         for (int i = 0; i < postInitTasks.size(); i++) {
                             try {
                                 postInitTasks.get(i).run();
                             }
                             catch (Throwable t) {
                                 t.printStackTrace();
                                 throw new RuntimeException("error running task", t);
                             }
                         }
 
                         sendAllDeferred();
                         postInitTasks.clear();
 
                         initialized = true;
 
 
                         break;
 
                     case Disconnect:
                         stop(false);
 
                         if (message.hasPart("Reason")) {
                             logError("The bus was disconnected by the server", "Reason: "
                                     + message.get(String.class, "Reason"), null);
                         }
                         break;
                 }
             }
         });
 
         /**
          * Send initial message to connect to the queue, to establish an HTTP session. Otherwise, concurrent
          * requests will result in multiple sessions being created.  Which is bad.  Avoid this at all costs.
          * Please.
          */
         if (!sendInitialMessage(callback)) {
             logError("Could not connect to remote bus", "", null);
         }
     }
 
     private void sendAllDeferred() {
         for (Iterator<Message> iter = deferredMessages.iterator(); iter.hasNext();) {
             Message m = iter.next();
             if (m.hasPart(MessageParts.PriorityProcessing)) {
                 directStore(m);
                 iter.remove();
             }
         }
 
         for (Iterator<Message> iter = deferredMessages.iterator(); iter.hasNext();) {
             directStore(iter.next());
             iter.remove();
         }
     }
 
     /**
      * Sends the initial message to connect to the queue, to estabish an HTTP session. Otherwise, concurrent
      * requests will result in multiple sessions being created.
      *
      * @param callback - callback function used for initializing the message bus
      * @return true if initial message was sent successfully.
      */
     private boolean sendInitialMessage(final HookCallback callback) {
         try {
             String initialMessage = "{\"CommandType\":\"ConnectToQueue\",\"ToSubject\":\"ServerBus\"," +
                     " \"PriorityProcessing\":\"1\"}";
 
             RequestBuilder initialRequest = getSendBuilder();
             initialRequest.setHeader("phase", "connection");
 
             initialRequest.sendRequest(initialMessage, new RequestCallback() {
                 public void onResponseReceived(Request request, Response response) {
                     try {
                         procIncomingPayload(response);
                         initializeMessagingBus(callback);
                     }
                     catch (Exception e) {
                         e.printStackTrace();
                         System.out.println("**DID NOT ATTACH**");
                         logError("Error attaching to bus", e.getMessage() + "<br/>Message Contents:<br/>"
                                 + response.getText(), e);
                     }
                 }
 
                 public void onError(Request request, Throwable exception) {
                     logError("Could not connect to remote bus", "", exception);
                 }
             });
 
 
         }
         catch (RequestException e) {
             e.printStackTrace();
             return false;
         }
         return true;
     }
 
     /**
      * Returns true if client message bus is initialized.
      *
      * @return true if client message bus is initialized.
      */
     public boolean isInitialized() {
         return initialized;
     }
 
     int maxRetries = 5;
     int retries = 0;
     int timeout = 2000;
     int statusCode = 0;
     DialogBox timeoutDB;
     Label timeoutMessage;
 
     private void createConnectAttemptGUI() {
         timeoutDB = new DialogBox();
         timeoutMessage = new Label();
         timeoutDB.add(timeoutMessage);
         RootPanel.get().add(timeoutDB);
         timeoutDB.show();
         timeoutDB.center();
     }
 
     private void clearConnectAttemptGUI() {
         timeoutDB.hide();
         RootPanel.get().remove(timeoutDB);
         timeoutDB = null;
         timeoutMessage = null;
         retries = 0;
     }
 
     protected class LongPollRequestCallback implements RequestCallback {
         public void onError(Request request, Throwable throwable) {
 
             switch (statusCode) {
                 case 1:
                 case 408:
                 case 502:
                 case 504:
                     if (retries != maxRetries) {
                         if (timeoutDB == null) {
                             createConnectAttemptGUI();
                         }
 
                         logAdapter.warn("Attempting reconnection -- Retries: " + (maxRetries - retries));
                         timeoutMessage.setText("Connection Interrupted -- Retries: " + (maxRetries - retries));
                         retries++;
                         incomingTimer.scheduleRepeating(timeout);
                         statusCode = 0;
                         return;
                     } else {
                         timeoutMessage.setText("Connection re-attempt failed!");
                     }
             }
 
             logError("Communication Error", "None", throwable);
             incomingTimer.cancel();
         }
 
         public void onResponseReceived(Request request, Response response) {
             if (response.getStatusCode() != 200) {
                 statusCode = response.getStatusCode();
                 onError(request, new Throwable());
                 return;
             }
 
             if (retries != 0) {
                 clearConnectAttemptGUI();
             }
 
             try {
                 procIncomingPayload(response);
                 schedule();
             }
             catch (Throwable e) {
                 logError("Errai MessageBus Disconnected Due to Fatal Error",
                         response.getText(), e);
                 incomingTimer.cancel();
             }
         }
 
         public void schedule() {
             if (incomingTimer != null) incomingTimer.schedule(1);
         }
     }
 
     protected class ShortPollRequestCallback extends LongPollRequestCallback {
         @Override
         public void schedule() {
             if (incomingTimer != null) incomingTimer.schedule(POLL_FREQUENCY);
         }
     }
 
     public static int POLL_FREQUENCY = 250;
     private RequestCallback COMM_CALLBACK = new ShortPollRequestCallback();
 
     /**
      * Initializes the message bus by setting up the <tt>recvBuilder</tt> to accept responses. Also, initializes the
      * incoming timer to ensure the client's polling with the server is active.
      *
      * @param initCallback - not used
      */
     @SuppressWarnings({"UnusedDeclaration"})
     private void initializeMessagingBus(final HookCallback initCallback) {
         if (disconnected) {
             return;
         }
 
         incomingTimer = new Timer() {
             @Override
             public void run() {
                 try {
                     recvBuilder.sendRequest(null, COMM_CALLBACK);
                 }
                 catch (RequestTimeoutException e) {
                     statusCode = 1;
                     COMM_CALLBACK.onError(null, e);
                 }
                 catch (RequestException e) {
                     logError(e.getMessage(), "", e);
                 }
             }
         };
 
         new Timer() {
             @Override
             public void run() {
                 incomingTimer.scheduleRepeating(POLL_FREQUENCY);
                 ExtensionsLoader loader = GWT.create(ExtensionsLoader.class);
                 loader.initExtensions(ClientMessageBusImpl.this);
             }
         }.schedule(5);
 
         heartBeatTimer =
                 new Timer() {
                     @Override
                     public void run() {
                         if (System.currentTimeMillis() - lastTransmit >= HEARTBEAT_DELAY) {
                             encodeAndTransmit(MessageBuilder.createMessage().toSubject("ServerBus")
                                     .command(BusCommands.Heartbeat).noErrorHandling().getMessage());
                             schedule(HEARTBEAT_DELAY);
                         } else {
                             long win = System.currentTimeMillis() - lastTransmit;
                             int diff = HEARTBEAT_DELAY - (int) win;
                             schedule(diff);
                         }
                     }
                 };
 
         heartBeatTimer.scheduleRepeating(HEARTBEAT_DELAY);
     }
 
     /**
      * Add runnable tasks to be run after the message bus is initialized
      *
      * @param run a {@link Runnable} task.
      */
     public void addPostInitTask(Runnable run) {
         if (isInitialized()) {
             run.run();
         } else {
             postInitTasks.add(run);
         }
     }
 
     /**
      * Do-nothing function, should eventually be able to add a global listener to receive all messages. Though global
      * message dispatches the message to all listeners attached.
      *
      * @param listener - listener to accept all messages dispatched
      */
     public void addGlobalListener(MessageListener listener) {
     }
 
     /**
      * Adds a subscription listener, so it is possible to add subscriptions to the client.
      *
      * @param listener - subscription listener
      */
     public void addSubscribeListener(SubscribeListener listener) {
         this.onSubscribeHooks.add(listener);
     }
 
     /**
      * Adds an unsubscription listener, so it is possible for applications to remove subscriptions from the client
      *
      * @param listener - unsubscription listener
      */
     public void addUnsubscribeListener(UnsubscribeListener listener) {
         this.onUnsubscribeHooks.add(listener);
     }
 
     private native static void _unsubscribe(Object registrationHandle) /*-{
          $wnd.PageBus.unsubscribe(registrationHandle);
      }-*/;
 
     private native static Object _subscribe(String subject, MessageCallback callback,
                                             Object subscriberData) /*-{
           return $wnd.PageBus.subscribe(subject, null,
                   function (subject, message) {
                      callback.@org.jboss.errai.bus.client.api.MessageCallback::callback(Lorg/jboss/errai/bus/client/api/Message;)(@org.jboss.errai.bus.client.json.JSONUtilCli::decodeCommandMessage(Ljava/lang/Object;)(message))
                   },
                   null);
      }-*/;
 
     public native static void _store(String subject, Object value) /*-{
           $wnd.PageBus.store(subject, value);
      }-*/;
 
     private static String decodeCommandMessage(Message msg) {
         StringBuffer decode = new StringBuffer(
                 "<table><thead style='font-weight:bold;'><tr><td>Field</td><td>Value</td></tr></thead><tbody>");
 
         for (Map.Entry<String, Object> entry : msg.getParts().entrySet()) {
             decode.append("<tr><td>").append(entry.getKey()).append("</td><td>").append(entry.getValue()).append("</td></tr>");
         }
 
         return decode.append("</tbody></table>").toString();
     }
 
     class BusErrorDialog extends DialogBox {
         ScrollPanel scrollPanel;
         VerticalPanel contentPanel = new VerticalPanel();
 
         public BusErrorDialog() {
             setText("Message Bus Error");
 
             VerticalPanel panel = new VerticalPanel();
             HorizontalPanel buttonPanel = new HorizontalPanel();
             buttonPanel.getElement().getStyle().setProperty("backgroundColor", "darkgrey");
 
             Button clearErrors = new Button("Clear");
             clearErrors.addClickHandler(new ClickHandler() {
                 public void onClick(ClickEvent event) {
                     contentPanel.clear();
                 }
             });
 
             Button closeButton = new Button("Dismiss");
             closeButton.addClickHandler(new ClickHandler() {
                 public void onClick(ClickEvent event) {
                     errorDialog.hide();
                 }
             });
 
             buttonPanel.add(clearErrors);
             buttonPanel.add(closeButton);
 
             panel.add(buttonPanel);
             panel.setCellHorizontalAlignment(buttonPanel, HasHorizontalAlignment.ALIGN_RIGHT);
 
             Style s = panel.getElement().getStyle();
 
             s.setProperty("border", "1px");
             s.setProperty("borderStyle", "solid");
             s.setProperty("borderColor", "black");
             s.setProperty("backgroundColor", "lightgrey");
 
 
             scrollPanel = new ScrollPanel();
             scrollPanel.setWidth(Window.getClientWidth() * 0.80 + "px");
             scrollPanel.setHeight("500px");
             scrollPanel.setAlwaysShowScrollBars(true);
             panel.add(scrollPanel);
             scrollPanel.add(contentPanel);
             add(panel);
         }
 
         public void addError(String message, String additionalDetails, Throwable e) {
             contentPanel.add(new HTML("<strong style='background:red;color:white;'>" + message + "</strong>"));
 
             StringBuffer buildTrace = new StringBuffer("<tt style=\"font-size:11px;\"><pre>");
             if (e != null) {
                 buildTrace.append(e.getClass().getName()).append(": ").append(e.getMessage()).append("<br/>");
                 for (StackTraceElement ste : e.getStackTrace()) {
                     buildTrace.append("  ").append(ste.toString()).append("<br/>");
                 }
             }
             buildTrace.append("</pre>");
 
             contentPanel.add(new HTML(buildTrace.toString() + "<br/><strong>Additional Details:</strong>" + additionalDetails + "</tt>"));
 
             if (!isShowing()) {
                 show();
                 center();
                 getElement().getStyle().setProperty("zIndex", "5000");
             }
         }
     }
 
 
     private void logError(String message, String additionalDetails, Throwable e) {
         logAdapter.error(message + "<br/>Additional details:<br/> " + additionalDetails, e);
     }
 
     private void showError(String message, Throwable e) {
         if (errorDialog == null) {
             errorDialog = new BusErrorDialog();
         }
         errorDialog.addError(message, "", e);
     }
 
     /**
      * Process the incoming payload and push all the incoming messages onto the bus.
      *
      * @param response -
      * @throws Exception -
      */
     private void procIncomingPayload(Response response) throws Exception {
         try {
             for (MarshalledMessage m : decodePayload(response.getText())) {
                 _store(m.getSubject(), m.getMessage());
             }
         }
         catch (RuntimeException e) {
             logError("Error delivering message into bus", response.getText(), e);
             if (incomingTimer != null) incomingTimer.cancel();
         }
     }
 
     public void attachMonitor(BusMonitor monitor) {
     }
 
     public void setLogAdapter(LogAdapter logAdapter) {
         this.logAdapter = logAdapter;
     }
 
     public LogAdapter getLogAdapter() {
         return logAdapter;
     }
 }
