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
 
 package org.restlet.engine.io;
 
 import java.nio.channels.ClosedChannelException;
 import java.nio.channels.SelectionKey;
 import java.nio.channels.Selector;
 import java.nio.channels.SocketChannel;
 
 // [excludes gwt]
 /**
  * Readable byte channel based on a source socket channel that must only be
  * partially read.
  */
 public class WrapperSocketChannel extends WrapperChannel<SocketChannel>
         implements SelectionChannel {
 
     /** The selector to use. */
     private Selector selector;
 
     /**
      * Constructor.
      * 
      * @param wrappedChannel
      *            The source channel.
      * @param selector
      *            The selector to use.
      */
     public WrapperSocketChannel(SocketChannel wrappedChannel, Selector selector) {
         super(wrappedChannel);
         this.selector = selector;
     }
 
     /**
      * Returns the selector.
      * 
      * @return The selector.
      */
     protected Selector getSelector() {
         return selector;
     }
 
     /**
      * Registers the given listener with the underlying selector and socket
      * channel for the operations of interest.
      * 
      * @param ops
      *            The operations of interest.
      * @param listener
      *            The listener to notify.
     * @return The created or updated selection key.
      */
     public SelectionKey register(int ops, SelectionListener listener)
             throws ClosedChannelException {
         return getWrappedChannel().register(getSelector(), ops, listener);
     }
 
 }
