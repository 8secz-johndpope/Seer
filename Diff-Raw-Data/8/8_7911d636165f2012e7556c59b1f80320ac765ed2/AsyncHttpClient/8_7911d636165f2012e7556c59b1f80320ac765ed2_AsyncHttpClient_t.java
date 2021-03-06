 package com.koushikdutta.async.http;
 
 import android.os.Handler;
 import com.koushikdutta.async.*;
 import com.koushikdutta.async.callback.CompletedCallback;
 import com.koushikdutta.async.callback.ConnectCallback;
 import com.koushikdutta.async.callback.DataCallback;
 import com.koushikdutta.async.callback.RequestCallback;
 import com.koushikdutta.async.future.*;
 import com.koushikdutta.async.http.AsyncHttpClientMiddleware.OnRequestCompleteData;
 import com.koushikdutta.async.http.libcore.RawHeaders;
 import com.koushikdutta.async.stream.OutputStreamDataCallback;
 import org.json.JSONException;
 import org.json.JSONObject;
 
 import java.io.*;
 import java.net.HttpURLConnection;
 import java.net.URI;
 import java.util.ArrayList;
 import java.util.concurrent.TimeoutException;
 
 public class AsyncHttpClient {
     private static AsyncHttpClient mDefaultInstance;
     public static AsyncHttpClient getDefaultInstance() {
         if (mDefaultInstance == null)
             mDefaultInstance = new AsyncHttpClient(AsyncServer.getDefault());
 
         return mDefaultInstance;
     }
 
     ArrayList<AsyncHttpClientMiddleware> mMiddleware = new ArrayList<AsyncHttpClientMiddleware>();
     public ArrayList<AsyncHttpClientMiddleware> getMiddleware() {
         return mMiddleware;
     }
     public void insertMiddleware(AsyncHttpClientMiddleware middleware) {
         mMiddleware.add(0, middleware);
     }
 
     AsyncServer mServer;
     public AsyncHttpClient(AsyncServer server) {
         mServer = server;
         insertMiddleware(new AsyncSocketMiddleware(this));
         insertMiddleware(new AsyncSSLSocketMiddleware(this));
     }
 
    public Future<AsyncHttpResponse> execute(final AsyncHttpRequest request) {
         return execute(request, (HttpConnectCallback)null);
     }
 
    public Future<AsyncHttpResponse> execute(final AsyncHttpRequest request, final HttpConnectCallback callback) {
         FutureAsyncHttpResponse ret;
         execute(request, 0, ret = new FutureAsyncHttpResponse(), callback);
         return ret;
     }
 
     private static final String LOGTAG = "AsyncHttp";
    public static class FutureAsyncHttpResponse extends SimpleFuture<AsyncHttpResponse> {
         public AsyncSocket socket;
 
         @Override
         public boolean cancel() {
             if (!super.cancel())
                 return false;
 
             if (socket != null)
                 socket.close();
 
             return true;
         }
     }
 
     private void reportConnectedCompleted(FutureAsyncHttpResponse cancel, Exception ex, AsyncHttpResponseImpl response, final HttpConnectCallback callback) {
         boolean complete;
         if (ex != null)
             complete = cancel.setComplete(ex);
         else
             complete = cancel.setComplete(response);
         if (complete) {
             if (callback != null)
                 callback.onConnectCompleted(ex, response);
             return;
         }
 
         if (response != null) {
             // the request was cancelled, so close up shop, and eat any pending data
             response.setDataCallback(new NullDataCallback());
             response.close();
         }
     }
 
     private void execute(final AsyncHttpRequest request, final int redirectCount, final FutureAsyncHttpResponse cancel, final HttpConnectCallback callback) {
         if (redirectCount > 5) {
             reportConnectedCompleted(cancel, new Exception("too many redirects"), null, callback);
             return;
         }
         final URI uri = request.getUri();
         final OnRequestCompleteData data = new OnRequestCompleteData();
         data.request = request;
 
         data.connectCallback = new ConnectCallback() {
             Object scheduled = null;
             {
                 if (request.getTimeout() > 0) {
                     scheduled = mServer.postDelayed(new Runnable() {
                         @Override
                         public void run() {
                             reportConnectedCompleted(cancel, new TimeoutException(), null, callback);
                         }
                     }, request.getTimeout());
                 }
             }
 
             @Override
             public void onConnectCompleted(Exception ex, AsyncSocket socket) {
                 if (cancel.isCancelled()) {
                     if (socket != null)
                         socket.close();
                     return;
                 }
 
                 data.socket = socket;
                 for (AsyncHttpClientMiddleware middleware: mMiddleware) {
                     middleware.onSocket(data);
                 }
 
                 cancel.socket = socket;
 
                 if (ex != null) {
                     reportConnectedCompleted(cancel, ex, null, callback);
                     return;
                 }
 
                 final AsyncHttpResponseImpl ret = new AsyncHttpResponseImpl(request) {
                     @Override
                     public void setDataEmitter(DataEmitter emitter) {
                         data.bodyEmitter = emitter;
                         for (AsyncHttpClientMiddleware middleware: mMiddleware) {
                             middleware.onBodyDecoder(data);
                         }
                         mHeaders = data.headers;
 
                         super.setDataEmitter(data.bodyEmitter);
 
                         RawHeaders headers = mHeaders.getHeaders();
                         if ((headers.getResponseCode() == HttpURLConnection.HTTP_MOVED_PERM || headers.getResponseCode() == HttpURLConnection.HTTP_MOVED_TEMP) && request.getFollowRedirect()) {
                             URI redirect = URI.create(headers.get("Location"));
                             if (redirect == null || redirect.getScheme() == null) {
                                 redirect = URI.create(uri.toString().substring(0, uri.toString().length() - uri.getPath().length()) + headers.get("Location"));
                             }
                             AsyncHttpRequest newReq = new AsyncHttpRequest(redirect, request.getMethod());
                             execute(newReq, redirectCount + 1, cancel, callback);
 
                             setDataCallback(new NullDataCallback());
                             return;
                         }
 
                         // at this point the headers are done being modified
                         reportConnectedCompleted(cancel, null, this, callback);
                     }
 
                     protected void onHeadersReceived() {
                         try {
                             if (cancel.isCancelled())
                                 return;
 
                             if (scheduled != null)
                                 mServer.removeAllCallbacks(scheduled);
 
                             // allow the middleware to massage the headers before the body is decoded
 
                             data.headers = mHeaders;
                             for (AsyncHttpClientMiddleware middleware: mMiddleware) {
                                 middleware.onHeadersReceived(data);
                             }
                             mHeaders = data.headers;
 
                             // drop through, and setDataEmitter will be called for the body decoder.
                             // headers will be further massaged in there.
                         }
                         catch (Exception ex) {
                             reportConnectedCompleted(cancel, ex, null, callback);
                         }
                     }
 
                     @Override
                     protected void report(Exception ex) {
                         if (cancel.isCancelled())
                             return;
                         if (ex instanceof AsyncSSLException) {
                             AsyncSSLException ase = (AsyncSSLException)ex;
                             request.onHandshakeException(ase);
                             if (ase.getIgnore())
                                 return;
                         }
                         final AsyncSocket socket = getSocket();
                         if (socket == null)
                             return;
                         super.report(ex);
                         if (!socket.isOpen() || ex != null) {
                             if (getHeaders() == null && ex != null)
                                 reportConnectedCompleted(cancel, ex, null, callback);
                         }
 
                         data.exception = ex;
                         for (AsyncHttpClientMiddleware middleware: mMiddleware) {
                             middleware.onRequestComplete(data);
                         }
                     }
 
 
                     @Override
                     public AsyncSocket detachSocket() {
                         AsyncSocket socket = getSocket();
                         if (socket == null)
                             return null;
                         socket.setWriteableCallback(null);
                         socket.setClosedCallback(null);
                         socket.setEndCallback(null);
                         socket.setDataCallback(null);
                         setSocket(null);
                         return socket;
                     }
                 };
 
                 ret.setSocket(socket);
             }
         };
 
         for (AsyncHttpClientMiddleware middleware: mMiddleware) {
             Cancellable socketCancellable = middleware.getSocket(data);
             if (socketCancellable != null) {
                 cancel.setParent(socketCancellable);
                 return;
             }
         }
         assert false;
     }
 
     public Future<AsyncHttpResponse> execute(URI uri, final HttpConnectCallback callback) {
         return execute(new AsyncHttpGet(uri), callback);
     }
 
     public Future<AsyncHttpResponse> execute(String uri, final HttpConnectCallback callback) {
         return execute(new AsyncHttpGet(URI.create(uri)), callback);
     }
 
     public static abstract class RequestCallbackBase<T> implements RequestCallback<T> {
         @Override
         public void onProgress(AsyncHttpResponse response, int downloaded, int total) {
         }
         @Override
         public void onConnect(AsyncHttpResponse response) {
         }
     }
 
     public static abstract class DownloadCallback extends RequestCallbackBase<ByteBufferList> {
     }
 
     public static abstract class StringCallback extends RequestCallbackBase<String> {
     }
 
     public static abstract class JSONObjectCallback extends RequestCallbackBase<JSONObject> {
     }
 
     public static abstract class FileCallback extends RequestCallbackBase<File> {
     }
 
     private interface ResultConvert<T> {
         public T convert(ByteBufferList bb) throws Exception;
     }
 
     @Deprecated
     public Future<ByteBufferList> get(String uri, final DownloadCallback callback) {
         return getByteBufferList(uri, callback);
     }
 
     public Future<ByteBufferList> getByteBufferList(String uri) {
         return getByteBufferList(uri, null);
     }
     public Future<ByteBufferList> getByteBufferList(String uri, final DownloadCallback callback) {
         return get(uri, new ResultConvert<ByteBufferList>() {
             @Override
             public ByteBufferList convert(ByteBufferList b) {
                 return b;
             }
         }, callback);
     }
 
     @Deprecated
     public Future<String> get(String uri, final StringCallback callback) {
         return executeString(new AsyncHttpGet(uri), callback);
     }
     @Deprecated
     public Future<String> execute(AsyncHttpRequest req, final StringCallback callback) {
         return executeString(req, callback);
     }
 
     public Future<String> getString(String uri) {
         return executeString(new AsyncHttpGet(uri), null);
     }
     public Future<String> getString(String uri, final StringCallback callback) {
         return executeString(new AsyncHttpGet(uri), callback);
     }
 
     public Future<String> executeString(AsyncHttpRequest req) {
         return executeString(req, null);
     }
     public Future<String> executeString(AsyncHttpRequest req, final StringCallback callback) {
         return execute(req, new ResultConvert<String>() {
             @Override
             public String convert(ByteBufferList bb) {
                 String ret = bb.peekString();
                 bb.clear();
                 return ret;
             }
         }, callback);
     }
 
     @Deprecated
     public Future<JSONObject> get(String uri, final JSONObjectCallback callback) {
         return executeJSONObject(new AsyncHttpGet(uri), callback);
     }
     @Deprecated
     public Future<JSONObject> execute(AsyncHttpRequest req, final JSONObjectCallback callback) {
         return executeJSONObject(req, callback);
     }
 
     public Future<JSONObject> getJSONObject(String uri) {
         return getJSONObject(uri, null);
     }
     public Future<JSONObject> getJSONObject(String uri, final JSONObjectCallback callback) {
         return executeJSONObject(new AsyncHttpGet(uri), callback);
     }
 
     public Future<JSONObject> executeJSONObject(AsyncHttpRequest req) {
         return executeJSONObject(req, null);
     }
     public Future<JSONObject> executeJSONObject(AsyncHttpRequest req, final JSONObjectCallback callback) {
         return execute(req, new ResultConvert<JSONObject>() {
             @Override
             public JSONObject convert(ByteBufferList bb) throws JSONException {
                 String ret = bb.peekString();
                 bb.clear();
                 return new JSONObject(ret);
             }
         }, callback);
     }
 
     private <T> void invokeWithAffinity(final RequestCallback<T> callback, SimpleFuture<T> future, final AsyncHttpResponse response, final Exception e, final T result) {
         boolean complete;
         if (e != null)
             complete = future.setComplete(e);
         else
             complete = future.setComplete(result);
         if (!complete)
             return;
         if (callback != null)
             callback.onCompleted(e, response, result);
     }
 
     private <T> void invoke(Handler handler, final RequestCallback<T> callback, final SimpleFuture<T> future, final AsyncHttpResponse response, final Exception e, final T result) {
         Runnable runnable = new Runnable() {
             @Override
             public void run() {
                 invokeWithAffinity(callback, future, response, e, result);
             }
         };
         if (handler == null)
             mServer.post(runnable);
         else
             AsyncServer.post(handler, runnable);
     }
 
     private void invokeProgress(final RequestCallback callback, final AsyncHttpResponse response, final int downloaded, final int total) {
         if (callback != null)
             callback.onProgress(response, downloaded, total);
     }
 
     private void invokeConnect(final RequestCallback callback, final AsyncHttpResponse response) {
         if (callback != null)
             callback.onConnect(response);
     }
 
     @Deprecated
     public Future<File> get(String uri, final String filename, final FileCallback callback) {
         return executeFile(new AsyncHttpGet(uri), filename, callback);
     }
     @Deprecated
     public Future<File> execute(AsyncHttpRequest req, final String filename, final FileCallback callback) {
         return executeFile(req, filename, callback);
     }
 
     public Future<File> getFile(String uri, final String filename) {
         return getFile(uri, null);
     }
     public Future<File> getFile(String uri, final String filename, final FileCallback callback) {
         return executeFile(new AsyncHttpGet(uri), filename, callback);
     }
 
     public Future<File> executeFile(AsyncHttpRequest req, final String filename) {
         return executeFile(req, filename, null);
     }
     public Future<File> executeFile(AsyncHttpRequest req, final String filename, final FileCallback callback) {
         final Handler handler = req.getHandler();
         final File file = new File(filename);
         FutureAsyncHttpResponse cancel = new FutureAsyncHttpResponse();
         final SimpleFuture<File> ret = new SimpleFuture<File>() {
             @Override
             public boolean cancel() {
                 if (!super.cancel())
                     return false;
                 file.delete();
                 return true;
             }
         };
         ret.setParent(cancel);
         file.getParentFile().mkdirs();
         final OutputStream fout;
         try {
             fout = new BufferedOutputStream(new FileOutputStream(file), 8192);
         }
         catch (FileNotFoundException e) {
             if (ret.setComplete(e))
                 invoke(handler, callback, ret, null, e, null);
             return ret;
         }
         execute(req, 0, cancel, new HttpConnectCallback() {
             int mDownloaded = 0;
             @Override
             public void onConnectCompleted(Exception ex, final AsyncHttpResponse response) {
                 if (ex != null) {
                     try {
                         fout.close();
                     }
                     catch (IOException e) {
                     }
                     file.delete();
                     if (ret.setComplete(ex))
                         invoke(handler, callback, ret, response, ex, null);
                     return;
                 }
                 invokeConnect(callback, response);
 
                 final int contentLength = response.getHeaders().getContentLength();
 
                 response.setDataCallback(new OutputStreamDataCallback(fout) {
                     @Override
                     public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {
                         mDownloaded += bb.remaining();
                         super.onDataAvailable(emitter, bb);
                         invokeProgress(callback, response, mDownloaded, contentLength);
                     }
                 });
                 response.setEndCallback(new CompletedCallback() {
                     @Override
                     public void onCompleted(Exception ex) {
                         try {
                             fout.close();
                         }
                         catch (IOException e) {
                             ex = e;
                         }
                         if (ex != null) {
                             file.delete();
                             invoke(handler, callback, ret, response, ex, null);
                         }
                         else {
                             invoke(handler, callback, ret, response, null, file);
                         }
                     }
                 });
             }
         });
         return ret;
     }
 
     private <T> SimpleFuture<T> execute(AsyncHttpRequest req, final ResultConvert<T> convert, final RequestCallback<T> callback) {
         final SimpleFuture<T> ret = new SimpleFuture<T>();
         final Handler handler = req.getHandler();
         final FutureAsyncHttpResponse cancel = new FutureAsyncHttpResponse();
         execute(req, 0, cancel, new HttpConnectCallback() {
             int mDownloaded = 0;
             ByteBufferList buffer = new ByteBufferList();
             @Override
             public void onConnectCompleted(Exception ex, final AsyncHttpResponse response) {
                 if (ex != null) {
                     if (ret.setComplete(ex))
                         invoke(handler, callback, ret, response, ex, null);
                     return;
                 }
                 invokeConnect(callback, response);
 
                 final int contentLength = response.getHeaders().getContentLength();
 
                 response.setDataCallback(new DataCallback() {
                     @Override
                     public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {
                         mDownloaded += bb.remaining();
                         buffer.add(bb);
                         bb.clear();
                         invokeProgress(callback, response, mDownloaded, contentLength);
                     }
                 });
                 response.setEndCallback(new CompletedCallback() {
                     @Override
                     public void onCompleted(Exception ex) {
                         if (ex == null) {
                             try {
                                 T value = convert.convert(buffer);
                                 if (ret.setComplete(value))
                                     invoke(handler, callback, ret, response, null, value);
                                 return;
                             }
                             catch (Exception e) {
                                 ex = e;
                             }
                         }
                         if (ret.setComplete(ex))
                             invoke(handler, callback, ret, response, ex, null);
                     }
                 });
             }
         });
         ret.setParent(cancel);
         return ret;
     }
 
     private <T> Future<T> get(String uri, final ResultConvert<T> convert, final RequestCallback<T> callback) {
         return execute(new AsyncHttpGet(URI.create(uri)), convert, callback);
     }
 
     public static interface WebSocketConnectCallback {
         public void onCompleted(Exception ex, WebSocket webSocket);
     }
 
     public Future<WebSocket> websocket(final AsyncHttpRequest req, String protocol, final WebSocketConnectCallback callback) {
         WebSocketImpl.addWebSocketUpgradeHeaders(req, protocol);
         final SimpleFuture<WebSocket> ret = new SimpleFuture<WebSocket>();
         Cancellable connect = execute(req, new HttpConnectCallback() {
             @Override
             public void onConnectCompleted(Exception ex, AsyncHttpResponse response) {
                 if (ex != null) {
                     if (ret.setComplete(ex)) {
                         if (callback != null)
                             callback.onCompleted(ex, null);
                     }
                     return;
                 }
                 WebSocket ws = WebSocketImpl.finishHandshake(req.getHeaders().getHeaders(), response);
                 if (ws == null) {
                     if (!ret.setComplete(new Exception("Unable to complete websocket handshake")))
                         return;
                 }
                 else {
                     if (!ret.setComplete(ws))
                         return;
                 }
                 if (callback != null)
                     callback.onCompleted(ex, null);
             }
         });
 
         ret.setParent(connect);
         return ret;
     }
 
     public Future<WebSocket> websocket(String uri, String protocol, final WebSocketConnectCallback callback) {
         final AsyncHttpGet get = new AsyncHttpGet(uri);
         return websocket(get, protocol, callback);
     }
 
     public AsyncServer getServer() {
         return mServer;
     }
 }
