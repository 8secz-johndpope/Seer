 package org.dynmap.web;
 
 import java.io.IOException;
 import java.io.InputStream;
 import java.io.OutputStream;
 import java.io.PrintStream;
 import java.io.StringWriter;
 import java.net.Socket;
 import java.util.Map.Entry;
 import java.util.logging.Level;
 import java.util.logging.Logger;
 import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
 import org.dynmap.debug.Debug;
 
 public class HttpServerConnection extends Thread {
     protected static final Logger log = Logger.getLogger("Minecraft");
 
     private static Pattern requestHeaderLine = Pattern.compile("^(\\S+)\\s+(\\S+)\\s+HTTP/(.+)$");
     private static Pattern requestHeaderField = Pattern.compile("^([^:]+):\\s*(.+)$");
     
     private Socket socket;
     private HttpServer server;
     
     private PrintStream printOut;
     private StringWriter sw = new StringWriter();
     private Matcher requestHeaderLineMatcher;
     private Matcher requestHeaderFieldMatcher;
 
     public HttpServerConnection(Socket socket, HttpServer server) {
         this.socket = socket;
         this.server = server;
     }
 
     private final static void readLine(InputStream in, StringWriter sw) throws IOException {
         int readc;
         while((readc = in.read()) > 0) {
             char c = (char)readc;
             if (c == '\n')
                 break;
             else if (c != '\r')
                 sw.append(c);
         }
     }
     
     private final String readLine(InputStream in) throws IOException {
         readLine(in, sw);
         String r = sw.toString();
         sw.getBuffer().setLength(0);
         return r;
     }
 
     private final boolean readRequestHeader(InputStream in, HttpRequest request) throws IOException {
         String statusLine = readLine(in);
         
         if (statusLine == null)
             return false;
         
         if (requestHeaderLineMatcher == null) {
             requestHeaderLineMatcher = requestHeaderLine.matcher(statusLine);
         } else {
             requestHeaderLineMatcher.reset(statusLine);
         }
         
         Matcher m = requestHeaderLineMatcher;
         if (!m.matches())
             return false;
         request.method = m.group(1);
         request.path = m.group(2);
         request.version = m.group(3);
 
         String line;
         while (!(line = readLine(in)).equals("")) {
             if (requestHeaderFieldMatcher == null) {
                 requestHeaderFieldMatcher = requestHeaderField.matcher(line);
             } else {
                 requestHeaderFieldMatcher.reset(line);
             }
             
             m = requestHeaderFieldMatcher;
             // Warning: unknown lines are ignored.
             if (m.matches()) {
                 String fieldName = m.group(1);
                 String fieldValue = m.group(2);
                 // TODO: Does not support duplicate field-names.
                 request.fields.put(fieldName, fieldValue);
             }
         }
         return true;
     }
 
     public static final void writeResponseHeader(PrintStream out, HttpResponse response) throws IOException {
         out.append("HTTP/");
         out.append(response.version);
         out.append(" ");
         out.append(String.valueOf(response.statusCode));
         out.append(" ");
         out.append(response.statusMessage);
         out.append("\r\n");
         for (Entry<String, String> field : response.fields.entrySet()) {
             out.append(field.getKey());
             out.append(": ");
             out.append(field.getValue());
             out.append("\r\n");
         }
         out.append("\r\n");
         out.flush();
     }
     
     public final void writeResponseHeader(HttpResponse response) throws IOException {
         writeResponseHeader(printOut, response);
     }
 
     public void run() {
         try {
             socket.setSoTimeout(5000);
             InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();
             
            printOut = new PrintStream(out);
             while (true) {
                 HttpRequest request = new HttpRequest();
                 
                 if (!readRequestHeader(in, request)) {
                     socket.close();
                     return;
                 }
                 
                 long bound = -1;
                 BoundInputStream boundBody = null;
                 {
                     String contentLengthStr = request.fields.get(HttpField.contentLength);
                     if (contentLengthStr != null) {
                         try {
                             bound = Long.parseLong(contentLengthStr);
                         } catch (NumberFormatException e) {
                         }
                         if (bound >= 0) {
                             request.body = boundBody = new BoundInputStream(in, bound);
                         } else {
                             request.body = in;
                         }
                     }
                 }
 
                 // TODO: Optimize HttpHandler-finding by using a real path-aware tree.
                 HttpHandler handler = null;
                 String relativePath = null;
                 for (Entry<String, HttpHandler> entry : server.handlers.entrySet()) {
                     String key = entry.getKey();
                     boolean directoryHandler = key.endsWith("/");
                     if (directoryHandler && request.path.startsWith(entry.getKey()) || !directoryHandler && request.path.equals(entry.getKey())) {
                         relativePath = request.path.substring(entry.getKey().length());
                         handler = entry.getValue();
                         break;
                     }
                 }
 
                 if (handler == null) {
                     socket.close();
                     return;
                 }
                 
                 HttpResponse response = new HttpResponse(this, out);
 
                 try {
                     handler.handle(relativePath, request, response);
                 } catch (IOException e) {
                     throw e;
                 } catch (Exception e) {
                     log.log(Level.SEVERE, "HttpHandler '" + handler + "' has thown an exception", e);
                     if (socket != null) {
                         out.flush();
                         socket.close();
                     }
                     return;
                 }
 
                 if (bound > 0) {
                     boundBody.skip(bound);
                 }
                 
                 String connection = response.fields.get("Connection");
                 String contentLength = response.fields.get("Content-Length");
                 if (contentLength == null && connection == null) {
                     response.fields.put("Content-Length", "0");
                     OutputStream responseBody = response.getBody();
 
                     // The HttpHandler has already send the headers and written to the body without setting the Content-Length.
                     if (responseBody == null) {
                         Debug.debug("Response was given without Content-Length by '" + handler + "' for path '" + request.path + "'.");
                         out.flush();
                         socket.close();
                         return;
                     }
                 }
 
                 if (connection != null && connection.equals("close")) {
                     out.flush();
                     socket.close();
                     return;
                 }
 
                 out.flush();
             }
         } catch (IOException e) {
             if (socket != null) {
                 try {
                     socket.close();
                 } catch (IOException ex) {
                 }
             }
             return;
         } catch (Exception e) {
             if (socket != null) {
                 try {
                     socket.close();
                 } catch (IOException ex) {
                 }
             }
             log.log(Level.SEVERE, "Exception while handling request: ", e);
             e.printStackTrace();
             return;
         }
     }
 }
