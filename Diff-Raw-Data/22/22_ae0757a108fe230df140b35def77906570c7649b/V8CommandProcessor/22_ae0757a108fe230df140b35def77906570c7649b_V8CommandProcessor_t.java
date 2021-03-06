 // Copyright (c) 2009 The Chromium Authors. All rights reserved.
 // Use of this source code is governed by a BSD-style license that can be
 // found in the LICENSE file.
 
 package org.chromium.sdk.internal.tools.v8;
 
 import java.util.Collection;
 import java.util.logging.Level;
 import java.util.logging.Logger;
 
 import org.chromium.sdk.SyncCallback;
 import org.chromium.sdk.internal.CloseableMap;
 import org.chromium.sdk.internal.JsonUtil;
 import org.chromium.sdk.internal.tools.v8.request.DebuggerMessage;
 import org.chromium.sdk.internal.tools.v8.request.V8MessageType;
 import org.json.simple.JSONObject;
 
 /**
  * Sends JSON commands to V8 VM and handles responses. Command is sent
  * via {@code V8CommandOutput}. Response is passed back to callback if it was provided.
  * Also all responses and events are dispatched to group of dedicated processors.
  */
 public class V8CommandProcessor {
 
   /**
    * A callback to handle V8 debugger responses.
    */
   public interface V8HandlerCallback {
     /**
      * This method is invoked when a debugger command result has become
      * available.
      *
      * @param response from the V8 debugger
      */
     void messageReceived(JSONObject response);
 
     /**
      * This method is invoked when a debugger command has failed.
      *
      * @param message containing the failure reason
      */
     void failure(String message);
 
     /** A no-op callback implementation. */
     V8HandlerCallback NULL_CALLBACK = new V8HandlerCallback() {
       public void failure(String message) {
       }
 
       public void messageReceived(JSONObject response) {
       }
     };
   }
 
   /** The class logger. */
   static final Logger LOGGER = Logger.getLogger(V8CommandProcessor.class.getName());
 
   private final CloseableMap<Integer, CallbackEntry> callbackMap = CloseableMap.newLinkedMap();
 
   private final V8CommandOutput messageOutput;
 
   private final DefaultResponseHandler defaultResponseHandler;
 
 
   public V8CommandProcessor(V8CommandOutput messageOutput,
       DefaultResponseHandler defaultResponseHandler) {
     this.messageOutput = messageOutput;
     this.defaultResponseHandler = defaultResponseHandler;
   }
 
   public void sendV8CommandAsync(DebuggerMessage message, boolean isImmediate,
       V8CommandProcessor.V8HandlerCallback v8HandlerCallback, SyncCallback syncCallback) {
 
     if (v8HandlerCallback != null) {
       // TODO(peter.rybin): should we handle IllegalStateException better than rethrowing it?
       try {
         callbackMap.put(message.getSeq(), new CallbackEntry(v8HandlerCallback,
             syncCallback));
       } catch (IllegalStateException e) {
         throw new IllegalStateException("Connection is closed", e);
       }
     }
     try {
       messageOutput.send(message, isImmediate);
     } catch (RuntimeException e) {
       if (v8HandlerCallback != null) {
         callbackMap.remove(message.getSeq());
       }
       throw e;
     }
   }
 
   public void processIncomingJson(final JSONObject v8Json) {
     V8MessageType type = V8MessageType.forString(JsonUtil.getAsString(v8Json, V8Protocol.KEY_TYPE));
 
     if (V8MessageType.RESPONSE == type) {
      Long requestSeq = JsonUtil.getAsLong(v8Json, V8Protocol.KEY_REQSEQ);
       checkNull(requestSeq, "Could not read 'request_seq' from debugger reply");
      int requestSeqInt = requestSeq.intValue();
      CallbackEntry callbackEntry = callbackMap.removeIfContains(requestSeqInt);
       if (callbackEntry != null) {
         LOGGER.log(
             Level.INFO,
             "Request-response roundtrip: {0}ms",
             getCurrentMillis() - callbackEntry.commitMillis);
 
         CallbackCaller caller = new CallbackCaller() {
           @Override
           void call(V8HandlerCallback handlerCallback) {
             handlerCallback.messageReceived(v8Json);
           }
         };
         try {
          callThemBack(callbackEntry, caller, requestSeqInt);
         } catch (RuntimeException e) {
           LOGGER.log(Level.SEVERE, "Failed to dispatch response to callback", e);
         }
       }
     }
 
     defaultResponseHandler.handleResponseWithHandler(type, v8Json);
   }
 
   public void processEos() {
     // We should call them in the order they have been submitted.
     Collection<CallbackEntry> entries = callbackMap.close().values();
     for (CallbackEntry entry : entries) {
       callThemBack(entry, failureCaller, -1);
     }
   }
 
   private static abstract class CallbackCaller {
     abstract void call(V8HandlerCallback handlerCallback);
   }
 
   private final static CallbackCaller failureCaller = new CallbackCaller() {
     @Override
     void call(V8HandlerCallback handlerCallback) {
       handlerCallback.failure("Detach");
     }
   };
 
 
   private void callThemBack(CallbackEntry callbackEntry, CallbackCaller callbackCaller,
       int requestSeq) {
     RuntimeException callbackException = null;
     try {
       if (callbackEntry.v8HandlerCallback != null) {
         LOGGER.log(
             Level.INFO, "Notified debugger command callback, request_seq={0}", requestSeq);
         callbackCaller.call(callbackEntry.v8HandlerCallback);
       }
     } catch (RuntimeException e) {
       callbackException = e;
     } finally {
       if (callbackEntry.syncCallback != null) {
         callbackEntry.syncCallback.callbackDone(callbackException);
       }
     }
   }
 
   /**
    * @return milliseconds since the epoch
    */
   private static long getCurrentMillis() {
     return System.currentTimeMillis();
   }
 
   static void checkNull(Object object, String message) {
     if (object == null) {
       throw new IllegalArgumentException(message);
     }
   }
 
 
   private static class CallbackEntry {
     final V8HandlerCallback v8HandlerCallback;
 
     final SyncCallback syncCallback;
 
     final long commitMillis;
 
     CallbackEntry(V8HandlerCallback v8HandlerCallback, SyncCallback syncCallback) {
       this.v8HandlerCallback = v8HandlerCallback;
       this.commitMillis = getCurrentMillis();
       this.syncCallback = syncCallback;
     }
   }
 
   public void removeAllCallbacks() {
     // TODO(peter.rybin): get rid of this method
   }
 }
