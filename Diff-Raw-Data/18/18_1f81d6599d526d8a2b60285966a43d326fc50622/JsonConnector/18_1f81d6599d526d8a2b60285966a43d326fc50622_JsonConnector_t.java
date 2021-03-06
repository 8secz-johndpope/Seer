 package com.jsonrpclib;
 
 
 import android.util.Base64;
 import android.util.Log;
 
 import java.io.ByteArrayInputStream;
 import java.io.InputStream;
 import java.lang.reflect.Field;
 import java.lang.reflect.InvocationTargetException;
 import java.lang.reflect.Method;
 import java.util.*;
 import java.util.concurrent.Callable;
 import java.util.concurrent.ExecutorService;
 import java.util.concurrent.Executors;
 
 class JsonConnector {
 
     private final String url;
     private final JsonRpcImplementation rpc;
     private final JsonConnection connection;
     private final Random randomGenerator = new Random();
 
     public JsonConnector(String url, JsonRpcImplementation rpc, JsonConnection connection) {
         this.url = url;
         this.rpc = rpc;
         this.connection = connection;
     }
 
     private static void longLog(String tag, String message) {
         JsonLoggerImpl.longLog(tag, message);
     }
 
     private static String convertStreamToString(InputStream is) {
         Scanner s = new Scanner(is).useDelimiter("\\A");
         return s.hasNext() ? s.next() : "";
     }
 
     private JsonResult sendRequest(JsonRequest request, JsonTimeStat timeStat) {
         return sendRequest(request, timeStat, null, null);
     }
 
     private JsonResult sendRequest(JsonRequest request, JsonTimeStat timeStat, String hash, Long time) {
         try {
             JsonResult result;
 
             Object virtualObject = handleVirtualServerRequest(request, timeStat);
             if (virtualObject != null) {
                 result = new JsonSuccessResult(request.getId(), virtualObject);
             } else {
                 ProtocolController controller = rpc.getProtocolController();
                 ProtocolController.RequestInfo requestInfo = controller.createRequest(url, request);
                 timeStat.tickCreateTime();
                 lossCheck();
                 delay();
                 JsonConnection.Connection conn = connection.send(controller, requestInfo, request.getTimeout(), timeStat,
                         rpc.getDebugFlags(), request.getMethod(), new JsonConnection.CacheInfo(hash, time));
 
                 if (!conn.isNewestAvailable()) {
                     if ((rpc.getDebugFlags() & JsonRpc.RESPONSE_DEBUG) > 0) {
                         JsonLoggerImpl.log("No new data for method " + request.getName());
                     }
 
                     return new JsonNoNewResult();
                 }
 
                 InputStream connectionStream = conn.getStream();
                 if ((rpc.getDebugFlags() & JsonRpc.RESPONSE_DEBUG) > 0) {
 
                     String resStr = convertStreamToString(conn.getStream());
                     longLog("Response(" + resStr.length() + "B)", resStr);
                     connectionStream = new ByteArrayInputStream(resStr.getBytes("UTF-8"));
                 }
                 JsonInputStream stream = new JsonInputStream(connectionStream, timeStat, conn.getContentLength());
                 result = controller.parseResponse(request, stream, conn.getHeaders());
                 if (result instanceof JsonSuccessResult) {
                     result.hash = conn.getHash();
                     result.time = conn.getDate();
                 }
                 timeStat.tickParseTime();
                 conn.close();
             }
             if (result instanceof JsonSuccessResult) {
                 if (rpc.isVerifyResultModel()) {
                     verifyResult(request, result);
                 }
                 if (rpc.isProcessingMethod()) {
                     processingMethod(result.result);
                 }
             }
             return result;
         } catch (Exception e) {
             return new JsonErrorResult(request.getId(), e);
         }
 
     }
 
     public static void processingMethod(Object object) {
         if (object instanceof Iterable) {
             for (Object obj : ((Iterable) object)) {
                 processingMethod(obj);
             }
         } else {
             for (Field field : object.getClass().getFields()) {
                 field.setAccessible(true);
                 try {
                    if (!field.getDeclaringClass().equals(Object.class) && !field.getType().isPrimitive()) {
                        Object fieldObject =field.get(object);
                        if(fieldObject!=null)
                        {
                            processingMethod(fieldObject);
                        }
                     }
                 } catch (Exception e) {
                     throw new RuntimeException(e);
                 }
             }
             invokeProcessingMethod(object);
         }
     }
 
     public static void invokeProcessingMethod(Object result) {
         Class<?> clazz = result.getClass();
         do {
             for (Method method : clazz.getDeclaredMethods()) {
                 if (method.isAnnotationPresent(JsonProcessingMethod.class)) {
                     method.setAccessible(true);
                     try {
                         method.invoke(result);
                     } catch (Exception e) {
                         throw new RuntimeException(e);
                     }
                 }
             }
             clazz = clazz.getSuperclass();
         } while (!clazz.equals(Object.class));
 
     }
 
     public static void verifyResult(JsonRequest request, JsonResult result) throws JsonException {
         if (result instanceof JsonSuccessResult && !request.getReturnType().equals(Void.class)) {
 
 
             if (result.result == null) {
                 JsonRequired ann = request.getMethod().getAnnotation(JsonRequired.class);
                 if (ann != null) {
                     throw new JsonException("Result object required.");
                 }
                 JsonRequiredList ann2 = request.getMethod().getAnnotation(JsonRequiredList.class);
                 if (ann2 != null) {
                     throw new JsonException("Result object required.");
                 }
             } else {
                 JsonRequiredList ann = request.getMethod().getAnnotation(JsonRequiredList.class);
                 if (ann != null) {
                     if (result.result instanceof Iterable) {
                         int i = 0;
                         for (Object obj : (Iterable) result.result) {
                             verifyResultObject(obj);
                             i++;
                         }
                         if (ann.minSize() > 0 && i < ann.minSize()) {
                             throw new JsonException("Result list from method " + request.getName() + "(size " + i + ") is smaller then limit: " + ann.minSize() + ".");
                         }
                         if (ann.maxSize() > 0 && i > ann.maxSize()) {
                             throw new JsonException("Result list from method " + request.getName() + "(size " + i + ") is larger then limit: " + ann.maxSize() + ".");
                         }
                     }
                 }
                 verifyResultObject(result.result);
             }
         }
     }
 
     public static void verifyResultObject(Object object) throws JsonException {
         if (object instanceof Iterable) {
             for (Object obj : ((Iterable) object)) {
                 verifyResultObject(obj);
             }
         } else {
             for (Field field : object.getClass().getFields()) {
                 try {
                     field.setAccessible(true);
 
                     if (field.get(object) == null) {
                         if (field.isAnnotationPresent(JsonRequired.class) || field.isAnnotationPresent(JsonRequiredList.class)) {
                             throw new JsonException("Field " + object.getClass().getName() + "." + field.getName() + " required.");
                         }
                     } else {
 
                         Object iterableObject = field.get(object);
                         if (iterableObject instanceof Iterable) {
                             JsonRequiredList ann = field.getAnnotation(JsonRequiredList.class);
                             if (ann != null) {
                                 int i = 0;
                                 for (Object obj : (Iterable) iterableObject) {
                                     verifyResultObject(obj);
                                     i++;
                                 }
 
                                 if (ann.minSize() > 0 && i < ann.minSize()) {
                                     throw new JsonException("List " + object.getClass().getName() + "." + field.getName() + "(size " + i + ") is smaller then limit: " + ann.minSize() + ".");
                                 }
                                 if (ann.maxSize() > 0 && i > ann.maxSize()) {
                                     throw new JsonException("List " + object.getClass().getName() + "." + field.getName() + "(size " + i + ") is larger then limit: " + ann.maxSize() + ".");
                                 }
                             }
                         } else if (field.getAnnotation(JsonRequired.class) != null) {
                             verifyResultObject(field.get(object));
                         }
                     }
                 } catch (IllegalAccessException e) {
                     e.printStackTrace();
                 }
             }
         }
     }
 
     public static Object[] addElement(Object[] org, Object added) {
         Object[] result = new Object[org.length + 1];
         System.arraycopy(org, 0, result, 0, org.length);
         result[org.length] = added;
         return result;
     }
 
 
     private Object handleVirtualServerRequest(JsonRequest request, JsonTimeStat timeStat) throws Exception {
         JsonVirtualServerInfo virtualServerInfo = rpc.getVirtualServers().get(request.getMethod().getDeclaringClass());
         if (virtualServerInfo != null) {
             if (request.getCallback() == null) {
                 try {
                     Object object = request.getMethod().invoke(virtualServerInfo.server, request.getArgs());
                     int delay = randDelay(virtualServerInfo.minDelay, virtualServerInfo.maxDelay);
                     for (int i = 0; i <= JsonTimeStat.TICKS; i++) {
                         Thread.sleep(delay / JsonTimeStat.TICKS);
                         timeStat.tickTime(i);
                     }
                     return object;
                 } catch (InvocationTargetException ex) {
                     if (ex.getCause() == null || !(ex.getCause() instanceof UnsupportedOperationException)) {
                         throw ex;
                     }
                 }
 
             } else {
                 JsonVirtualCallback callback = new JsonVirtualCallback(request.getId());
                 Object[] args = request.getArgs() != null ? addElement(request.getArgs(), callback) : new Object[]{callback};
                 boolean implemented = true;
                 try {
                     request.getMethod().invoke(virtualServerInfo.server, args);
                     int delay = randDelay(virtualServerInfo.minDelay, virtualServerInfo.maxDelay);
                     for (int i = 0; i <= JsonTimeStat.TICKS; i++) {
                         Thread.sleep(delay / JsonTimeStat.TICKS);
                         timeStat.tickTime(i);
                     }
                 } catch (InvocationTargetException ex) {
                     if (ex.getCause() != null && ex.getCause() instanceof UnsupportedOperationException) {
                         implemented = false;
                     } else {
                         throw ex;
                     }
                 }
                 if (implemented) {
                     if (callback.getResult().error != null) {
                         throw callback.getResult().error;
                     }
                     return callback.getResult().result;
                 }
             }
         }
         return null;
     }
 
 
     @SuppressWarnings("unchecked")
     public Object call(JsonRequest request) throws Exception {
         try {
 
             JsonCacheResult localCacheObject = null;
             JsonCacheResult serverCacheObject = null;
             JsonTimeStat timeStat = new JsonTimeStat(request);
 
 
             if ((rpc.isCacheEnabled() && request.isLocalCachable()) || rpc.isTest()) {
                 JsonLocalCacheLevel cacheLevel = rpc.isTest() ? JsonLocalCacheLevel.DISK_CACHE : request.getLocalCacheLevel();
                 localCacheObject = rpc.getMemoryCache().get(request.getMethod(), request.getArgs(), rpc.isTest() ? 0 : request.getLocalCacheLifeTime(), request.getLocalCacheSize());
                 if (localCacheObject.result) {
                     if (!request.isLocalCacheOnlyOnError()) {
                         timeStat.tickCacheTime();
                         return localCacheObject.object;
                     }
                 } else if (cacheLevel != JsonLocalCacheLevel.MEMORY_ONLY) {
                     JsonCacheMethod cacheMethod = new JsonCacheMethod(rpc.getTestName(), rpc.getTestRevision(), url, request.getMethod(), cacheLevel);
                     localCacheObject = rpc.getDiskCache().get(cacheMethod, Arrays.deepToString(request.getArgs()), request.getLocalCacheLifeTime());
                     if (localCacheObject.result) {
                         if (!rpc.isTest()) {  //we don't know when test will be stop
                             rpc.getMemoryCache().put(request.getMethod(), request.getArgs(), localCacheObject.object, request.getLocalCacheSize());
                         }
                         if (!request.isLocalCacheOnlyOnError()) {
                             timeStat.tickCacheTime();
                             return localCacheObject.object;
                         }
                     }
 
                 }
             }
 
             if (rpc.isCacheEnabled() && request.isServerCachable()) {
                 JsonCacheMethod cacheMethod = new JsonCacheMethod(url, request.getMethod(), request.getServerCacheLevel());
                 serverCacheObject = rpc.getDiskCache().get(cacheMethod, Arrays.deepToString(request.getArgs()), 0);
 
             }
 
             if (request.getArgs() != null && rpc.isByteArrayAsBase64()) {
                 int i = 0;
                 for (Object object : request.getArgs()) {
                     if (object instanceof byte[]) {
                         request.getArgs()[i] = Base64.encodeToString((byte[]) object, Base64.NO_WRAP);
                     }
                     i++;
                 }
             }
 
             JsonResult result;
             if (serverCacheObject != null && serverCacheObject.result) {
                 result = sendRequest(request, timeStat, serverCacheObject.hash, serverCacheObject.time);
                 if (result instanceof JsonNoNewResult) {
                     return serverCacheObject.object;
                 } else if (result instanceof JsonErrorResult && request.useServerCacheOldOnError()) {
                     return serverCacheObject.object;
                 }
             } else {
                 result = sendRequest(request, timeStat, null, null);
             }
 
             if (result instanceof JsonErrorResult) {
                 if (request.isLocalCacheOnlyOnError() && localCacheObject != null && localCacheObject.result) {
                     timeStat.tickCacheTime();
                     return localCacheObject.object;
                 }
             }
 
             if (result.error != null) {
                 throw result.error;
             }
 
 
             timeStat.tickEndTime();
 
 
             if (rpc.isTimeProfiler()) {
                 refreshStat(request.getName(), timeStat.getMethodTime());
             }
 
             if ((rpc.getDebugFlags() & JsonRpc.TIME_DEBUG) > 0) {
                 timeStat.logTime("End single request(" + request.getName() + "):");
             }
 
             if ((rpc.isCacheEnabled() && request.isLocalCachable()) || rpc.isTest()) {
                 rpc.getMemoryCache().put(request.getMethod(), request.getArgs(), result.result, request.getLocalCacheSize());
                 if (rpc.getCacheMode() == JsonCacheMode.CLONE) {
                     result.result = rpc.getJsonClonner().clone(result.result);
                 }
                 JsonLocalCacheLevel cacheLevel = rpc.isTest() ? JsonLocalCacheLevel.DISK_CACHE : request.getLocalCacheLevel();
                 if (cacheLevel != JsonLocalCacheLevel.MEMORY_ONLY) {
 
                     JsonCacheMethod cacheMethod = new JsonCacheMethod(rpc.getTestName(), rpc.getTestRevision(), url, request.getMethod(), cacheLevel);
                     rpc.getDiskCache().put(cacheMethod, Arrays.deepToString(request.getArgs()), result.result, request.getLocalCacheSize());
                 }
 
 
             } else if (rpc.isCacheEnabled() && request.isServerCachable() && (result.hash != null || result.time != null)) {
                 JsonCacheMethod cacheMethod = new JsonCacheMethod(url, request.getMethod(), request.getServerCacheLevel());
                 rpc.getDiskCache().put(cacheMethod, Arrays.deepToString(request.getArgs()), result.result, request.getServerCacheSize());
             }
 
 
 
             return result.result;
         } catch (Exception e) {
             refreshErrorStat(request.getName(), request.getTimeout());
             throw e;
         }
 
     }
 
     public List<JsonResult> callBatch(List<JsonRequest> requests, JsonProgressObserver progressObserver, Integer timeout) throws Exception {
         final List<JsonResult> results = new ArrayList<JsonResult>(requests.size());
 
 
         if (requests.size() > 0) {
 
 
             if (rpc.getProtocolController().isBatchSupported()) {
 
                 List<JsonRequest> copyRequest = new ArrayList<JsonRequest>(requests);
                 JsonVirtualServerInfo virtualServerInfo = rpc.getVirtualServers().get(requests.get(0).getMethod().getDeclaringClass());
                 if (virtualServerInfo != null) {
 
                     JsonTimeStat timeStat = new JsonTimeStat(progressObserver);
 
                     int delay = randDelay(virtualServerInfo.minDelay, virtualServerInfo.maxDelay);
 
                     for (int i = copyRequest.size() - 1; i >= 0; i--) {
                         JsonRequest request = copyRequest.get(i);
 
                         JsonVirtualCallback callback = new JsonVirtualCallback(request.getId());
                         Object[] args = request.getArgs() != null ? addElement(request.getArgs(), callback) : new Object[]{callback};
                         boolean implemented = true;
                         try {
                             request.getMethod().invoke(virtualServerInfo.server, args);
 
                         } catch (InvocationTargetException ex) {
                             if (ex.getCause() != null && ex.getCause() instanceof UnsupportedOperationException) {
                                 implemented = false;
                             } else {
                                 throw ex;
                             }
                         }
                         if (implemented) {
                             results.add(callback.getResult());
                             copyRequest.remove(request);
                         }
                     }
                     if (copyRequest.size() == 0) {
                         for (int z = 0; z < JsonTimeStat.TICKS; z++) {
                             Thread.sleep(delay / JsonTimeStat.TICKS);
                             timeStat.tickTime(z);
                         }
                     }
                 }
 
 
                 String requestsName = "";
                 for (JsonRequest request : requests) {
                     requestsName += " " + request.getName();
                     if (request.getArgs() != null && rpc.isByteArrayAsBase64()) {
                         int i = 0;
                         for (Object object : request.getArgs()) {
                             if (object instanceof byte[]) {
                                 request.getArgs()[i] = Base64.encodeToString((byte[]) object, Base64.NO_WRAP);
                             }
                             i++;
                         }
                     }
                 }
 
 
                 if (copyRequest.size() > 0) {
                     results.addAll(callRealBatch(copyRequest, progressObserver, timeout, requestsName));
                 }
             } else {
 
                 for (JsonRequest request : requests) {
                     if (request.getArgs() != null && rpc.isByteArrayAsBase64()) {
                         int i = 0;
                         for (Object object : request.getArgs()) {
                             if (object instanceof byte[]) {
                                 request.getArgs()[i] = Base64.encodeToString((byte[]) object, Base64.NO_WRAP);
                             }
                             i++;
                         }
                     }
                 }
 
                 synchronized (progressObserver) {
                     progressObserver.setMaxProgress(progressObserver.getMaxProgress() + (requests.size() - 1) * JsonTimeStat.TICKS);
                 }
                 ExecutorService executors = Executors.newFixedThreadPool(rpc.getMaxConnections());
                 List<Callable<Object>> todo = new ArrayList<Callable<Object>>();
 
                 for (final JsonRequest request : requests) {
 
                     final JsonTimeStat timeStat = new JsonTimeStat(progressObserver);
 
                     todo.add(Executors.callable(new Runnable() {
                         @Override
                         public void run() {
                             JsonCacheResult cacheObject = null;
                             if (rpc.isCacheEnabled() && request.isServerCachable()) {
                                 JsonCacheMethod cacheMethod = new JsonCacheMethod(url, request.getMethod(), request.getServerCacheLevel());
                                 cacheObject = rpc.getDiskCache().get(cacheMethod, Arrays.deepToString(request.getArgs()), request.getServerCacheSize());
 
                             }
 
                             if (cacheObject != null && cacheObject.result) {
                                 JsonResult result = sendRequest(request, timeStat, cacheObject.hash, cacheObject.time);
                                 synchronized (results) {
                                     if (result instanceof JsonNoNewResult) {
                                         results.add(new JsonSuccessResult(request.getId(), cacheObject.object));
                                     } else if (result instanceof JsonErrorResult && request.useServerCacheOldOnError()) {
                                         results.add(new JsonSuccessResult(request.getId(), cacheObject.object));
                                     } else {
                                         results.add(result);
                                     }
                                 }
                             } else {
                                 synchronized (results) {
                                     results.add(sendRequest(request, timeStat));
                                 }
                             }
                         }
                     }));
 
                 }
                 executors.invokeAll(todo);
                 executors.shutdown();
             }
         }
         return results;
     }
 
 
     private void delay() {
         int delay = rpc.getDelay();
         if (delay > 0) {
             try {
                 Thread.sleep(delay);
             } catch (InterruptedException e) {
                 e.printStackTrace();
             }
         }
     }
 
     public List<JsonResult> callRealBatch(List<JsonRequest> requests, JsonProgressObserver progressObserver, Integer timeout, String requestsName) throws Exception {
 
         try {
             ProtocolController controller = rpc.getProtocolController();
             List<JsonResult> responses;
             JsonTimeStat timeStat = new JsonTimeStat(progressObserver);
 
 
             ProtocolController.RequestInfo requestInfo = controller.createRequest(url, (List) requests);
             timeStat.tickCreateTime();
             lossCheck();
             delay();
             JsonConnection.Connection conn = connection.send(controller, requestInfo, timeout, timeStat, rpc.getDebugFlags(), null, null);
             InputStream connectionStream = conn.getStream();
             if ((rpc.getDebugFlags() & JsonRpc.RESPONSE_DEBUG) > 0) {
 
                 String resStr = convertStreamToString(conn.getStream());
                 longLog("Response body(" + resStr.length() + " Bytes)", resStr);
                 connectionStream = new ByteArrayInputStream(resStr.getBytes("UTF-8"));
             }
 
             JsonInputStream stream = new JsonInputStream(connectionStream, timeStat, conn.getContentLength());
             responses = controller.parseResponses((List) requests, stream, conn.getHeaders());
             timeStat.tickParseTime();
             conn.close();
             timeStat.tickEndTime();
             if (rpc.isTimeProfiler()) {
 
                 for (JsonRequest request : requests) {
                     refreshStat(request.getName(), timeStat.getMethodTime() / requests.size());
                 }
             }
 
 
             if ((rpc.getDebugFlags() & JsonRpc.TIME_DEBUG) > 0) {
                 timeStat.logTime("End batch request(" + requestsName.substring(1) + "):");
             }
 
             return responses;
         } catch (Exception e) {
             for (JsonRequest request : requests) {
                 refreshErrorStat(request.getName(), request.getTimeout());
             }
             throw new JsonException(requestsName.substring(1), e);
         }
     }
 
 
     private void lossCheck() throws JsonException {
         if (rpc.getPercentLoss() != 0 && randomGenerator.nextFloat() < rpc.getPercentLoss()) {
             throw new JsonException("Random package lost.");
         }
     }
 
     private JsonStat getStat(String method) {
         JsonStat stat;
         if (rpc.getStats().containsKey(method)) {
             stat = rpc.getStats().get(method);
         } else {
             stat = new JsonStat();
             rpc.getStats().put(method, stat);
         }
         return stat;
     }
 
     private void refreshStat(String method, long time) {
         JsonStat stat = getStat(method);
         stat.avgTime = ((stat.avgTime * stat.requestCount) + time) / (stat.requestCount + 1);
         stat.requestCount++;
         rpc.saveStat();
     }
 
     private void refreshErrorStat(String method, long timeout) {
         JsonStat stat = getStat(method);
         stat.avgTime = ((stat.avgTime * stat.requestCount) + timeout) / (stat.requestCount + 1);
         stat.errors++;
         stat.requestCount++;
         rpc.saveStat();
     }
 
     public void setReconnections(int reconnections) {
         connection.setReconnections(reconnections);
     }
 
     public void setConnectTimeout(int connectTimeout) {
         connection.setConnectTimeout(connectTimeout);
     }
 
     public void setMethodTimeout(int methodTimeout) {
         connection.setMethodTimeout(methodTimeout);
     }
 
     public int getMethodTimeout() {
         return connection.getMethodTimeout();
     }
 
     public int randDelay(int minDelay, int maxDelay) {
         if (maxDelay == 0) {
             return 0;
         }
         Random random = new Random();
         if (maxDelay != minDelay) {
             return minDelay + random.nextInt(maxDelay - minDelay);
         } else {
             return minDelay;
         }
     }
 }
