 /**
  * Copyright 2005-2010 Noelios Technologies.
  * 
  * The contents of this file are subject to the terms of one of the following
  * open source licenses: LGPL 3.0 or LGPL 2.1 or CDDL 1.0 or EPL 1.0 (the
  * "Licenses"). You can select the license that you prefer but you may not use
  * this file except in compliance with one of these Licenses.
  * 
  * You can obtain a copy of the LGPL 3.0 license at
  * http://www.opensource.org/licenses/lgpl-3.0.html
  * 
  * You can obtain a copy of the LGPL 2.1 license at
  * http://www.opensource.org/licenses/lgpl-2.1.php
  * 
  * You can obtain a copy of the CDDL 1.0 license at
  * http://www.opensource.org/licenses/cddl1.php
  * 
  * You can obtain a copy of the EPL 1.0 license at
  * http://www.opensource.org/licenses/eclipse-1.0.php
  * 
  * See the Licenses for the specific language governing permissions and
  * limitations under the Licenses.
  * 
  * Alternatively, you can obtain a royalty free commercial license with less
  * limitations, transferable or non-transferable, directly at
  * http://www.noelios.com/products/restlet-engine
  * 
  * Restlet is a registered trademark of Noelios Technologies.
  */
 
 package org.restlet.engine.connector;
 
 import java.io.IOException;
 import java.nio.channels.ReadableByteChannel;
 import java.nio.channels.SelectionKey;
 import java.util.logging.Level;
 
 import org.restlet.Context;
 import org.restlet.Response;
 import org.restlet.data.Form;
 import org.restlet.data.Parameter;
 import org.restlet.data.Status;
 import org.restlet.engine.header.HeaderReader;
 import org.restlet.engine.header.HeaderUtils;
 import org.restlet.engine.io.BufferState;
 import org.restlet.engine.io.IoState;
 import org.restlet.engine.io.ReadableBufferedChannel;
 import org.restlet.engine.io.ReadableChunkedChannel;
 import org.restlet.engine.io.ReadableSizedSelectionChannel;
 import org.restlet.representation.EmptyRepresentation;
 import org.restlet.representation.ReadableRepresentation;
 import org.restlet.representation.Representation;
 import org.restlet.util.SelectionRegistration;
 import org.restlet.util.Series;
 
 /**
  * A network connection way though which messages are received. Messages can be
  * either requests or responses.
  * 
  * @author Jerome Louvel
  */
 public abstract class InboundWay extends Way {
 
     /** The line builder index. */
     private volatile int builderIndex;
 
     /** The NIO selection registration of the entity. */
     private volatile SelectionRegistration entityRegistration;
 
     /**
      * Constructor.
      * 
      * @param connection
      *            The parent connection.
      * @param bufferSize
      *            The byte buffer size.
      */
     public InboundWay(Connection<?> connection, int bufferSize) {
         super(connection, bufferSize);
         this.builderIndex = 0;
     }
 
     @Override
     public void clear() {
         super.clear();
         this.builderIndex = 0;
         this.entityRegistration = null;
     }
 
     /**
      * Returns the message entity if available.
      * 
      * @param headers
      *            The headers to use.
      * @return The inbound message if available.
      */
     protected Representation createEntity(Series<Parameter> headers) {
         Representation result = null;
         long contentLength = HeaderUtils.getContentLength(headers);
         boolean chunkedEncoding = HeaderUtils.isChunkedEncoding(headers);
 
         // In some cases there is an entity without a content-length header
         boolean connectionClose = HeaderUtils.isConnectionClose(headers);
 
         // Create the representation
         if ((contentLength != Representation.UNKNOWN_SIZE && contentLength != 0)
                 || chunkedEncoding || connectionClose) {
             ReadableByteChannel inboundEntityChannel = null;
 
             // Wraps the remaining bytes into a special buffer channel
             ReadableBufferedChannel rbc = new ReadableBufferedChannel(this,
                     getIoBuffer(), getConnection()
                             .getReadableSelectionChannel());
 
             if (chunkedEncoding) {
                 // Wrap the buffer channel to decode chunks
                 inboundEntityChannel = new ReadableChunkedChannel(rbc);
             } else {
                 // Wrap the buffer channel to control its announced size
                 inboundEntityChannel = new ReadableSizedSelectionChannel(rbc,
                         contentLength);
             }
 
             setEntityRegistration(rbc.getRegistration());
 
             if (inboundEntityChannel != null) {
                 result = new ReadableRepresentation(inboundEntityChannel, null,
                         contentLength) {
                     @Override
                     public void release() {
                         getConnection().close(false);
                     }
                 };
 
                 result.setSize(contentLength);
                 setMessageState(MessageState.BODY);
             }
         } else {
             result = new EmptyRepresentation();
         }
 
         if (headers != null) {
             try {
                 result = HeaderUtils.extractEntityHeaders(headers, result);
             } catch (Throwable t) {
                 getLogger().log(Level.WARNING,
                         "Error while parsing entity headers", t);
             }
         }
 
         return result;
     }
 
     /**
      * Read the current message line (start line or header line).
      * 
      * @return True if the line is ready for reading.
      * @throws IOException
      */
     protected boolean fillLine() throws IOException {
         setLineBuilderState(getIoBuffer().fillLine(getLineBuilder(),
                 getLineBuilderState()));
         return getLineBuilderState() == BufferState.DRAINING;
     }
 
     /**
      * Returns the line builder index.
      * 
      * @return The line builder index.
      */
     protected int getBuilderIndex() {
         return builderIndex;
     }
 
     /**
      * Returns the NIO selection registration of the entity.
      * 
      * @return The NIO selection registration of the entity.
      */
     protected SelectionRegistration getEntityRegistration() {
         return entityRegistration;
     }
 
     @Override
     protected int getSocketInterestOps() {
         int result = 0;
 
         if ((getMessageState() == MessageState.BODY)
                 && (getIoState() == IoState.IDLE)
                 && (getEntityRegistration() != null)
                 && (getEntityRegistration().getListener() != null)) {
             result = getEntityRegistration().getInterestOperations();
         } else if (getIoState() == IoState.INTEREST) {
             result = SelectionKey.OP_READ;
         }
 
         return result;
     }
 
     /**
      * Indicates if the next message line is readable.
      * 
      * @return True if the next message line is readable.
      * @throws IOException
      */
     protected boolean isLineReadable() throws IOException {
         return isMessageReadable() && fillLine();
     }
 
     /**
      * Indicates if the inbound way can attempt to read the current message or
      * part of it.
      * 
      * @return True if the inbound way can attempt to read the current message
      *         or part of it.
      */
     protected boolean isMessageReadable() {
         return isSelected() && getIoBuffer().canDrain();
     }
 
     @Override
     protected boolean isSelected() {
         return super.isSelected() && (getMessageState() != MessageState.BODY);
     }
 
     @Override
     public void onCompleted(boolean endDetected) {
         if (getLogger().isLoggable(Level.FINER)) {
             getLogger().finer("Inbound message fully received");
         }
 
         super.onCompleted(endDetected);
     }
 
     @Override
     public void onError(Status status) {
         getHelper().onInboundError(status, getMessage());
         setMessage(null);
     }
 
     /**
      * Callback invoked when a message has been received. Note that one the
      * start line and the headers must have been received, not the optional
      * body.
      */
     protected void onReceived() {
         if (getLogger().isLoggable(Level.FINER)) {
             getLogger()
                     .finer("Inbound message start line and headers received");
         }
     }
 
     /**
      * Call back invoked when the message is received.
      * 
      * @param message
      *            The new message received.
      */
     protected abstract void onReceived(Response message);
 
     @Override
     public void onSelected(SelectionRegistration registration) {
         try {
             super.onSelected(registration);
 
             if ((getMessageState() == MessageState.BODY)
                     && (getEntityRegistration() != null)) {
                 getEntityRegistration().onSelected(
                         registration.getReadyOperations());
             } else {
                 while (isSelected()) {
                    if (getIoBuffer().isFilling()) {
                        int result = getIoBuffer().refill(
                                getConnection().getReadableSelectionChannel());

                        if (result == 0) {
                            if (getIoState() == IoState.PROCESSING) {
                                // Socket channel exhausted
                                setIoState(IoState.INTEREST);
                            }
                        } else if (result == -1) {
                            // End of connection detected
                            getConnection().close(true);
                            setIoState(IoState.IDLE);
                            setMessageState(MessageState.IDLE);
                         }
                     }
 
                     while (isMessageReadable()) {
                         // Bytes are available in the buffer
                         // attempt to parse the next message
                         readMessage();
                     }
                 }
             }
         } catch (Exception e) {
             getConnection().onError(
                     "Error while reading a message. Closing the connection.",
                     e, Status.CONNECTOR_ERROR_COMMUNICATION);
         }
     }
 
     /**
      * Read a message header.
      * 
      * @return The new message header or null.
      * @throws IOException
      */
     protected Parameter readHeader() throws IOException {
         Parameter header = HeaderReader.readHeader(getLineBuilder());
         clearLineBuilder();
         return header;
     }
 
     /**
      * Reads the next message if possible.
      * 
      * @throws IOException
      */
     protected void readMessage() throws IOException {
         boolean continueReading = true;
 
         while (continueReading && isLineReadable()) {
             // Parse next ready lines
             if (getMessageState() == MessageState.START) {
                 if (getLineBuilder().length() == 0) {
                     // Silently eat empty lines used for keep alive purpose
                     // sometimes (SIP)
                     continueReading = false;
                 } else {
                     if (getHelper().getLogger().isLoggable(Level.FINE)) {
                         getHelper().getLogger().fine(
                                 "Reading message from "
                                         + getConnection().getSocketAddress());
                     }
 
                     readStartLine();
                 }
             } else if (getMessageState() == MessageState.HEADERS) {
                 Parameter header = readHeader();
 
                 if (header != null) {
                     if (getHeaders() == null) {
                         setHeaders(new Form());
                     }
 
                     getHeaders().add(header);
                 } else {
                     // All headers received
                     onReceived();
                 }
             }
         }
     }
 
     /**
      * Read the start line of the current message received.
      * 
      * @throws IOException
      */
     protected abstract void readStartLine() throws IOException;
 
     /**
      * Sets the line builder index.
      * 
      * @param builderIndex
      *            The line builder index.
      */
     protected void setBuilderIndex(int builderIndex) {
         this.builderIndex = builderIndex;
     }
 
     /**
      * Sets the NIO selection registration of the entity.
      * 
      * @param entityRegistration
      *            The NIO selection registration of the entity.
      */
     protected void setEntityRegistration(
             SelectionRegistration entityRegistration) {
         this.entityRegistration = entityRegistration;
     }
 
     @Override
     public void setIoState(IoState ioState) {
         if (Context.getCurrentLogger().isLoggable(Level.FINER)) {
             Context.getCurrentLogger().log(Level.FINER,
                     "Inbound way: " + ioState);
         }
 
         super.setIoState(ioState);
     }
 
 }
