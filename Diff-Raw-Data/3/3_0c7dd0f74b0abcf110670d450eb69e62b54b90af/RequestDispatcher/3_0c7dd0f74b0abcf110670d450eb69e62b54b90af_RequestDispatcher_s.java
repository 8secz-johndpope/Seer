 package com.orbekk.protobuf;
 
 import java.util.concurrent.BlockingQueue;
 import java.util.concurrent.ExecutorService;
 import java.util.logging.Level;
 import java.util.logging.Logger;
 
 import com.google.protobuf.Descriptors;
 import com.google.protobuf.InvalidProtocolBufferException;
 import com.google.protobuf.Message;
 import com.google.protobuf.RpcCallback;
 import com.google.protobuf.Service;
 
 public class RequestDispatcher extends Thread {
     private static final Logger logger = Logger.getLogger(RequestDispatcher.class.getName());
     public static int DEFAULT_QUEUE_SIZE = 5;
     private volatile boolean isStopped = false;
     private final BlockingQueue<Data.Response> output;
     private final ServiceHolder services;
     
     /** A pool that can be shared among all dispatchers. */
     private final ExecutorService pool;
 
     private static class RequestHandler implements Runnable {
         private final Data.Request request;
         private final Data.Response.Builder response =
                 Data.Response.newBuilder();
         private final BlockingQueue<Data.Response> output;
         private final ServiceHolder services;
         private final Rpc rpc = new Rpc();
         
         private final RpcCallback<Message> callback =
                 new RpcCallback<Message>() {
             @Override public void run(Message responseMessage) {
                 if (responseMessage != null) {
                     response.setResponseProto(responseMessage.toByteString());
                 }
                 if (logger.isLoggable(Level.FINER)) {
                     logger.finer(String.format("I(%d): %s <= ",
                             request.getRequestId(), responseMessage));
                 }
                 rpc.writeTo(response);
                 try {
                     output.put(response.build());
                 } catch (InterruptedException e) {
                     // Terminate callback.
                     return;
                 }
             }
         };
         
         public RequestHandler(Data.Request request,
                 BlockingQueue<Data.Response> output,
                 ServiceHolder services) {
             this.request = request;
             this.output = output;
             this.services = services;
         }
         
         public void internalRun() throws InterruptedException {
             Service service = services.get(request.getFullServiceName());
             if (service == null) {
                 response.setError(Data.Response.RpcError.UNKNOWN_SERVICE);
                 output.put(response.build());
                 return;
             }
             
             Descriptors.MethodDescriptor method =
                     service.getDescriptorForType()
                             .findMethodByName(request.getMethodName());
             if (method == null) {
                 response.setError(Data.Response.RpcError.UNKNOWN_METHOD);
                 output.put(response.build());
                 return;
             }
             
             Message requestMessage = null;
             try {
                 requestMessage = service.getRequestPrototype(method)
                         .toBuilder().mergeFrom(request.getRequestProto()).build();
             } catch (InvalidProtocolBufferException e) {
                 response.setError(Data.Response.RpcError.INVALID_PROTOBUF);
                 output.put(response.build());
                 return;
             }
             
             if (logger.isLoggable(Level.FINER)) {
                 logger.fine(String.format("I(%d) => %s(%s)",
                         request.getRequestId(),
                         method.getFullName(),
                         requestMessage));
             }
            response.setRequestId(request.getRequestId());
             service.callMethod(method, rpc, requestMessage, callback);
         }
         
         @Override public void run() {
             try {
                 internalRun();
             } catch (InterruptedException e) {
                 // Terminate request.
                 return;
             }
         }
     }
     
     public RequestDispatcher(ExecutorService pool,
             BlockingQueue<Data.Response> output,
             ServiceHolder services) {
         this.pool = pool;
         this.output = output;
         this.services = services;
     }
 
     public void handleRequest(Data.Request request) throws InterruptedException {
         RequestHandler handler = new RequestHandler(request, output, services);
         pool.execute(handler);
     }
 }
