 /*
  * Copyright 2010 Ning, Inc.
  *
  * Ning licenses this file to you under the Apache License, version 2.0
  * (the "License"); you may not use this file except in compliance with the
  * License.  You may obtain a copy of the License at:
  *
  *    http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
  * License for the specific language governing permissions and limitations
  * under the License.
  */
 package com.ning.http.client.providers;
 
 import com.ning.http.client.AsyncHttpProvider;
 import com.ning.http.client.HttpResponseBodyPart;
 import org.jboss.netty.buffer.ChannelBuffer;
 import org.jboss.netty.handler.codec.http.HttpChunk;
 import org.jboss.netty.handler.codec.http.HttpResponse;
 
 import java.io.IOException;
 import java.io.OutputStream;
 import java.net.URI;
 
 /**
  * A callback class used when an HTTP response body is received.
  */
 public class ResponseBodyPart extends HttpResponseBodyPart {
 
     private final HttpChunk chunk;
     private final HttpResponse response;
 
     public ResponseBodyPart(URI uri, HttpResponse response, AsyncHttpProvider<HttpResponse> provider) {
         super(uri, provider);
         this.chunk = null;
         this.response = response;
     }
 
    public ResponseBodyPart(URI uri, HttpResponse response, AsyncHttpProvider<HttpResponse> provider, HttpChunk chunk) {
         super(uri, provider);
         this.chunk = chunk;
         this.response = response;
     }
 
     /**
      * Return the response body's part bytes received.
     *
      * @return the response body's part bytes received.
      */
     public byte[] getBodyPartBytes() {
         if (chunk != null) {
             if (chunk.getContent().hasArray()) {
                 return chunk.getContent().array();
             } else {
                 ChannelBuffer b = chunk.getContent();
                 byte[] bytes = new byte[b.readableBytes()];
                b.getBytes(0, bytes, 0, bytes.length);
                 return bytes;
             }
         } else {
             if (response.getContent().hasArray()) {
                 return response.getContent().array();
             } else {
                 ChannelBuffer b = response.getContent();
                 byte[] bytes = new byte[b.readableBytes()];
                b.getBytes(0, bytes, 0, bytes.length);
                 return bytes;
             }
         }
     }
 
    public int writeTo(OutputStream outputStream) throws IOException {
        if (chunk != null) {
            ChannelBuffer b = chunk.getContent();
            b.readBytes(outputStream, b.readableBytes());
            return b.readableBytes();
        } else {
            ChannelBuffer b = response.getContent();
            b.readBytes(outputStream, b.readableBytes());
            return b.readableBytes();
        }
     }
 

     protected HttpChunk chunk() {
         return chunk;
     }
 }
